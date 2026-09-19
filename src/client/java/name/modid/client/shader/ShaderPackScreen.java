package name.modid.client.shader;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import name.modid.client.StrontiumClient;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public final class ShaderPackScreen extends Screen {
	private final Screen parent;
	private final ShaderPackManager manager;
	private String status = "Drop a .zip here or import one";

	public ShaderPackScreen(Screen parent, ShaderPackManager manager) {
		super(Component.literal("Strontium Shader Packs"));
		this.parent = parent;
		this.manager = manager;
	}

	@Override
	protected void init() {
		int center = width / 2;
		int y = 40;
		for (ShaderPack pack : manager.packs()) {
			ShaderPack selected = pack;
			addRenderableWidget(Button.builder(Component.literal(
							(manager.active() == pack ? "[Active] " : "") + pack.name()),
					button -> {
						if (manager.active() == selected) {
							StrontiumClient.disableShaderPack();
							status = "Disabled " + selected.name();
							rebuildWidgets();
							minecraft.setScreenAndShow(new ShaderStatusScreen(this, status));
							return;
						}
						manager.activate(selected);
						boolean loaded = StrontiumClient.activateShaderPack(selected);
						rebuildWidgets();
						status = loaded ? "Loaded " + selected.name()
								: "Pack incompatible; Strontium lighting fallback is active";
						minecraft.setScreenAndShow(new ShaderStatusScreen(this, status));
					}).bounds(center - 150, y, 300, 20).build());
			y += 24;
		}
		addRenderableWidget(Button.builder(Component.literal("Import shader pack .zip"), button -> importZip())
				.bounds(center - 150, height - 55, 145, 20).build());
		addRenderableWidget(Button.builder(Component.literal("Rescan folder"), button -> {
			manager.rescan();
			rebuildWidgets();
			status = "Scanned " + manager.directory();
		}).bounds(center + 5, height - 55, 145, 20).build());
		addRenderableWidget(Button.builder(CommonComponents.GUI_BACK, button -> minecraft.setScreenAndShow(parent))
				.bounds(center - 75, height - 30, 150, 20).build());
	}

	private void importZip() {
		if (!System.getProperty("os.name", "").toLowerCase(java.util.Locale.ROOT).contains("win")) {
			status = "Use drag-and-drop to import a shader pack on this platform";
			return;
		}
		status = "Opening Windows file picker...";
		Thread.startVirtualThread(() -> {
			try {
				String selected = chooseWindowsZip();
				if (selected == null || selected.isBlank()) {
					minecraft.execute(() -> status = "Import cancelled");
					return;
				}
				minecraft.execute(() -> importPack(Path.of(selected.trim())));
			} catch (IOException | InterruptedException exception) {
				minecraft.execute(() -> status = "Unable to open file picker: " + exception.getMessage());
			}
		});
	}

	private void importPack(Path source) {
		try {
			ShaderPack imported = manager.importZip(source);
			manager.activate(imported);
			boolean loaded = StrontiumClient.activateShaderPack(imported);
			rebuildWidgets();
			status = loaded ? "Loaded " + imported.name()
					: "Pack incompatible; Strontium lighting fallback is active";
			minecraft.setScreenAndShow(new ShaderStatusScreen(this, status));
		} catch (IOException | RuntimeException exception) {
			status = "Import failed: " + exception.getMessage();
		}
	}

	private static String chooseWindowsZip() throws IOException, InterruptedException {
		String script = "Add-Type -AssemblyName System.Windows.Forms; "
				+ "$d=New-Object System.Windows.Forms.OpenFileDialog; "
				+ "$d.Filter='Shader packs (*.zip)|*.zip'; "
				+ "$d.Title='Import Strontium shader pack'; "
				+ "if($d.ShowDialog() -eq [System.Windows.Forms.DialogResult]::OK){$d.FileName}";
		Process process = new ProcessBuilder(
				"powershell.exe", "-NoProfile", "-NonInteractive", "-STA", "-Command", script)
				.redirectErrorStream(true)
				.start();
		String result = new String(process.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
		if (process.waitFor() != 0) {
			throw new IOException(result.trim());
		}
		return result.trim();
	}

	@Override
	public void onFilesDrop(List<Path> paths) {
		for (Path path : paths) {
			if (path.getFileName().toString().toLowerCase(java.util.Locale.ROOT).endsWith(".zip")) {
				try {
					ShaderPack imported = manager.importZip(path);
					manager.activate(imported);
					boolean loaded = StrontiumClient.activateShaderPack(imported);
					rebuildWidgets();
					status = loaded ? "Loaded " + imported.name()
							: "Pack incompatible; Strontium lighting fallback is active";
					minecraft.setScreenAndShow(new ShaderStatusScreen(this, status));
					return;
				} catch (IOException | RuntimeException exception) {
					status = "Import failed: " + exception.getMessage();
				}
			}
		}
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		super.extractRenderState(graphics, mouseX, mouseY, delta);
		graphics.centeredText(font, title, width / 2, 15, 0xFFFFFF);
		graphics.centeredText(font, Component.literal(status), width / 2, height - 75, 0xAAAAAA);
		graphics.centeredText(font, Component.literal("Shaderpacks folder: " + manager.directory()),
				width / 2, height - 90, 0x777777);
	}
}
