package name.modid.client;

import name.modid.optimization.BoundedCache;

public final class FontWidthCache {
	private static final BoundedCache<String, Integer> WIDTHS = new BoundedCache<>(4096);

	private FontWidthCache() {
	}

	public static int getOrCompute(String text, java.util.function.ToIntFunction<String> calculator) {
		return WIDTHS.getOrCompute(text, value -> calculator.applyAsInt(value));
	}

	public static void clear() {
		WIDTHS.clear();
	}
}
