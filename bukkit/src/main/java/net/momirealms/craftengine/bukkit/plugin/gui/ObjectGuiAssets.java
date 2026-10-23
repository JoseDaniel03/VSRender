package net.momirealms.craftengine.bukkit.plugin.gui;

import net.momirealms.craftengine.core.pack.Pack;
import net.momirealms.craftengine.core.plugin.CraftEngine;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Paths and image-based titles for object-owned GUIs.
 *
 * This is intentionally not a new inventory system: storage remains managed by
 * simple_storage_block / simple_storage_furniture, and the vanilla player
 * inventory retains its original slots and click semantics.
 */
public final class ObjectGuiAssets {
    private ObjectGuiAssets() {}

    public static boolean validSegment(String value) {
        return value != null && value.matches("[a-z0-9][a-z0-9_.-]{0,47}")
                && !value.equals(".") && !value.equals("..");
    }

    public static boolean validType(String value) {
        return value != null && (value.equals("items") || value.equals("blocks")
                || value.equals("furniture"));
    }

    public static void validate(String pack, String category, String type, String id) {
        if (!validSegment(pack) || !validSegment(category) || !validSegment(id) || !validType(type)) {
            throw new IllegalArgumentException(
                    "Usa pack/categoria/id en minusculas y tipo items, blocks o furniture");
        }
    }

    public static Path objectFolder(CraftEngine plugin, String pack, String category, String type, String id) {
        validate(pack, category, type, id);
        return plugin.dataFolderPath().resolve("resources").resolve(pack).resolve("bibliotecas")
                .resolve(category).resolve(type).resolve(id);
    }

    public static String imageId(String namespace, String category, String type, String id) {
        return namespace + ":gui/" + category + "/" + type + "/" + id;
    }

    public static String imageTitle(String imageId) {
        return "<white><shift:-11><image:" + imageId + ">";
    }

    /**
     * Optional behavior setting: gui: "pack:category/type/id".
     * Only enables the PNG title when an import has generated the font config.
     */
    public static String titleFor(CraftEngine plugin, String reference, String fallback) {
        if (reference == null || reference.isBlank()) return fallback;
        String[] parts = reference.split(":", 2);
        if (parts.length != 2) return fallback;
        String[] object = parts[1].split("/", -1);
        if (object.length != 3) return fallback;
        try {
            validate(parts[0], object[0], object[1], object[2]);
            for (Pack pack : plugin.packManager().loadedPacks()) {
                if (!pack.name().equals(parts[0]) || !pack.enabled()) continue;
                Path imported = pack.configurationFolder().resolve("gui").resolve("objects")
                        .resolve(object[0]).resolve(object[1]).resolve(object[2] + ".yml");
                if (Files.isRegularFile(imported)) {
                    return imageTitle(imageId(pack.namespace(), object[0], object[1], object[2]));
                }
            }
        } catch (IllegalArgumentException ignored) {
            // Invalid GUI references must not stop existing storage from opening.
        }
        return fallback;
    }
}
