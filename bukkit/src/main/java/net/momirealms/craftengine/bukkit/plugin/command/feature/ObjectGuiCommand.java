package net.momirealms.craftengine.bukkit.plugin.command.feature;

import net.momirealms.craftengine.bukkit.plugin.command.BukkitCommandFeature;
import net.momirealms.craftengine.bukkit.plugin.gui.ObjectGuiAssets;
import net.momirealms.craftengine.core.pack.Pack;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.command.CraftEngineCommandManager;
import org.bukkit.command.CommandSender;
import org.incendo.cloud.Command;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.parser.standard.StringParser;
import org.snakeyaml.engine.v2.api.Dump;
import org.snakeyaml.engine.v2.api.DumpSettings;
import org.snakeyaml.engine.v2.api.Load;
import org.snakeyaml.engine.v2.api.LoadSettings;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.LinkedHashMap;
import java.util.Map;

/** Exports editable PNG templates and imports them into CraftEngine resource packs. */
public final class ObjectGuiCommand extends BukkitCommandFeature<CommandSender> {
    private static final int MAX_PNG_BYTES = 5_000_000;
    private final boolean export;

    public ObjectGuiCommand(CraftEngineCommandManager<CommandSender> manager, CraftEngine plugin, boolean export) {
        super(manager, plugin);
        this.export = export;
    }

    @Override
    public String getFeatureID() {
        return export ? "object_gui_export" : "object_gui_import";
    }

    @Override
    public Command.Builder<? extends CommandSender> assembleCommand(
            CommandManager<CommandSender> manager, Command.Builder<CommandSender> builder) {
        var command = builder.required("pack", StringParser.stringParser())
                .required("category", StringParser.stringParser())
                .required("type", StringParser.stringParser())
                .required("id", StringParser.stringParser());
        if (export) command = command.optional("rows", StringParser.stringParser());
        return command.handler(context -> {
            String packName = context.get("pack");
            String category = context.get("category");
            String type = context.get("type");
            String id = context.get("id");
            try {
                ObjectGuiAssets.validate(packName, category, type, id);
                Pack pack = plugin().packManager().loadedPacks().stream()
                        .filter(p -> p.name().equals(packName) && p.enabled())
                        .findFirst().orElse(null);
                if (pack == null) {
                    context.sender().sendMessage("VSRender: primero crea/habilita el pack " + packName);
                    return;
                }
                Path folder = ObjectGuiAssets.objectFolder(plugin(), packName, category, type, id)
                        .resolve("gui");
                if (export) {
                    int rows = Integer.parseInt(context.getOrDefault("rows", "3"));
                    if (rows < 1 || rows > 6) throw new IllegalArgumentException("Filas: de 1 a 6");
                    exportTemplate(folder, id, rows);
                    context.sender().sendMessage("VSRender: PNGs y YAML exportados a " + folder);
                    context.sender().sendMessage("Edita los PNG y luego usa /vsr gui import "
                            + packName + " " + category + " " + type + " " + id);
                } else {
                    importTemplate(pack, folder, category, type, id);
                    context.sender().sendMessage("VSRender: GUI importada; recursos preparados.");
                    context.sender().sendMessage("Despues de recargar la configuracion y regenerar/enviar el pack, "
                            + "usa /vsr gui open " + packName + " " + previewName(category, type, id));
                    context.sender().sendMessage("Para un almacenamiento: agrega gui: \""
                            + packName + ":" + category + "/" + type + "/" + id
                            + "\" al comportamiento simple_storage correspondiente.");
                }
            } catch (NumberFormatException ex) {
                context.sender().sendMessage("VSRender: filas debe ser un numero entre 1 y 6.");
            } catch (IllegalArgumentException ex) {
                context.sender().sendMessage("VSRender: " + ex.getMessage());
            } catch (IOException ex) {
                plugin().logger().warn("Failed to export/import object GUI", ex);
                context.sender().sendMessage("VSRender: error de archivos GUI: " + ex.getMessage());
            }
        });
    }

    private static String previewName(String category, String type, String id) {
        return "obj_" + category + "_" + type + "_" + id;
    }

