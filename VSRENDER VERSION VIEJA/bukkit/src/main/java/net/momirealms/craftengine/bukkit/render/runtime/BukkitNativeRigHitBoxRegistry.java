package net.momirealms.craftengine.bukkit.render.runtime;

import net.momirealms.craftengine.core.world.collision.AABB;
import net.momirealms.craftengine.core.render.rig.RenderMountPolicy;
import net.momirealms.craftengine.core.world.Vec3d;
import org.bukkit.entity.Player;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

/**
 * Value-oriented lookup for packet-only native rig hitboxes.
 * Network listeners use it only to validate the exact AABB and forward the packet to the real owner.
 */
public final class BukkitNativeRigHitBoxRegistry {
    private static final ConcurrentHashMap<Integer, Target> TARGETS = new ConcurrentHashMap<>();
    // Minecraft ProjectileUtil.DEFAULT_ENTITY_HIT_RESULT_MARGIN for 26.2 / 1.21.10.
    private static final double PROJECTILE_MARGIN = 0.30000001192092896d;

    private BukkitNativeRigHitBoxRegistry() {
    }

    static void register(int entityId, UUID ownerUuid, int ownerEntityId, org.bukkit.entity.Entity ownerEntity,
                         UUID worldUuid, int partIndex, AABB bounds,
                         RenderMountPolicy mountPolicy, BiConsumer<Integer, Player> mountRequest) {
        Objects.requireNonNull(ownerUuid, "ownerUuid");
        Objects.requireNonNull(ownerEntity, "ownerEntity");
        Objects.requireNonNull(worldUuid, "worldUuid");
        Objects.requireNonNull(bounds, "bounds");
        Objects.requireNonNull(mountPolicy, "mountPolicy");
        Target target = new Target(entityId, ownerUuid, ownerEntityId, ownerEntity, worldUuid, partIndex, bounds, mountPolicy, mountRequest);
        Target previous = TARGETS.putIfAbsent(entityId, target);
        if (previous != null) throw new IllegalStateException("duplicate native hitbox entity id: " + entityId);
    }

    static void update(int entityId, UUID worldUuid, AABB bounds) {
        Objects.requireNonNull(worldUuid, "worldUuid");
        Objects.requireNonNull(bounds, "bounds");
        Target target = TARGETS.get(entityId);
        if (target != null) {
            target.worldUuid = worldUuid;
            target.bounds = bounds;
        }
    }

    static void unregister(int entityId) {
        TARGETS.remove(entityId);
    }

    public static Target target(int entityId) {
        return TARGETS.get(entityId);
    }

    public static boolean isEmpty() {
        return TARGETS.isEmpty();
    }

    /**
     * Finds the closest exact native hitbox crossed by a projectile movement segment.
     * This is intentionally value-only: no Bukkit entity method is invoked while iterating the registry.
     */
    public static ProjectileHit nearestProjectileHit(UUID worldUuid, Vec3d start, Vec3d end,
                                                     UUID shooterUuid, UUID temporarilyIgnoredOwner) {
        Objects.requireNonNull(worldUuid, "worldUuid");
        Objects.requireNonNull(start, "start");
        Objects.requireNonNull(end, "end");
        ProjectileHit best = null;
        double bestDistance = Double.POSITIVE_INFINITY;
        int bestEntityId = Integer.MAX_VALUE;
        for (Target target : TARGETS.values()) {
            if (!worldUuid.equals(target.worldUuid)) continue;
            if (shooterUuid != null && shooterUuid.equals(target.ownerUuid)) continue;
            if (temporarilyIgnoredOwner != null && temporarilyIgnoredOwner.equals(target.ownerUuid)) continue;
            AABB bounds = inflate(target.bounds, PROJECTILE_MARGIN);
            Vec3d point;
            if (contains(bounds, start)) {
                point = start;
            } else {
                var clipped = bounds.clip(start, end);
                if (clipped.isEmpty()) continue;
                point = clipped.get().hitLocation();
            }
            double distance = Vec3d.distanceToSqr(start, point);
            if (distance < bestDistance || (Double.compare(distance, bestDistance) == 0 && target.entityId < bestEntityId)) {
                bestDistance = distance;
                bestEntityId = target.entityId;
                best = new ProjectileHit(target, point, distance);
            }
        }
        return best;
    }

    public static boolean ownerContains(UUID ownerUuid, UUID worldUuid, Vec3d point) {
        if (ownerUuid == null || worldUuid == null || point == null) return false;
        for (Target target : TARGETS.values()) {
            if (ownerUuid.equals(target.ownerUuid) && worldUuid.equals(target.worldUuid) && contains(target.bounds, point)) {
                return true;
            }
        }
        return false;
    }


    private static AABB inflate(AABB box, double amount) {
        return new AABB(
                box.minX - amount, box.minY - amount, box.minZ - amount,
                box.maxX + amount, box.maxY + amount, box.maxZ + amount
        );
    }

    private static boolean contains(AABB box, Vec3d point) {
        return point.x >= box.minX && point.x <= box.maxX
                && point.y >= box.minY && point.y <= box.maxY
                && point.z >= box.minZ && point.z <= box.maxZ;
    }

    public record ProjectileHit(Target target, Vec3d point, double distanceSquared) {}

    public static final class Target {
        private final int entityId;
        private final UUID ownerUuid;
        private final int ownerEntityId;
        private final org.bukkit.entity.Entity ownerEntity;
        private final int partIndex;
        private final RenderMountPolicy mountPolicy;
        private final BiConsumer<Integer, Player> mountRequest;
        private volatile UUID worldUuid;
        private volatile AABB bounds;

        private Target(int entityId, UUID ownerUuid, int ownerEntityId, org.bukkit.entity.Entity ownerEntity,
                       UUID worldUuid, int partIndex, AABB bounds,
                       RenderMountPolicy mountPolicy, BiConsumer<Integer, Player> mountRequest) {
            this.entityId = entityId;
            this.ownerUuid = ownerUuid;
            this.ownerEntityId = ownerEntityId;
            this.ownerEntity = ownerEntity;
            this.worldUuid = worldUuid;
            this.partIndex = partIndex;
            this.bounds = bounds;
            this.mountPolicy = mountPolicy;
            this.mountRequest = mountRequest;
        }

        public int entityId() { return entityId; }
        public UUID ownerUuid() { return ownerUuid; }
        public int ownerEntityId() { return ownerEntityId; }
        public org.bukkit.entity.Entity ownerEntity() { return ownerEntity; }
        public UUID worldUuid() { return worldUuid; }
        public int partIndex() { return partIndex; }
        public AABB bounds() { return bounds; }
        public RenderMountPolicy mountPolicy() { return mountPolicy; }
        public boolean requestMount(Player player) {
            if (!mountPolicy.canMount() || mountRequest == null || player == null) return false;
            mountRequest.accept(partIndex, player);
            return true;
        }
    }
}
