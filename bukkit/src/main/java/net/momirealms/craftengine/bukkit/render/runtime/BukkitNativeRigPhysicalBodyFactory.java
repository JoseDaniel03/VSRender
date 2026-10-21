package net.momirealms.craftengine.bukkit.render.runtime;

import net.momirealms.craftengine.core.world.collision.AABB;

import java.util.UUID;

/** Canvas/Paper version bridge for native rig physical bodies. */
@FunctionalInterface
public interface BukkitNativeRigPhysicalBodyFactory {
    BukkitNativeRigPhysicalBody create(Object minecraftOwner, UUID ownerUuid, AABB[] initialBounds);
}