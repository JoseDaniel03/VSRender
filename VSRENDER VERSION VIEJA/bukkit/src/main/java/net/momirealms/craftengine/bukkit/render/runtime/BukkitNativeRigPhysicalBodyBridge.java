package net.momirealms.craftengine.bukkit.render.runtime;

import net.momirealms.craftengine.core.world.collision.AABB;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Explicit installation point for the versioned Canvas/Paper collision backend.
 *
 * <p>There is intentionally no packet-only/no-op fallback here. A native rig that declares hitboxes
 * requires a physical backend on the supported Canvas 26.2 target; silently dropping the body would
 * turn a startup/configuration problem into false feature parity.</p>
 */
public final class BukkitNativeRigPhysicalBodyBridge {
    private static final AtomicReference<BukkitNativeRigPhysicalBodyFactory> FACTORY = new AtomicReference<>();

    private BukkitNativeRigPhysicalBodyBridge() {
    }

    public static void install(BukkitNativeRigPhysicalBodyFactory factory) {
        Objects.requireNonNull(factory, "factory");
        BukkitNativeRigPhysicalBodyFactory current = FACTORY.get();
        if (current == factory) return;
        if (current != null && current.getClass() == factory.getClass()) return;
        if (!FACTORY.compareAndSet(null, factory)) {
            throw new IllegalStateException("native rig physical-body backend already installed");
        }
    }

    static BukkitNativeRigPhysicalBody create(Object minecraftOwner, UUID ownerUuid, AABB[] initialBounds) {
        BukkitNativeRigPhysicalBodyFactory factory = FACTORY.get();
        if (factory == null) {
            throw new IllegalStateException(
                    "VsRender native physical hitbox backend is unavailable; Canvas/Paper 26.2 loader was not installed"
            );
        }
        return factory.create(
                Objects.requireNonNull(minecraftOwner, "minecraftOwner"),
                Objects.requireNonNull(ownerUuid, "ownerUuid"),
                Objects.requireNonNull(initialBounds, "initialBounds")
        );
    }

    static boolean installed() {
        return FACTORY.get() != null;
    }
}