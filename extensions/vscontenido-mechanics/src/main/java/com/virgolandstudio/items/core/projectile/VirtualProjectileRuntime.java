package com.virgolandstudio.items.core.projectile;

import com.virgolandstudio.items.core.instance.ItemInstance;
import com.virgolandstudio.items.core.math.Vec3;
import com.virgolandstudio.items.core.ranged.CompiledMaterialImpact;
import com.virgolandstudio.items.core.ranged.CompiledProjectile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** One compact active set for all virtual projectiles. */
public final class VirtualProjectileRuntime {
    private static final double TRACE_EPSILON = 0.02;
    private static final double NUMERIC_EPSILON = 1.0e-9;

    private final int maxGlobal;
    private final int maxPerOwner;
    private final List<VirtualProjectile> active = new ArrayList<>();
    private final Map<UUID, Integer> ownerCounts = new HashMap<>();
    private final Map<Long, Long> lastProcessedLogicalTick = new HashMap<>();
    private long nextId = 1L;

    public VirtualProjectileRuntime() { this(2048, 256); }

    public VirtualProjectileRuntime(int maxGlobal, int maxPerOwner) {
        if (maxGlobal < 1 || maxPerOwner < 1) throw new IllegalArgumentException("Projectile limits must be >= 1");
        this.maxGlobal = maxGlobal;
        this.maxPerOwner = maxPerOwner;
    }

    public synchronized SpawnResult spawn(
            UUID worldId,
            UUID ownerId,
            ItemInstance sourceItem,
            CompiledProjectile spec,
            Vec3 position,
            Vec3 direction
    ) {
        return spawnDetailed(worldId, ownerId, sourceItem, spec, position, direction).result();
    }

    public synchronized SpawnOutcome spawnDetailed(
            UUID worldId,
            UUID ownerId,
            ItemInstance sourceItem,
            CompiledProjectile spec,
            Vec3 position,
            Vec3 direction
    ) {
        Objects.requireNonNull(worldId, "worldId");
        Objects.requireNonNull(ownerId, "ownerId");
        Objects.requireNonNull(sourceItem, "sourceItem");
        Objects.requireNonNull(spec, "spec");
        Objects.requireNonNull(position, "position");
        Objects.requireNonNull(direction, "direction");
        if (active.size() >= maxGlobal) return new SpawnOutcome(SpawnResult.GLOBAL_LIMIT, null);
        int ownerCount = ownerCounts.getOrDefault(ownerId, 0);
        if (ownerCount >= maxPerOwner) return new SpawnOutcome(SpawnResult.OWNER_LIMIT, null);
        Vec3 normalized = direction.normalize();
        if (normalized.lengthSquared() == 0.0) return new SpawnOutcome(SpawnResult.INVALID_DIRECTION, null);

        var projectile = new VirtualProjectile(
                nextId++, worldId, ownerId, sourceItem, spec, position, normalized.multiply(spec.speed())
        );
        active.add(projectile);
        ownerCounts.put(ownerId, ownerCount + 1);
        return new SpawnOutcome(SpawnResult.SPAWNED, projectile);
    }

    /** Legacy single-threaded tick retained for Core tests and non-region platforms. */
    public synchronized void tick(ProjectileEnvironment environment) {
        Objects.requireNonNull(environment, "environment");
        for (int index = active.size() - 1; index >= 0; index--) {
            VirtualProjectile projectile = active.get(index);
            tickSafely(index, projectile, environment);
        }
    }

    /** Immutable routing information used by Paper/Folia to choose the owning region. */
    public synchronized List<ProjectileTicket> tickTickets() {
        if (active.isEmpty()) return List.of();
        var result = new ArrayList<ProjectileTicket>(active.size());
        for (VirtualProjectile projectile : active) {
            result.add(new ProjectileTicket(projectile.id(), projectile.worldId(), projectile.position()));
        }
        return List.copyOf(result);
    }

