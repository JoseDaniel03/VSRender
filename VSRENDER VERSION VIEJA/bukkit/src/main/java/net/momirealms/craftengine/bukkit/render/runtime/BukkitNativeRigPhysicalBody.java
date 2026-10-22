package net.momirealms.craftengine.bukkit.render.runtime;

import net.momirealms.craftengine.core.world.collision.AABB;

/**
 * Server-side physical body for a compiled native rig.
 *
 * <p>The packet-only Interaction envelope remains responsible for clicks/attacks. Implementations
 * of this contract exist only to participate in server collision and owner contact effects, so they
 * must never become a second damage or interaction target.</p>
 */
public interface BukkitNativeRigPhysicalBody extends AutoCloseable {

    /** Synchronizes every physical part as one atomic owner sample. */
    void sync(AABB[] bounds);

    @Override
    void close();
}