    private static void exportTemplate(Path folder, String id, int rows) throws IOException {
        // An existing edited GUI is never reset, even when export is used again.
        Files.createDirectories(folder);
        Path config = folder.resolve("gui.yml");
        if (Files.exists(config)) {
            throw new java.nio.file.FileAlreadyExistsException(config.toString());
        }
        int height = 114 + rows * 18;
        BufferedImage background = new BufferedImage(176, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = background.createGraphics();
        try {
            g.setColor(new Color(198, 198, 198));
            g.fillRect(0, 0, 176, height);
            g.setColor(new Color(73, 73, 73));
            g.drawRect(0, 0, 175, height - 1);
            drawSlots(g, rows);
        } finally {
            g.dispose();
        }
        BufferedImage overlay = new BufferedImage(176, height, BufferedImage.TYPE_INT_ARGB);
        BufferedImage guide = new BufferedImage(176, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D guideGraphics = guide.createGraphics();
        try {
            guideGraphics.setColor(new Color(255, 0, 0, 130));
            for (int row = 0; row < rows; row++) {
                for (int col = 0; col < 9; col++) {
                    guideGraphics.drawRect(7 + 18 * col, 17 + 18 * row, 17, 17);
                }
            }
            guideGraphics.setColor(new Color(0, 90, 255, 155));
            for (int row = 0; row < 3; row++) {
                for (int col = 0; col < 9; col++) {
                    guideGraphics.drawRect(7 + 18 * col, rows * 18 + 30 + 18 * row, 17, 17);
                }
            }
            for (int col = 0; col < 9; col++) {
                guideGraphics.drawRect(7 + 18 * col, rows * 18 + 88, 17, 17);
            }
        } finally {
            guideGraphics.dispose();
        }
        writeImageNew(folder.resolve("background.png"), background);
        writeImageNew(folder.resolve("overlay_player_inventory.png"), overlay);
        writeImageNew(folder.resolve("guia_casillas.png"), guide);
        String yaml = """
                # PNG editables del objeto. La imagen cubre tambien el inventario del jugador.
                # No cambies rows sin volver a exportar los PNG al tamano correspondiente.
                title: "%s"
                rows: %d
                buttons: {}
                """.formatted(id, rows);
        Files.writeString(config, yaml, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
        Files.writeString(folder.resolve("layout.yml"), """
                # Se conservan los slots vanilla del inventario del jugador.
                # El overlay solo cambia su apariencia; no mueve ni bloquea slots.
                player_inventory:
                  mode: vanilla
                  decorate: true
                """, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
    }

    private static void drawSlots(Graphics2D g, int rows) {
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < 9; col++) drawSlot(g, 8 + 18 * col, 18 + 18 * row);
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                drawSlot(g, 8 + 18 * col, rows * 18 + 31 + 18 * row);
            }
        }
        for (int col = 0; col < 9; col++) drawSlot(g, 8 + 18 * col, rows * 18 + 89);
    }

    private static void drawSlot(Graphics2D g, int x, int y) {
        g.setColor(new Color(55, 55, 55));
        g.fillRect(x - 1, y - 1, 18, 18);
        g.setColor(new Color(139, 139, 139));
        g.fillRect(x, y, 16, 16);
    }

    private static void writeImageNew(Path path, BufferedImage image) throws IOException {
        if (Files.exists(path)) throw new java.nio.file.FileAlreadyExistsException(path.toString());
        if (!ImageIO.write(image, "PNG", path.toFile())) {
            throw new IOException("PNG encoder not found");
        }
    }

    private static void importTemplate(Pack pack, Path folder, String category, String type, String id)
            throws IOException {
        Path yamlFile = folder.resolve("gui.yml");
        if (!Files.isRegularFile(yamlFile) || Files.isSymbolicLink(yamlFile)
                || Files.size(yamlFile) > 65536) {
            throw new IllegalArgumentException("No existe un gui/gui.yml valido. Primero exporta la GUI.");
        }
        Object parsed = new Load(LoadSettings.builder().build()).loadFromString(Files.readString(yamlFile));
        if (!(parsed instanceof Map<?, ?> raw)) throw new IllegalArgumentException("YAML invalido");
        Object rowValue = raw.get("rows");
        if (!(rowValue instanceof Number n) || n.intValue() < 1 || n.intValue() > 6
                || n.doubleValue() != n.intValue()) throw new IllegalArgumentException("Filas: de 1 a 6");
        int rows = ((Number) rowValue).intValue();
        int height = 114 + rows * 18;

        BufferedImage background = readImage(folder.resolve("background.png"), height);
        Path layoutFile = folder.resolve("layout.yml");
        boolean decorate = true;
        if (Files.isRegularFile(layoutFile)) {
            if (Files.isSymbolicLink(layoutFile) || Files.size(layoutFile) > 65536) {
                throw new IllegalArgumentException("layout.yml invalido");
            }
            Object layout = new Load(LoadSettings.builder().build()).loadFromString(Files.readString(layoutFile));
            if (!(layout instanceof Map<?, ?> root)
                    || !(root.get("player_inventory") instanceof Map<?, ?> playerLayout)) {
                throw new IllegalArgumentException("Falta player_inventory en layout.yml");
            }
            if (!"vanilla".equals(playerLayout.get("mode"))) {
                throw new IllegalArgumentException("Los slots del jugador solo admiten mode: vanilla");
            }
            if (playerLayout.get("decorate") instanceof Boolean flag) decorate = flag;
        }
        if (decorate && Files.isRegularFile(folder.resolve("overlay_player_inventory.png"))) {
            BufferedImage overlay = readImage(folder.resolve("overlay_player_inventory.png"), height);
            Graphics2D graphics = background.createGraphics();
            try {
                graphics.drawImage(overlay, 0, 0, null);
            } finally {
                graphics.dispose();
            }
        }

        String namespace = pack.namespace();
        String imageId = ObjectGuiAssets.imageId(namespace, category, type, id);
        String fontPath = category + "/" + type + "/" + id;
        Path pngOut = pack.resourcePackFolder().resolve("assets").resolve(namespace)
                .resolve("textures").resolve("font").resolve("gui").resolve(fontPath + ".png");
        Files.createDirectories(pngOut.getParent());
        if (!ImageIO.write(background, "PNG", pngOut.toFile())) {
            throw new IOException("PNG encoder not found");
        }

        Path imageYml = pack.configurationFolder().resolve("gui").resolve("objects")
                .resolve(category).resolve(type).resolve(id + ".yml");
        Files.createDirectories(imageYml.getParent());
        Map<String, Object> imageValues = new LinkedHashMap<>();
        imageValues.put("height", height);
        imageValues.put("ascent", 18);
        imageValues.put("font", "minecraft:gui");
        imageValues.put("file", namespace + ":font/gui/" + fontPath + ".png");
        Files.writeString(imageYml, dump(Map.of("images", Map.of(imageId, imageValues))));

        // The existing GUI preview uses the exact same BasicGui, no second engine.
        Path preview = pack.configurationFolder().resolve("gui").resolve(previewName(category, type, id) + ".yml");
        Map<String, Object> previewConfig = new LinkedHashMap<>();
        previewConfig.put("title", ObjectGuiAssets.imageTitle(imageId));
        previewConfig.put("rows", rows);
        Object buttons = raw.get("buttons");
        previewConfig.put("buttons", buttons == null ? Map.of() : buttons);
        Files.writeString(preview, dump(previewConfig));
    }

    private static BufferedImage readImage(Path path, int expectedHeight) throws IOException {
        if (!Files.isRegularFile(path) || Files.isSymbolicLink(path)
                || Files.size(path) > MAX_PNG_BYTES) {
            throw new IllegalArgumentException("PNG inexistente, enlace simbolico o demasiado grande: " + path.getFileName());
        }
        BufferedImage image = ImageIO.read(path.toFile());
        if (image == null || image.getWidth() != 176 || image.getHeight() != expectedHeight) {
            throw new IllegalArgumentException("PNG debe medir 176x" + expectedHeight + ": " + path.getFileName());
        }
        BufferedImage rgba = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D copy = rgba.createGraphics();
        try {
            copy.drawImage(image, 0, 0, null);
        } finally {
            copy.dispose();
        }
        return rgba;
    }

    private static String dump(Map<String, ?> map) {
        return new Dump(DumpSettings.builder().build()).dumpToString(map);
    }
}
