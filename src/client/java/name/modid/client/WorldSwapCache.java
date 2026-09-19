package name.modid.client;

import net.minecraft.client.multiplayer.ClientLevel;

public final class WorldSwapCache {
	private static ClientLevel currentLevel;
	private static long generation;

	private WorldSwapCache() {
	}

	public static synchronized boolean begin(ClientLevel nextLevel) {
		if (currentLevel == nextLevel) {
			return false;
		}
		currentLevel = nextLevel;
		generation++;
		FontWidthCache.clear();
		return true;
	}

	public static synchronized long generation() {
		return generation;
	}
}
