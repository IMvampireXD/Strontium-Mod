package name.modid.client.shader;

import name.modid.Strontium;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ShaderPackManager {
	private static final List<String> PASS_ORDER = List.of(
			"shadow", "shadowcomp", "gbuffers", "deferred", "composite", "final"
	);
	private final Path directory;
	private final Map<String, ShaderPack> packs = new ConcurrentHashMap<>();
	private volatile ShaderPack active;

	public ShaderPackManager(Path gameDirectory) {
		directory = gameDirectory.resolve("shaderpacks");
		try {
			Files.createDirectories(directory);
		} catch (IOException exception) {
			throw new IllegalStateException("Unable to create " + directory, exception);
		}
		rescan();
	}

	public Path directory() {
		return directory;
	}

	public synchronized void rescan() {
		packs.clear();
		try (var files = Files.list(directory)) {
			files.filter(ShaderPackManager::isZip)
					.sorted()
					.forEach(path -> {
						try {
							ShaderPack pack = load(path);
							packs.put(pack.name(), pack);
						} catch (IOException | RuntimeException exception) {
							Strontium.LOGGER.warn("Ignoring invalid shader pack {}", path, exception);
						}
					});
		} catch (IOException exception) {
			Strontium.LOGGER.error("Unable to scan shader pack directory {}", directory, exception);
		}
	}

	public List<ShaderPack> packs() {
		return packs.values().stream().sorted(Comparator.comparing(ShaderPack::name)).toList();
	}

	public ShaderPack active() {
		return active;
	}

	public synchronized void activate(ShaderPack pack) {
		Objects.requireNonNull(pack, "pack");
		if (!packs.containsKey(pack.name())) {
			throw new IllegalArgumentException("Pack is not managed by this directory");
		}
		active = pack;
		Strontium.LOGGER.info("Loaded shader pack {} with {} passes", pack.name(), pack.passes().size());
	}

	public synchronized void deactivate() {
		active = null;
		Strontium.LOGGER.info("Shader pack disabled");
	}

	public ShaderPack importZip(Path source) throws IOException {
		if (!isZip(source)) {
			throw new IOException("Shader pack must be a .zip file");
		}
		Path destination = directory.resolve(source.getFileName().toString()).normalize();
		if (!destination.getParent().equals(directory)) {
			throw new IOException("Invalid shader pack path");
		}
		Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);
		ShaderPack imported = load(destination);
		packs.put(imported.name(), imported);
		return imported;
	}

	private static ShaderPack load(Path file) throws IOException {
		try (ZipFile zip = new ZipFile(file.toFile())) {
			List<ShaderPass> passes = new ArrayList<>();
			for (String program : PASS_ORDER) {
				if ("gbuffers".equals(program)) {
					for (String name : List.of("terrain", "entities", "block", "hand", "water", "textured")) {
						addPass(zip, passes, "gbuffers_" + name);
					}
				} else {
					addPass(zip, passes, program);
				}
			}
			if (passes.isEmpty()) {
				throw new IOException("No supported shader stages found under shaders/");
			}
			return new ShaderPack(file.getFileName().toString(), file, passes);
		}
	}

	private static void addPass(ZipFile zip, List<ShaderPass> passes, String name) throws IOException {
		String vertex = readOptional(zip, "shaders/" + name + ".vsh");
		String fragment = readOptional(zip, "shaders/" + name + ".fsh");
		if (vertex == null && fragment == null) {
			return;
		}
		String source = fragment != null ? fragment : vertex;
		passes.add(new ShaderPass(name, vertex, fragment, parseTargets(source)));
	}

	private static List<String> parseTargets(String source) {
		if (source == null) {
			return List.of();
		}
		int marker = source.indexOf("DRAWBUFFERS:");
		if (marker < 0) {
			marker = source.indexOf("RENDERTARGETS:");
		}
		if (marker < 0) {
			return List.of();
		}
		int end = source.indexOf("*/", marker);
		if (end < 0) {
			end = source.length();
		}
		String value = source.substring(marker).substring(source.substring(marker).indexOf(':') + 1, end - marker).trim();
		return List.of(value.split("[, ]+"));
	}

	private static String readOptional(ZipFile zip, String path) throws IOException {
		return readOptional(zip, path, new java.util.HashSet<>());
	}

	private static String readOptional(ZipFile zip, String path, java.util.Set<String> included) throws IOException {
		ZipEntry entry = zip.getEntry(path);
		if (entry == null || entry.isDirectory()) {
			return null;
		}
		try (InputStream input = zip.getInputStream(entry)) {
			String source = new String(input.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
			if (!included.add(path)) {
				throw new IOException("recursive shader include: " + path);
			}
			Matcher matcher = Pattern.compile("(?m)^\\s*#include\\s+[\"<]([^\">]+)[\">]\\s*$").matcher(source);
			StringBuffer expanded = new StringBuffer();
			while (matcher.find()) {
				String include = matcher.group(1);
				String includePath = "shaders/" + include;
				if (zip.getEntry(includePath) == null) {
					includePath = "shaders/include/" + include;
				}
				String replacement = readOptional(zip, includePath, included);
				matcher.appendReplacement(expanded, Matcher.quoteReplacement(replacement));
			}
			matcher.appendTail(expanded);
			return expanded.toString();
		} finally {
			included.remove(path);
		}
	}

	private static boolean isZip(Path path) {
		return Files.isRegularFile(path)
				&& path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".zip");
	}
}
