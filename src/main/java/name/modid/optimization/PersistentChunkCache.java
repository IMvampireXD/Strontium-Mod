package name.modid.optimization;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Bounded LRU chunk cache backed by a small append-only file. Entries are
 * checksummed; a torn or corrupt suffix is truncated during recovery.
 */
public final class PersistentChunkCache implements AutoCloseable {
	private static final int MAGIC = 0x53544348;
	private static final int HEADER = 4 + 8 + 32 + 4 + 4;
	private final Path file;
	private final int maximumEntries;
	private final long maximumBytes;
	private final Map<Key, byte[]> values = new LinkedHashMap<>(16, 0.75f, true);
	private long bytes;

	public PersistentChunkCache(Path file, int maximumEntries, long maximumBytes) {
		if (maximumEntries < 1 || maximumBytes < 1) throw new IllegalArgumentException("invalid cache bounds");
		this.file = file;
		this.maximumEntries = maximumEntries;
		this.maximumBytes = maximumBytes;
		load();
	}

	public synchronized byte[] get(long chunkKey, byte[] contentHash) {
		byte[] value = values.get(new Key(chunkKey, ChunkRecord.copyHash(contentHash)));
		return value == null ? null : value.clone();
	}

	public synchronized void put(long chunkKey, byte[] contentHash, byte[] data) {
		byte[] hash = ChunkRecord.copyHash(contentHash);
		byte[] copy = data.clone();
		if (!Arrays.equals(hash, ChunkRecordCodec.hash(copy))) return;
		if (copy.length > maximumBytes) return;
		Key key = new Key(chunkKey, hash);
		byte[] previous = values.put(key, copy);
		if (previous != null) bytes -= previous.length;
		bytes += copy.length;
		evict();
		append(key, copy);
	}

	public synchronized int size() {
		return values.size();
	}

	public synchronized long bytes() {
		return bytes;
	}

	public synchronized void clear() {
		values.clear();
		bytes = 0;
		if (file != null) {
			try {
				Files.deleteIfExists(file);
			} catch (IOException ignored) {
				// A cache is optional; a failed cleanup must not affect networking.
			}
		}
	}

	@Override
	public void close() {
		// Writes are synchronous and bounded, so no close operation is required.
	}

	private void load() {
		if (file == null || !Files.isRegularFile(file)) return;
		try {
			byte[] all = Files.readAllBytes(file);
			int offset = 0;
			while (offset + HEADER <= all.length) {
				ByteBuffer header = ByteBuffer.wrap(all, offset, HEADER);
				if (header.getInt() != MAGIC) break;
				long key = header.getLong();
				byte[] hash = new byte[32];
				header.get(hash);
				int length = header.getInt();
				int checksum = header.getInt();
				if (length < 0 || length > maximumBytes || offset + HEADER + length > all.length) break;
				byte[] data = Arrays.copyOfRange(all, offset + HEADER, offset + HEADER + length);
				if (checksum(data) != checksum || !Arrays.equals(hash, ChunkRecordCodec.hash(data))) break;
				byte[] previous = values.put(new Key(key, hash), data);
				if (previous != null) bytes -= previous.length;
				bytes += length;
				evict();
				offset += HEADER + length;
			}
			if (offset != all.length) truncate(offset);
		} catch (IOException | RuntimeException ignored) {
			values.clear();
			bytes = 0;
			try { Files.deleteIfExists(file); } catch (IOException ignoredDelete) { }
		}
	}

	private void append(Key key, byte[] data) {
		if (file == null) return;
		try {
			Path parent = file.getParent();
			if (parent != null) Files.createDirectories(parent);
			ByteBuffer header = ByteBuffer.allocate(HEADER);
			header.putInt(MAGIC).putLong(key.chunkKey()).put(key.hash()).putInt(data.length).putInt(checksum(data));
			Files.write(file, concat(header.array(), data), StandardOpenOption.CREATE, StandardOpenOption.WRITE,
					StandardOpenOption.APPEND);
			if (Files.size(file) > Math.max(maximumBytes * 2, 4096)) compact();
		} catch (IOException ignored) {
			// Persistence is best effort; the in-memory cache remains usable.
		}
	}

	private void compact() throws IOException {
		Path temporary = file.resolveSibling(file.getFileName() + ".rewrite");
		Files.deleteIfExists(temporary);
		for (Map.Entry<Key, byte[]> entry : values.entrySet()) {
			ByteBuffer header = ByteBuffer.allocate(HEADER);
			header.putInt(MAGIC).putLong(entry.getKey().chunkKey()).put(entry.getKey().hash())
					.putInt(entry.getValue().length).putInt(checksum(entry.getValue()));
			Files.write(temporary, concat(header.array(), entry.getValue()), StandardOpenOption.CREATE,
					StandardOpenOption.WRITE, StandardOpenOption.APPEND);
		}
		try {
			Files.move(temporary, file, java.nio.file.StandardCopyOption.REPLACE_EXISTING,
					java.nio.file.StandardCopyOption.ATOMIC_MOVE);
		} catch (java.nio.file.AtomicMoveNotSupportedException exception) {
			Files.move(temporary, file, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
		}
	}

	private void truncate(int length) throws IOException {
		try (var channel = Files.newByteChannel(file, StandardOpenOption.WRITE)) {
			channel.truncate(length);
		}
	}

	private void evict() {
		while (values.size() > maximumEntries || bytes > maximumBytes) {
			var iterator = values.entrySet().iterator();
			if (!iterator.hasNext()) break;
			bytes -= iterator.next().getValue().length;
			iterator.remove();
		}
	}

	private static int checksum(byte[] data) {
		java.util.zip.CRC32 crc = new java.util.zip.CRC32();
		crc.update(data);
		return (int) crc.getValue();
	}

	private static byte[] concat(byte[] first, byte[] second) {
		byte[] result = Arrays.copyOf(first, first.length + second.length);
		System.arraycopy(second, 0, result, first.length, second.length);
		return result;
	}

	private record Key(long chunkKey, byte[] hash) {
		@Override public boolean equals(Object object) {
			return object instanceof Key other && chunkKey == other.chunkKey && Arrays.equals(hash, other.hash);
		}
		@Override public int hashCode() { return 31 * Long.hashCode(chunkKey) + Arrays.hashCode(hash); }
	}
}
