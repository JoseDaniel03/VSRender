package com.virgolandstudio.items.core.projectile;

import com.virgolandstudio.items.core.instance.ItemInstance;
import com.virgolandstudio.items.core.math.Vec3;
import com.virgolandstudio.items.core.ranged.CompiledImpactResponse;
import com.virgolandstudio.items.core.ranged.CompiledProjectile;

import java.util.Objects;
import java.util.UUID;

public final class VirtualProjectile {
    private final long id;
    private final UUID worldId;
    private final UUID ownerId;
    private final ItemInstance sourceItem;
    private final CompiledProjectile spec;
    private final CompiledImpactResponse impactResponse;
    private Vec3 position;
    private Vec3 velocity;
    private int ageTicks;
    private double traveledDistance;
    private int entityPenetrationCount;
    private int blockPenetrationCount;
    private int ricochetCount;
    private UUID[] penetratedEntities;
    private double remainingBlockPenetrationPower;

    VirtualProjectile(long id, UUID worldId, UUID ownerId, ItemInstance sourceItem, CompiledProjectile spec, Vec3 position, Vec3 velocity) {
        this.id = id;
        this.worldId = Objects.requireNonNull(worldId, "worldId");
        this.ownerId = Objects.requireNonNull(ownerId, "ownerId");
        this.sourceItem = Objects.requireNonNull(sourceItem, "sourceItem");
        this.spec = Objects.requireNonNull(spec, "spec");
        this.position = Objects.requireNonNull(position, "position");
        this.velocity = Objects.requireNonNull(velocity, "velocity");
        var ranged = sourceItem.definition().rangedSystem();
        this.impactResponse = ranged == null ? null : ranged.impactResponse();
        this.remainingBlockPenetrationPower = impactResponse == null ? 0.0 : impactResponse.blockPenetrationPower();
    }

    public long id() { return id; }
    public UUID worldId() { return worldId; }
    public UUID ownerId() { return ownerId; }
    public ItemInstance sourceItem() { return sourceItem; }
    public CompiledProjectile spec() { return spec; }
    public CompiledImpactResponse impactResponse() { return impactResponse; }
    public Vec3 position() { return position; }
    public Vec3 velocity() { return velocity; }
    public int ageTicks() { return ageTicks; }
    public double traveledDistance() { return traveledDistance; }
    public int penetrationCount() { return entityPenetrationCount; }
    public int entityPenetrationCount() { return entityPenetrationCount; }
    public int blockPenetrationCount() { return blockPenetrationCount; }
    public int ricochetCount() { return ricochetCount; }
    public double remainingBlockPenetrationPower() { return remainingBlockPenetrationPower; }

    public boolean hasPenetratedEntity(UUID entityId) {
        if (penetratedEntities == null || entityId == null) return false;
        for (int i = 0; i < entityPenetrationCount; i++) {
            if (entityId.equals(penetratedEntities[i])) return true;
        }
        return false;
    }

    void recordEntityPenetration(UUID entityId) {
        if (entityId == null || hasPenetratedEntity(entityId)) return;
        if (penetratedEntities == null) penetratedEntities = new UUID[Math.max(1, spec.maxEntityPenetrations())];
        if (entityPenetrationCount >= penetratedEntities.length) return;
        penetratedEntities[entityPenetrationCount++] = entityId;
    }

    void recordRicochet() { ricochetCount++; }

    void recordBlockPenetration(double resistanceCost) {
        if (resistanceCost < 0.0 || !Double.isFinite(resistanceCost)) {
            throw new IllegalArgumentException("resistanceCost must be finite and >= 0");
        }
        blockPenetrationCount++;
        remainingBlockPenetrationPower = Math.max(0.0, remainingBlockPenetrationPower - resistanceCost);
    }

    void advance(Vec3 newPosition, Vec3 newVelocity, double distance) {
        this.position = newPosition;
        this.velocity = newVelocity;
        this.ageTicks++;
        this.traveledDistance += distance;
    }
}
