package net.momirealms.craftengine.bukkit.render.runtime;

import net.momirealms.craftengine.core.render.rig.RenderMountPolicy;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry for real server-side native rig seat carriers.
 * Event listeners only inspect immutable policy/identity state here; all entity mutations are
 * delegated back to the owning render instance scheduler.
 */
public final class BukkitNativeRigMountRegistry {
    private static final ConcurrentHashMap<UUID, Target> BY_CARRIER = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<UUID, Target> BY_RIDER = new ConcurrentHashMap<>();

    private BukkitNativeRigMountRegistry() {
    }

    static Target register(Entity carrier, UUID ownerUuid, int partIndex, RenderMountPolicy policy,
                           BukkitNativeRigMountInstance instance) {
        Objects.requireNonNull(carrier, "carrier");
        Objects.requireNonNull(ownerUuid, "ownerUuid");
        Objects.requireNonNull(policy, "policy");
        Objects.requireNonNull(instance, "instance");
        Target target = new Target(carrier.getUniqueId(), ownerUuid, partIndex, policy, instance);
        Target previous = BY_CARRIER.putIfAbsent(target.carrierUuid, target);
        if (previous != null) throw new IllegalStateException("duplicate native mount carrier: " + target.carrierUuid);
        return target;
    }

    static void unregister(Target target) {
        if (target == null) return;
        UUID rider;
        synchronized (target) {
            if (!target.active) return;
            target.active = false;
            BY_CARRIER.remove(target.carrierUuid, target);
            rider = target.riderUuid;
            target.riderUuid = null;
        }
        if (rider != null) BY_RIDER.remove(rider, target);
    }

    static boolean mounted(Target target, Player rider) {
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(rider, "rider");
        UUID riderUuid = rider.getUniqueId();
        Target previous;
        synchronized (target) {
            if (!target.active || BY_CARRIER.get(target.carrierUuid) != target) return false;
            previous = BY_RIDER.put(riderUuid, target);
            target.riderUuid = riderUuid;
        }
        if (previous != null && previous != target) previous.clearRider(riderUuid);
        return true;
    }

    static void dismounted(Target target, UUID riderUuid) {
        if (target == null || riderUuid == null) return;
        synchronized (target) {
            if (riderUuid.equals(target.riderUuid)) target.riderUuid = null;
        }
        BY_RIDER.remove(riderUuid, target);
    }

    public static boolean isCarrier(UUID entityUuid) {
        return entityUuid != null && BY_CARRIER.containsKey(entityUuid);
    }

    /** Value-only collision query scoped to one rendered owner. */
    public static boolean isCarrierForOwner(UUID ownerUuid, UUID entityUuid) {
        if (ownerUuid == null || entityUuid == null) return false;
        Target target = BY_CARRIER.get(entityUuid);
        return target != null && target.active && ownerUuid.equals(target.ownerUuid);
    }

    /** Value-only collision query used by the physical hitbox backend. */
    public static boolean isRiderForOwner(UUID ownerUuid, UUID riderUuid) {
        if (ownerUuid == null || riderUuid == null) return false;
        Target target = BY_RIDER.get(riderUuid);
        return target != null && target.active && ownerUuid.equals(target.ownerUuid);
    }

    static int registeredCarrierCount() {
        return BY_CARRIER.size();
    }

    static int registeredRiderCount() {
        return BY_RIDER.size();
    }

    public static boolean shouldCancelSelfDismount(Entity vehicle) {
        if (vehicle == null) return false;
        Target target = BY_CARRIER.get(vehicle.getUniqueId());
        return target != null
                && target.active
                && !target.forceDismount
                && (target.policy.canFly() || !target.policy.canDismountBySelf());
    }

    public static void onDismounted(Entity vehicle, Player rider) {
        if (vehicle == null || rider == null) return;
        Target target = BY_CARRIER.get(vehicle.getUniqueId());
        if (target != null) dismounted(target, rider.getUniqueId());
    }

    public static boolean canRiderDamage(UUID ownerUuid, UUID riderUuid) {
        if (ownerUuid == null || riderUuid == null) return true;
        Target target = BY_RIDER.get(riderUuid);
        return target == null || !target.active || !ownerUuid.equals(target.ownerUuid) || target.policy.canBeDamagedByRider();
    }

    public static void forceDismount(UUID riderUuid) {
        if (riderUuid == null) return;
        Target target = BY_RIDER.get(riderUuid);
        if (target != null && target.active) target.instance.requestDismount(riderUuid);
    }

    static final class Target {
        private final UUID carrierUuid;
        private final UUID ownerUuid;
        private final int partIndex;
        private final RenderMountPolicy policy;
        private final BukkitNativeRigMountInstance instance;
        private volatile UUID riderUuid;
        private volatile boolean forceDismount;
        private volatile boolean active = true;

        private Target(UUID carrierUuid, UUID ownerUuid, int partIndex, RenderMountPolicy policy,
                       BukkitNativeRigMountInstance instance) {
            this.carrierUuid = carrierUuid;
            this.ownerUuid = ownerUuid;
            this.partIndex = partIndex;
            this.policy = policy;
            this.instance = instance;
        }

        UUID carrierUuid() { return carrierUuid; }
        UUID ownerUuid() { return ownerUuid; }
        int partIndex() { return partIndex; }
        RenderMountPolicy policy() { return policy; }
        UUID riderUuid() { return riderUuid; }
        boolean forceDismount() { return forceDismount; }
        void forceDismount(boolean value) { forceDismount = value; }
        boolean active() { return active; }

        private void clearRider(UUID riderUuid) {
            synchronized (this) {
                if (riderUuid.equals(this.riderUuid)) this.riderUuid = null;
            }
        }
    }
}