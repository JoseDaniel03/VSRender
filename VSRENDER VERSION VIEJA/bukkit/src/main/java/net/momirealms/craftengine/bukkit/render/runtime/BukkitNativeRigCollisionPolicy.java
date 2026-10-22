package net.momirealms.craftengine.bukkit.render.runtime;

/** Pure collision-exclusion policy shared by the versioned physical backend and its unit gate. */
public final class BukkitNativeRigCollisionPolicy {
    private BukkitNativeRigCollisionPolicy() {
    }

    public static boolean shouldCollide(boolean owner,
                                        boolean bodyPassenger,
                                        boolean ownerPassenger,
                                        boolean siblingBody,
                                        boolean mountRider,
                                        boolean mountCarrier) {
        return !owner
                && !bodyPassenger
                && !ownerPassenger
                && !siblingBody
                && !mountRider
                && !mountCarrier;
    }
}