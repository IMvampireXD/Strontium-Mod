package name.modid;

import net.fabricmc.api.ModInitializer;

import net.minecraft.resources.Identifier;
import name.modid.optimization.ResourceDeduplicator;
import name.modid.optimization.StringDeduplicator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Strontium implements ModInitializer {
	public static final String MOD_ID = "strontium";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	public static final StringDeduplicator STRINGS = new StringDeduplicator();
	@Override
	public void onInitialize() {
		LOGGER.info("Strontium optimization core enabled");
	}

	public static Identifier id(String path) {
		String namespace = STRINGS.deduplicateLowercase(MOD_ID);
		String canonicalPath = STRINGS.deduplicateLowercase(path);
		return ResourceDeduplicator.identifier(Identifier.fromNamespaceAndPath(namespace, canonicalPath));
	}
}
//
