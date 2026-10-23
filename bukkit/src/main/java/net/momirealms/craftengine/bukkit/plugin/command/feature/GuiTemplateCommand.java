package net.momirealms.craftengine.bukkit.plugin.command.feature;

import net.momirealms.craftengine.bukkit.api.BukkitAdaptor;
import net.momirealms.craftengine.bukkit.plugin.command.BukkitCommandFeature;
import net.momirealms.craftengine.bukkit.plugin.user.BukkitServerPlayer;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.command.CraftEngineCommandManager;
import net.momirealms.craftengine.core.plugin.context.PlayerOptionalContext;
import net.momirealms.craftengine.core.plugin.gui.BasicGui;
import net.momirealms.craftengine.core.plugin.gui.GuiElement;
import net.momirealms.craftengine.core.plugin.gui.GuiLayout;
import net.momirealms.craftengine.core.util.AdventureHelper;
import net.momirealms.craftengine.core.util.Key;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.incendo.cloud.Command;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.parser.standard.StringParser;
import org.snakeyaml.engine.v2.api.Load;
import org.snakeyaml.engine.v2.api.LoadSettings;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Map;

/** Generates editable GUI YAML and previews it using the existing BasicGui. */
public final class GuiTemplateCommand extends BukkitCommandFeature<CommandSender> {
    private static final int MAX_TEMPLATE_BYTES = 65536;
    private final boolean create;

    public GuiTemplateCommand(CraftEngineCommandManager<CommandSender> manager, CraftEngine plugin, boolean create) {
        super(manager, plugin);
        this.create = create;
    }

    @Override
    public String getFeatureID() {
        return create ? "create_gui" : "open_gui";
    }

