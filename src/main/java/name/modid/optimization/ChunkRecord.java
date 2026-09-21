package name.modid.optimization;

import java.util.Arrays;

/**
 * Chunk transfer alternatives. A reference or patch is never trusted without
 * validating the resulting content hash; callers can fall back to Full.
 */
public sealed interface ChunkRecord permits ChunkRecord.Full, ChunkRecord.Reference, ChunkRecord.Patch {
	long chunkKey();

	byte[] contentHash();

	record Full(long chunkKey, byte[] contentHash, byte[] data) implements ChunkRecord {
		public Full {
			contentHash = contentHash.clone();
			data = data.clone();
		}

		@Override
		public byte[] contentHash() { return contentHash.clone(); }
		@Override
		public byte[] data() { return data.clone(); }
	}

	record Reference(long chunkKey, byte[] contentHash) implements ChunkRecord {
		public Reference {
			contentHash = contentHash.clone();
		}

		@Override
		public byte[] contentHash() { return contentHash.clone(); }
	}

	record Patch(long chunkKey, byte[] baseHash, byte[] contentHash, int resultLength, byte[] patch) implements ChunkRecord {
		public Patch {
			baseHash = baseHash.clone();
			contentHash = contentHash.clone();
			patch = patch.clone();
		}

		@Override
		public byte[] contentHash() { return contentHash.clone(); }
		public byte[] baseHash() { return baseHash.clone(); }
		public byte[] patch() { return patch.clone(); }
	}

	static byte[] copyHash(byte[] hash) {
		if (hash == null || hash.length != 32) {
			throw new IllegalArgumentException("chunk hashes must be SHA-256 values");
		}
		return Arrays.copyOf(hash, hash.length);
	}
}
