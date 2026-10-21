package net.momirealms.craftengine.bukkit.render.runtime;

import net.momirealms.craftengine.bukkit.plugin.BukkitCraftEngine;
import net.momirealms.craftengine.core.plugin.scheduler.SchedulerTask;
import net.momirealms.craftengine.core.world.Vec3d;
import org.bukkit.Bukkit;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Projectile;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Region-owned projectile bridge for packet-only native rig hitboxes.
 *
 * <p>The interceptor predicts the projectile's vanilla movement segment from its current velocity,
 * compares the exact native AABB hit against Paper's own block/entity ray trace, and delegates the
 * winning collision back to {@link Projectile#hitEntity(Entity, Vector)}. Damage, deflection,
 * piercing and ProjectileHitEvent therefore remain owned by Paper/vanilla rather than being
 * reimplemented here.</p>
 */
final class BukkitNativeRigProjectileInterceptor {
    private static final double EPSILON = 1.0E-7;
    private static final double VANILLA_ENTITY_MARGIN = 0.3d;
    private static final ConcurrentHashMap<UUID, Tracker> TRACKERS = new ConcurrentHashMap<>();

    private BukkitNativeRigProjectileInterceptor() {
    }

    static void track(Projectile projectile) {
        if (projectile == null || !projectile.isValid()) return;
        TRACKERS.computeIfAbsent(projectile.getUniqueId(), ignored -> {
            Tracker tracker = new Tracker(projectile);
            tracker.start();
            return tracker;
        });
    }

    static void untrack(Projectile projectile) {
        if (projectile == null) return;
        Tracker tracker = TRACKERS.remove(projectile.getUniqueId());
        if (tracker != null) tracker.cancel();
    }

    static int trackedCount() {
        return TRACKERS.size();
    }

    private static final class Tracker implements Runnable {
        private final Projectile projectile;
        private final UUID projectileUuid;
        private volatile SchedulerTask task;
        private UUID lastHitOwner;

        private Tracker(Projectile projectile) {
            this.projectile = projectile;
            this.projectileUuid = projectile.getUniqueId();
        }

        private void start() {
            this.task = BukkitCraftEngine.instance().scheduler().platform().runRepeating(
                    this, this::retired, 1, 1, projectile);
        }

        @Override
        public void run() {
            if (!projectile.isValid() || projectile.isDead()) {
                finish();
                return;
            }
            if (BukkitNativeRigHitBoxRegistry.isEmpty()) return;

            Location startLocation = projectile.getLocation();
            Vector velocity = projectile.getVelocity();
            double distance = velocity.length();
            if (!(distance > EPSILON) || !Double.isFinite(distance)) return;

            UUID worldUuid = startLocation.getWorld().getUID();
            Vec3d start = new Vec3d(startLocation.getX(), startLocation.getY(), startLocation.getZ());
            Vec3d end = new Vec3d(start.x + velocity.getX(), start.y + velocity.getY(), start.z + velocity.getZ());

            if (lastHitOwner != null && !BukkitNativeRigHitBoxRegistry.ownerContains(lastHitOwner, worldUuid, start)) {
                lastHitOwner = null;
            }

            UUID shooterUuid = projectile.getOwnerUniqueId();
            BukkitNativeRigHitBoxRegistry.ProjectileHit hit = BukkitNativeRigHitBoxRegistry.nearestProjectileHit(
                    worldUuid, start, end, shooterUuid, lastHitOwner);
            if (hit == null) return;

            Entity owner = hit.target().ownerEntity();
            if (owner == null || !owner.isValid() || owner.isDead()) return;
            if (!Bukkit.isOwnedByCurrentRegion(owner)) return;
            if (!projectile.canHitEntity(owner)) return;

            Vector direction = velocity.clone().multiply(1.0d / distance);
            RayTraceResult vanilla = startLocation.getWorld().rayTrace(
                    startLocation, direction, distance, FluidCollisionMode.NEVER, false, VANILLA_ENTITY_MARGIN,
                    entity -> entity != projectile
                            && !BukkitNativeRigMountRegistry.isCarrier(entity.getUniqueId())
                            && projectile.canHitEntity(entity));
            if (vanilla != null) {
                double vanillaDistance = vanilla.getHitPosition().distanceSquared(startLocation.toVector());
                if (vanillaDistance <= hit.distanceSquared() + EPSILON) return;
            }

            Vec3d point = hit.point();
            lastHitOwner = hit.target().ownerUuid();
            projectile.hitEntity(owner, new Vector(point.x, point.y, point.z));
        }

        private void finish() {
            TRACKERS.remove(projectileUuid, this);
            cancel();
        }

        private void retired() {
            TRACKERS.remove(projectileUuid, this);
        }

        private void cancel() {
            SchedulerTask current = task;
            if (current != null && !current.cancelled()) current.cancel();
        }
    }
}