    @Override
    public Command.Builder<? extends CommandSender> assembleCommand(
            CommandManager<CommandSender> manager, Command.Builder<CommandSender> builder) {
        var b = builder.required("pack", StringParser.stringParser())
                .required("name", StringParser.stringParser());
        if (create) b = b.optional("rows", StringParser.stringParser());
        return b.handler(context -> {
            String pack = context.get("pack");
            String name = context.get("name");
            if (!isSimpleName(pack) || !isSimpleName(name)) {
                context.sender().sendMessage("VSRender: usa nombres simples en minusculas, sin rutas.");
                return;
            }
            Path file = plugin().dataFolderPath().resolve("resources").resolve(pack)
                    .resolve("configuration").resolve("gui").resolve(name + ".yml");
            if (create) {
                try {
                    int rows = Integer.parseInt(context.getOrDefault("rows", "3"));
                    if (rows < 1 || rows > 6) {
                        throw new IllegalArgumentException("Las filas deben estar entre 1 y 6");
                    }
                    boolean packExists = plugin().packManager().loadedPacks().stream()
                            .anyMatch(p -> p.name().equals(pack) && p.enabled());
                    if (!packExists) {
                        context.sender().sendMessage("VSRender: el pack debe existir y estar habilitado.");
                        return;
                    }
                    Files.createDirectories(file.getParent());
                    Files.writeString(file, defaultYaml(name, rows),
                            StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
                    context.sender().sendMessage("VSRender: GUI creada en " + file);
                    context.sender().sendMessage("Abrila con /vsr gui open " + pack + " " + name);
                } catch (java.nio.file.FileAlreadyExistsException ex) {
                    context.sender().sendMessage("VSRender: esta GUI ya existe. No se sobreescribio.");
                } catch (IllegalArgumentException ex) {
                    context.sender().sendMessage("VSRender: filas debe ser un entero entre 1 y 6.");
                } catch (IOException ex) {
                    plugin().logger().warn("Failed to create GUI template", ex);
                    context.sender().sendMessage("VSRender: no se pudo crear la GUI.");
                }
                return;
            }
            if (!(context.sender() instanceof Player player)) {
                context.sender().sendMessage("VSRender: abre las GUIs desde el juego.");
                return;
            }
            BukkitServerPlayer viewer = BukkitAdaptor.adapt(player);
            if (viewer == null) return;
            try {
                if (!Files.isRegularFile(file) || Files.size(file) > MAX_TEMPLATE_BYTES) {
                    context.sender().sendMessage("VSRender: no se encontro una GUI valida.");
                    return;
                }
                Object parsed = new Load(LoadSettings.builder().build())
                        .loadFromString(Files.readString(file));
                if (!(parsed instanceof Map<?, ?> config)) {
                    throw new IllegalArgumentException("La configuracion no es un objeto YAML");
                }
                Object rawRows = config.get("rows");
                if (!(rawRows instanceof Number n) || n.doubleValue() != n.intValue()
                        || n.intValue() < 1 || n.intValue() > 6) {
                    throw new IllegalArgumentException("rows debe ser un entero entre 1 y 6");
                }
                int rows = ((Number) rawRows).intValue();
                String[] grid = new String[rows];
                for (int row = 0; row < rows; row++) {
                    StringBuilder cells = new StringBuilder(9);
                    for (int col = 0; col < 9; col++) {
                        cells.append((char) (0x100 + row * 9 + col));
                    }
                    grid[row] = cells.toString();
                }
                GuiLayout layout = new GuiLayout(grid);
                // Keep all unoccupied preview cells empty; GuiLayout needs an element for each symbol.
                for (int slot = 0; slot < rows * 9; slot++) {
                    layout.addIngredient((char) (0x100 + slot), GuiElement.EMPTY);
                }
                Object rawButtons = config.get("buttons");
                if (rawButtons != null && !(rawButtons instanceof Map<?, ?>)) {
                    throw new IllegalArgumentException("buttons debe ser un mapa de casillas");
                }
                if (rawButtons instanceof Map<?, ?> buttons) {
                    for (var entry : buttons.entrySet()) {
                        int slot = Integer.parseInt(entry.getKey().toString());
                        if (slot < 0 || slot >= rows * 9 || !(entry.getValue() instanceof Map<?, ?> button)) {
                            throw new IllegalArgumentException("Casilla invalida: " + entry.getKey());
                        }
                        Object rawItem = button.get("item");
                        if (!(rawItem instanceof String id) || id.isBlank()) {
                            throw new IllegalArgumentException("Falta item en la casilla " + slot);
                        }
                        Item item = Item.byId(Key.of(id), viewer);
                        if (item == null) {
                            throw new IllegalArgumentException("Item desconocido: " + id);
                        }
                        Object rawAction = button.get("action");
                        String action = rawAction == null ? "none" : rawAction.toString();
                        if (!action.equals("none") && !action.equals("close")) {
                            throw new IllegalArgumentException("Accion no permitida: " + action);
                        }
                        layout.addIngredient((char) (0x100 + slot), GuiElement.constant(item, (element, click) -> {
                            click.cancel();
                            if (action.equals("close")) click.clicker().closeInventory();
                        }));
                    }
                }
                String title = config.get("title") instanceof String value ? value : name;
                BasicGui.builder().layout(layout).build()
                        .title(AdventureHelper.deserialize(title, PlayerOptionalContext.of(viewer)))
                        .refresh().open(viewer);
            } catch (IOException ex) {
                plugin().logger().warn("Failed to read GUI template", ex);
                context.sender().sendMessage("VSRender: error al leer la GUI.");
            } catch (RuntimeException ex) {
                context.sender().sendMessage("VSRender: plantilla invalida: " + ex.getMessage());
            }
        });
    }

    private static boolean isSimpleName(String value) {
        return value != null && value.matches("[a-z0-9][a-z0-9_.-]{0,47}")
                && !value.equals(".") && !value.equals("..");
    }

    private static String defaultYaml(String name, int rows) {
        return """
                # GUI editable de VSRender. Slots numerados de 0 a filas*9-1.
                # Para un PNG propio registra una imagen del pack y colocala en title.
                # Ejemplo: title: "<white><shift:-11><image:demo:mi_fondo>"
                title: "<white>%s"
                rows: %d
                buttons:
                  %d:
                    item: minecraft:emerald
                    action: none
                  %d:
                    item: minecraft:barrier
                    action: close
                """.formatted(name, rows, rows * 9 / 2, rows * 9 - 1);
    }
}
