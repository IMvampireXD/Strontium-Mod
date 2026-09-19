package name.modid.optimization;

import java.util.concurrent.ConcurrentHashMap;

public final class EnumValues {
	private static final ConcurrentHashMap<Class<?>, Object[]> CACHE = new ConcurrentHashMap<>();

	private EnumValues() {
	}

	public static <E extends Enum<E>> E[] values(Class<E> type) {
		Object[] values = CACHE.computeIfAbsent(type, EnumValues::readValues);
		@SuppressWarnings("unchecked")
		E[] result = (E[]) values;
		return result;
	}

	private static Object[] readValues(Class<?> type) {
		return type.getEnumConstants();
	}
}
