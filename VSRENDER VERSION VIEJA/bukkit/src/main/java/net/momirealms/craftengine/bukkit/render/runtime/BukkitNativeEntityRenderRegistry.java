package net.momirealms.craftengine.bukkit.render.runtime;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/** Routes Bukkit entity lifecycle events to native packet-only render instances by owner UUID. */
public final class BukkitNativeEntityRenderRegistry {
    private static final ConcurrentHashMap<UUID, CopyOnWriteArraySet<BukkitEntityNativeRenderInstance>> INSTANCES = new ConcurrentHashMap<>();

    private BukkitNativeEntityRenderRegistry() {}

    static void register(UUID owner, BukkitEntityNativeRenderInstance instance) {
        INSTANCES.computeIfAbsent(owner, ignored -> new CopyOnWriteArraySet<>()).add(instance);
    }

    static void unregister(UUID owner, BukkitEntityNativeRenderInstance instance) {
        INSTANCES.computeIfPresent(owner, (ignored, set) -> {
            set.remove(instance);
            return set.isEmpty() ? null : set;
        });
    }

    public static void damage(UUID owner) {
        var set = INSTANCES.get(owner);
        if (set == null) return;
        for (BukkitEntityNativeRenderInstance instance : set) instance.onOwnerDamage();
    }

    public static void jump(UUID owner) {
        var set = INSTANCES.get(owner);
        if (set == null) return;
        for (BukkitEntityNativeRenderInstance instance : set) instance.onOwnerJump();
    }

    public static void death(UUID owner) {
        var set = INSTANCES.get(owner);
        if (set == null) return;
        for (BukkitEntityNativeRenderInstance instance : set) instance.onOwnerDeath();
    }

    static int instanceCount(UUID owner) {
        var set = INSTANCES.get(owner);
        return set == null ? 0 : set.size();
    }
}
