package name.modid.optimization;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

public final class SharedChunkPacketCache {
	private final Map<Key, byte[]> packets;

	public SharedChunkPacketCache(int capacity) {
		packets = new LinkedHashMap<>(capacity, 0.75f, true) {
			@Override
			protected boolean removeEldestEntry(Map.Entry<Key, byte[]> eldest) {
				return size() > capacity;
			}
		};
	}

	public synchronized byte[] get(long chunkKey, long contentHash) {
		byte[] packet = packets.get(new Key(chunkKey, contentHash));
		return packet == null ? null : packet.clone();
	}

	public synchronized void put(long chunkKey, long contentHash, byte[] packet) {
		packets.put(new Key(chunkKey, contentHash), packet.clone());
	}

	public synchronized void clear() {
		packets.clear();
	}

	private record Key(long chunkKey, long contentHash) {
	}
}