    /**
     * Advances one projectile at most once for a shared logical tick. The caller must already own
     * the Paper region containing the ticket position.
     */
    public synchronized void tickProjectile(long projectileId, long logicalTick, ProjectileEnvironment environment) {
        Objects.requireNonNull(environment, "environment");
        Long previous = lastProcessedLogicalTick.get(projectileId);
        if (previous != null && previous >= logicalTick) return;

        int index = indexOf(projectileId);
        if (index < 0) return;
        VirtualProjectile projectile = active.get(index);
        lastProcessedLogicalTick.put(projectileId, logicalTick);
        tickSafely(index, projectile, environment);
    }

    private void tickSafely(int index, VirtualProjectile projectile, ProjectileEnvironment environment) {
        try {
            tickOne(index, projectile, environment);
        } catch (RuntimeException error) {
            try {
                environment.onError(projectile, error);
            } catch (RuntimeException ignored) {
                // Error reporting must never keep a broken projectile in the shared active set.
            }
            int located = index < active.size() && active.get(index) == projectile ? index : active.indexOf(projectile);
            if (located >= 0) removeAt(located, projectile);
        }
    }

    private void tickOne(int index, VirtualProjectile projectile, ProjectileEnvironment environment) {
        Vec3 from = projectile.position();
        Vec3 velocity = projectile.velocity();
        double velocityDistance = velocity.length();
        double remainingDistance = projectile.spec().maxDistance() - projectile.traveledDistance();
        double segmentDistance = Math.min(velocityDistance, Math.max(0.0, remainingDistance));
        if (segmentDistance <= NUMERIC_EPSILON) {
            environment.onExpire(projectile);
            removeAt(index, projectile);
            return;
        }
        Vec3 to = velocityDistance <= segmentDistance
                ? from.add(velocity)
                : from.add(velocity.normalize().multiply(segmentDistance));
        boolean reachedDistanceLimit = segmentDistance + NUMERIC_EPSILON >= remainingDistance;

        Vec3 direction = to.subtract(from).normalize();
        Vec3 traceStart = from;
        double collisionVelocityMultiplier = 1.0;
        int maxBlockPenetrations = projectile.impactResponse() == null ? 0 : projectile.impactResponse().maxBlockPenetrations();
        int maxRicochets = projectile.impactResponse() == null ? 0 : projectile.impactResponse().maxRicochets();
        int collisionGuard = projectile.spec().maxEntityPenetrations() + maxBlockPenetrations + maxRicochets + 1;

        for (int collisionIndex = 0; collisionIndex < collisionGuard; collisionIndex++) {
            if (!environment.canTrace(projectile, traceStart, to)) {
                environment.onExpire(projectile);
                removeAt(index, projectile);
                return;
            }

            ProjectileCollision collision = environment.trace(projectile, traceStart, to);
            environment.onSegment(projectile, traceStart, collision == null ? to : collision.position());
            if (collision == null) break;

            environment.onCollision(projectile, collision);
            Vec3 nextStart;
            if (collision.kind() == ProjectileCollisionKind.ENTITY) {
                boolean canPenetrate = collision.entityId() != null
                        && projectile.entityPenetrationCount() < projectile.spec().maxEntityPenetrations();
                if (!canPenetrate) {
                    removeAt(index, projectile);
                    return;
                }
                projectile.recordEntityPenetration(collision.entityId());
                collisionVelocityMultiplier *= projectile.spec().penetrationVelocityMultiplier();
                nextStart = collision.position().add(direction.multiply(TRACE_EPSILON));
            } else {
                CompiledMaterialImpact material = projectile.impactResponse() == null
                        ? null
                        : projectile.impactResponse().resolve(collision.blockMaterialId());
                double resistanceCost = material == null ? Double.POSITIVE_INFINITY
                        : material.resistancePerBlock() * collision.blockThickness();
                Vec3 exit = collision.blockExitPosition();
                boolean exitInsideThisTick = exit != null
                        && from.distance(exit) <= segmentDistance + TRACE_EPSILON;
                boolean canPenetrate = material != null
                        && material.penetrable()
                        && projectile.blockPenetrationCount() < maxBlockPenetrations
                        && projectile.remainingBlockPenetrationPower() + NUMERIC_EPSILON >= resistanceCost
                        && exitInsideThisTick;
                if (!canPenetrate) {
                    boolean canRicochet = material != null
                            && material.ricochetEnabled()
                            && projectile.ricochetCount() < maxRicochets
                            && collision.surfaceNormal() != null
                            && incidenceAngleDegrees(direction, collision.surfaceNormal()) + NUMERIC_EPSILON >= material.ricochetMinAngleDegrees();
                    if (!canRicochet) {
                        removeAt(index, projectile);
                        return;
                    }

                    Vec3 reflectedDirection = direction.reflect(collision.surfaceNormal()).normalize();
                    if (reflectedDirection.lengthSquared() <= NUMERIC_EPSILON) {
                        removeAt(index, projectile);
                        return;
                    }
                    projectile.recordRicochet();
                    environment.onRicochet(projectile, collision);
                    double retained = collisionVelocityMultiplier * material.ricochetVelocityRetention();
                    Vec3 reflectedVelocity = velocity.multiply(retained).reflect(collision.surfaceNormal())
                            .multiply(projectile.spec().drag())
                            .add(new Vec3(0.0, -projectile.spec().gravity(), 0.0));
                    Vec3 bouncedPosition = collision.position().add(reflectedDirection.multiply(TRACE_EPSILON));
                    double traveled = Math.min(segmentDistance, from.distance(collision.position()));
                    projectile.advance(bouncedPosition, reflectedVelocity, traveled);
                    if (projectile.ageTicks() >= projectile.spec().maxLifetimeTicks()
                            || projectile.traveledDistance() + NUMERIC_EPSILON >= projectile.spec().maxDistance()) {
                        environment.onExpire(projectile);
                        removeAt(index, projectile);
                    }
                    return;
                }
                projectile.recordBlockPenetration(resistanceCost);
                collisionVelocityMultiplier *= material.velocityRetention();
                nextStart = exit.add(direction.multiply(TRACE_EPSILON));
            }

            if (from.distance(nextStart) + NUMERIC_EPSILON >= segmentDistance) break;
            traceStart = nextStart;
        }

        Vec3 nextVelocity = velocity.multiply(projectile.spec().drag() * collisionVelocityMultiplier)
                .add(new Vec3(0.0, -projectile.spec().gravity(), 0.0));
        projectile.advance(to, nextVelocity, segmentDistance);
        if (reachedDistanceLimit
                || projectile.ageTicks() >= projectile.spec().maxLifetimeTicks()
                || projectile.traveledDistance() + NUMERIC_EPSILON >= projectile.spec().maxDistance()) {
            environment.onExpire(projectile);
            removeAt(index, projectile);
        }
    }

