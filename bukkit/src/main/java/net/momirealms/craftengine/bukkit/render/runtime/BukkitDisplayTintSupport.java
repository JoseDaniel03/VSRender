package net.momirealms.craftengine.bukkit.render.runtime;

import net.momirealms.craftengine.bukkit.item.ComponentItemWrapper;
import net.momirealms.craftengine.bukkit.item.DataComponentTypes;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.proxy.minecraft.world.item.ItemStackProxy;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Platform-only CustomModelData color tinting compatible with BetterModel's damage tint semantics. */
public final class BukkitDisplayTintSupport {
    private BukkitDisplayTintSupport() {}

    public static boolean supported() {
        return VersionHelper.isOrAbove1_21_4;
    }

    public static Object tint(Object minecraftItem, int rgb) {
        if (minecraftItem == null) return null;
        Object copy = ItemStackProxy.INSTANCE.copy(minecraftItem);
        if (!supported()) return copy;

        ComponentItemWrapper wrapper = new ComponentItemWrapper(copy);
        @SuppressWarnings("unchecked")
        Map<String, Object> existing = (Map<String, Object>) wrapper
                .getComponentAsJava(DataComponentTypes.CUSTOM_MODEL_DATA)
                .orElse(null);

        LinkedHashMap<String, Object> data = existing == null
                ? new LinkedHashMap<>()
                : new LinkedHashMap<>(existing);
        data.putIfAbsent("floats", List.of());
        data.putIfAbsent("flags", List.of());
        data.putIfAbsent("strings", List.of());

        @SuppressWarnings("unchecked")
        List<Integer> sourceColors = existing == null
                ? List.of()
                : (List<Integer>) existing.getOrDefault("colors", List.of());
        List<Integer> colors;
        if (sourceColors.isEmpty()) {
            colors = List.of(rgb & 0xFFFFFF);
        } else {
            colors = new ArrayList<>(sourceColors.size());
            for (int color : sourceColors) colors.add(multiplyRgb(color, rgb));
        }
        data.put("colors", colors);
        wrapper.setJavaComponent(DataComponentTypes.CUSTOM_MODEL_DATA, data);
        return wrapper.minecraftItem();
    }

    static int multiplyRgb(int base, int tint) {
        int br = (base >>> 16) & 0xFF;
        int bg = (base >>> 8) & 0xFF;
        int bb = base & 0xFF;
        int tr = (tint >>> 16) & 0xFF;
        int tg = (tint >>> 8) & 0xFF;
        int tb = tint & 0xFF;
        int r = br * tr / 255;
        int g = bg * tg / 255;
        int b = bb * tb / 255;
        return (r << 16) | (g << 8) | b;
    }
}
