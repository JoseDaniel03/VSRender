package net.momirealms.craftengine.bukkit.plugin;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.bukkit.Bukkit;
import org.bukkit.Material;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Locale;

/**
 * Exporta los materiales ITEM del servidor real, sin catálogo estático en la web.
 * Snapshot de solo lectura. NO sirve peticiones HTTP ni escanea el disco del servidor.
 */
public final class VanillaMaterialCatalog {
    private VanillaMaterialCatalog() {
    }

    static void export(BukkitCraftEngine plugin) {
        try {
            JsonObject root = new JsonObject();
            root.addProperty("schema_version", 1);
            root.addProperty("minecraft_version", Bukkit.getServer().getMinecraftVersion());
            JsonArray materials = new JsonArray();
            Arrays.stream(Material.values())
                    .filter(Material::isItem)
                    .filter(m -> !m.isLegacy())
                    .sorted(Comparator.comparing(Enum::name))
                    .forEach(material -> {
                        JsonObject item = new JsonObject();
                        item.addProperty("id", "minecraft:" + material.name().toLowerCase(Locale.ROOT));
                        item.addProperty("block", material.isBlock());
                        item.addProperty("max_stack_size", material.getMaxStackSize());
                        materials.add(item);
                    });
            root.add("materials", materials);

            Path directory = plugin.dataFolderPath().resolve("content-data");
            Files.createDirectories(directory);
            Path target = directory.resolve("minecraft-materials.json");
            Path tmp = Files.createTempFile(directory, ".minecraft-materials-", ".tmp");
            try {
                String json = new GsonBuilder().setPrettyPrinting().create().toJson(root) + "\n";
                Files.writeString(tmp, json, StandardCharsets.UTF_8);
                try {
                    Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING,
                            StandardCopyOption.ATOMIC_MOVE);
                } catch (AtomicMoveNotSupportedException exception) {
                    Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
                }
            } finally {
                Files.deleteIfExists(tmp);
            }
        } catch (IOException | RuntimeException exception) {
            plugin.logger().warn("VSRender: could not export vanilla material catalog", exception);
        }
    }
}
