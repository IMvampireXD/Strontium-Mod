package name.modid.optimization;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

/**
 * Safe chunk record encoder/decoder and resolver. Patches use XOR for equal
 * sized chunks; all other cases are represented as a Full record.
 */
public final class ChunkRecordCodec {
	private static final int HASH_SIZE = 32;
	private static final int FULL = 1;
	private static final int REFERENCE = 2;
	private static final int PATCH = 3;

	private ChunkRecordCodec() {
	}

	public static ChunkRecord full(long chunkKey, byte[] data) {
		return new ChunkRecord.Full(chunkKey, hash(data), data);
	}

	public static ChunkRecord reference(long chunkKey, byte[] contentHash) {
		return new ChunkRecord.Reference(chunkKey, ChunkRecord.copyHash(contentHash));
	}

	public static ChunkRecord patch(long chunkKey, byte[] base, byte[] target) {
		if (base.length != target.length) {
			return full(chunkKey, target);
		}
		byte[] patch = new byte[target.length];
		for (int index = 0; index < patch.length; index++) {
			patch[index] = (byte) (base[index] ^ target[index]);
		}
		return new ChunkRecord.Patch(chunkKey, hash(base), hash(target), target.length, patch);
	}

	public static byte[] encode(ChunkRecord record) {
		if (record instanceof ChunkRecord.Full full) {
			byte[] data = full.data();
			ByteBuffer buffer = ByteBuffer.allocate(1 + 8 + HASH_SIZE + 4 + data.length);
			buffer.put((byte) FULL).putLong(full.chunkKey()).put(ChunkRecord.copyHash(full.contentHash()))
					.putInt(data.length).put(data);
			return buffer.array();
		}
		if (record instanceof ChunkRecord.Reference reference) {
			return ByteBuffer.allocate(1 + 8 + HASH_SIZE).put((byte) REFERENCE).putLong(reference.chunkKey())
					.put(ChunkRecord.copyHash(reference.contentHash())).array();
		}
		ChunkRecord.Patch patch = (ChunkRecord.Patch) record;
		byte[] delta = patch.patch();
		return ByteBuffer.allocate(1 + 8 + HASH_SIZE + HASH_SIZE + 4 + 4 + delta.length)
				.put((byte) PATCH).putLong(patch.chunkKey()).put(ChunkRecord.copyHash(patch.baseHash()))
				.put(ChunkRecord.copyHash(patch.contentHash())).putInt(patch.resultLength()).putInt(delta.length)
				.put(delta).array();
	}

	public static ChunkRecord decode(byte[] encoded, int maximumData) {
		if (encoded == null || maximumData < 0) {
			throw new IllegalArgumentException("invalid chunk record");
		}
		ByteBuffer buffer = ByteBuffer.wrap(encoded);
		if (!buffer.hasRemaining()) {
			throw new IllegalArgumentException("empty chunk record");
		}
		int type = buffer.get() & 0xff;
		if (type == FULL) {
			require(buffer, 8 + HASH_SIZE + 4);
			long key = buffer.getLong();
			byte[] contentHash = readHash(buffer);
			int length = buffer.getInt();
			requireLength(buffer, length, maximumData);
			byte[] data = new byte[length];
			buffer.get(data);
			if (buffer.hasRemaining() || !Arrays.equals(contentHash, hash(data))) {
				throw new IllegalArgumentException("invalid full chunk hash");
			}
			return new ChunkRecord.Full(key, contentHash, data);
		}
		if (type == REFERENCE) {
			require(buffer, 8 + HASH_SIZE);
			long key = buffer.getLong();
			byte[] contentHash = readHash(buffer);
			if (buffer.hasRemaining()) throw new IllegalArgumentException("trailing reference data");
			return new ChunkRecord.Reference(key, contentHash);
		}
		if (type == PATCH) {
			require(buffer, 8 + HASH_SIZE * 2 + 8);
			long key = buffer.getLong();
			byte[] baseHash = readHash(buffer);
			byte[] contentHash = readHash(buffer);
			int resultLength = buffer.getInt();
			int patchLength = buffer.getInt();
			requireLength(buffer, patchLength, maximumData);
			if (resultLength < 0 || resultLength > maximumData || patchLength != resultLength
					|| buffer.remaining() != patchLength) {
				throw new IllegalArgumentException("invalid patch length");
			}
			byte[] patch = new byte[patchLength];
			buffer.get(patch);
			return new ChunkRecord.Patch(key, baseHash, contentHash, resultLength, patch);
		}
		throw new IllegalArgumentException("unknown chunk record type");
	}

	public static ResolveResult resolve(ChunkRecord record, byte[] baseOrCached) {
		if (record instanceof ChunkRecord.Full full) {
			return verify(full.contentHash(), full.data())
					? new ResolveResult(full.data(), false) : new ResolveResult(null, true);
		}
		if (baseOrCached == null) {
			return new ResolveResult(null, true);
		}
		if (record instanceof ChunkRecord.Reference reference) {
			return verify(reference.contentHash(), baseOrCached)
					? new ResolveResult(baseOrCached, false) : new ResolveResult(null, true);
		}
		ChunkRecord.Patch patch = (ChunkRecord.Patch) record;
		if (!verify(patch.baseHash(), baseOrCached) || baseOrCached.length != patch.resultLength()) {
			return new ResolveResult(null, true);
		}
		byte[] result = new byte[patch.resultLength()];
		byte[] delta = patch.patch();
		for (int index = 0; index < result.length; index++) result[index] = (byte) (baseOrCached[index] ^ delta[index]);
		return verify(patch.contentHash(), result) ? new ResolveResult(result, false) : new ResolveResult(null, true);
	}

	public static byte[] hash(byte[] data) {
		try {
			return MessageDigest.getInstance("SHA-256").digest(data);
		} catch (NoSuchAlgorithmException exception) {
			throw new AssertionError(exception);
		}
	}

	private static boolean verify(byte[] expected, byte[] actual) {
		return Arrays.equals(expected, hash(actual));
	}

	private static byte[] readHash(ByteBuffer buffer) {
		require(buffer, HASH_SIZE);
		byte[] hash = new byte[HASH_SIZE];
		buffer.get(hash);
		return hash;
	}

	private static void require(ByteBuffer buffer, int bytes) {
		if (bytes < 0 || buffer.remaining() < bytes) throw new IllegalArgumentException("truncated chunk record");
	}

	private static void requireLength(ByteBuffer buffer, int length, int maximum) {
		if (length < 0 || length > maximum || buffer.remaining() < length) {
			throw new IllegalArgumentException("chunk record exceeds limit");
		}
	}

	public record ResolveResult(byte[] data, boolean fallbackRequired) {
		public ResolveResult {
			data = data == null ? null : data.clone();
		}
		@Override public byte[] data() { return data == null ? null : data.clone(); }
	}
}
