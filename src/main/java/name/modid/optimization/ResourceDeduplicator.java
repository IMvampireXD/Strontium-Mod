package name.modid.optimization;

import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Canonicalizes equal identifiers and resource keys so hot registries and
 * render state objects share one immutable instance.
 */
public final class ResourceDeduplicator {
	private static final ConcurrentMap<Identifier, Identifier> IDENTIFIERS = new ConcurrentHashMap<>();
	private static final ConcurrentMap<ResourceKey<?>, ResourceKey<?>> KEYS = new ConcurrentHashMap<>();

	private ResourceDeduplicator() {
	}

	public static Identifier identifier(Identifier identifier) {
		return IDENTIFIERS.computeIfAbsent(identifier, ignored -> identifier);
	}

	@SuppressWarnings("unchecked")
	public static <T> ResourceKey<T> key(ResourceKey<T> key) {
		return (ResourceKey<T>) KEYS.computeIfAbsent(key, ignored -> key);
	}

	public static void clear() {
		IDENTIFIERS.clear();
		KEYS.clear();
	}
}