    private static double incidenceAngleDegrees(Vec3 direction, Vec3 surfaceNormal) {
        double cosine = Math.abs(direction.normalize().dot(surfaceNormal.normalize()));
        cosine = Math.max(0.0, Math.min(1.0, cosine));
        return Math.toDegrees(Math.acos(cosine));
    }

    public synchronized int activeCount() { return active.size(); }
    public synchronized int activeForOwner(UUID ownerId) { return ownerCounts.getOrDefault(ownerId, 0); }

    public synchronized void clear(ProjectileEnvironment environment) {
        for (VirtualProjectile projectile : List.copyOf(active)) environment.onExpire(projectile);
        active.clear();
        ownerCounts.clear();
        lastProcessedLogicalTick.clear();
    }

    private int indexOf(long projectileId) {
        for (int i = 0; i < active.size(); i++) {
            if (active.get(i).id() == projectileId) return i;
        }
        return -1;
    }

    private void removeAt(int index, VirtualProjectile projectile) {
        int last = active.size() - 1;
        if (index != last) active.set(index, active.get(last));
        active.remove(last);
        lastProcessedLogicalTick.remove(projectile.id());
        ownerCounts.compute(projectile.ownerId(), (id, count) -> count == null || count <= 1 ? null : count - 1);
    }

    public record ProjectileTicket(long projectileId, UUID worldId, Vec3 position) {}
    public record SpawnOutcome(SpawnResult result, VirtualProjectile projectile) {}

    public enum SpawnResult {
        SPAWNED,
        GLOBAL_LIMIT,
        OWNER_LIMIT,
        INVALID_DIRECTION
    }
}
