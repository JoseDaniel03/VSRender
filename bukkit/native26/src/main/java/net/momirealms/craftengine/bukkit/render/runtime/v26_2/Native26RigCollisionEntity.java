package net.momirealms.craftengine.bukkit.render.runtime.v26_2;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.momirealms.craftengine.bukkit.render.runtime.BukkitNativeRigCollisionPolicy;
import net.momirealms.craftengine.bukkit.render.runtime.BukkitNativeRigMountRegistry;
import org.bukkit.event.entity.CreatureSpawnEvent;

import java.util.Objects;
import java.util.UUID;

/**
 * Invisible, non-pickable 26.2 server collision body for one native rig hitbox.
 *
 * <p>Damage, interaction and projectile routing remain owned by VsRender's packet/exact-AABB
 * hitbox layer. This entity only participates in physical entity collision and forwards contact
 * block/lava effects to the real owner.</p>
 */
final class Native26RigCollisionEntity extends ArmorStand {
    private final Entity owner;
    private final UUID ownerUuid;
    private final InsideBlockEffectApplier.StepBasedCollector effects = new InsideBlockEffectApplier.StepBasedCollector();
    private AABB exactBounds;
    private Vec3 sampledPosition;
    private boolean initialized;

    Native26RigCollisionEntity(ServerLevel level, Entity owner, UUID ownerUuid, AABB initialBounds) {
        super(EntityTypes.ARMOR_STAND, level);
        this.owner = Objects.requireNonNull(owner, "owner");
        this.ownerUuid = Objects.requireNonNull(ownerUuid, "ownerUuid");
        this.exactBounds = Objects.requireNonNull(initialBounds, "initialBounds");
        this.setInvisible(true);
        this.setSilent(true);
        // 26.3 NMS no longer exposes Entity#setInvulnerable(boolean).
        // The body is non-targetable and hurtServer(...) below rejects damage.
        this.setNoGravity(true);
        this.initialized = true;
        setExactBounds(initialBounds);
        this.sampledPosition = position();
    }

    void spawn() {
        if (!(level() instanceof ServerLevel level)) {
            throw new IllegalStateException("collision body is not in a ServerLevel");
        }
        if (!level.addFreshEntity(this, CreatureSpawnEvent.SpawnReason.CUSTOM)) {
            throw new IllegalStateException("failed to add VsRender physical collision body");
        }
    }

    void sync(AABB bounds) {
        if (isRemoved()) return;
        if (owner.isRemoved()) {
            destroy();
            return;
        }
        Vec3 previous = sampledPosition == null ? position() : sampledPosition;
        this.exactBounds = Objects.requireNonNull(bounds, "bounds");
        setExactBounds(bounds);
        Vec3 current = position();
        forwardContactEffects(previous, current, bounds);
        this.sampledPosition = current;
    }

    void destroy() {
        if (!isRemoved()) remove(RemovalReason.DISCARDED);
    }

    private void setExactBounds(AABB bounds) {
        double x = (bounds.minX + bounds.maxX) * 0.5d;
        double y = bounds.minY;
        double z = (bounds.minZ + bounds.maxZ) * 0.5d;
        setPos(x, y, z);
        setBoundingBox(bounds);
    }

    private void forwardContactEffects(Vec3 previous, Vec3 current, AABB bounds) {
        if (!(level() instanceof ServerLevel level) || owner.isRemoved()) return;
        boolean[] lava = {false};
        BlockGetter.forEachBlockIntersectedBetween(previous, current, bounds, (BlockPos pos, int step) -> {
            if (owner.isRemoved()) return false;
            BlockState state = level.getBlockState(pos);
            effects.advanceStep(step, pos);
            state.entityInside(level, pos, owner, effects, true);
            if (state.getFluidState().is(FluidTags.LAVA)) lava[0] = true;
            return true;
        });
        effects.applyAndClear(owner);
        if (lava[0] && !owner.isRemoved()) owner.lavaHurt();
    }

    private boolean shouldCollide(Entity other) {
        if (other == null) return true;
        boolean sibling = other instanceof Native26RigCollisionEntity body && body.owner == owner;
        return BukkitNativeRigCollisionPolicy.shouldCollide(
                other == owner,
                getPassengers().contains(other),
                owner.getPassengers().contains(other),
                sibling,
                BukkitNativeRigMountRegistry.isRiderForOwner(ownerUuid, other.getUUID()),
                BukkitNativeRigMountRegistry.isCarrierForOwner(ownerUuid, other.getUUID())
        );
    }

    @Override
    public boolean isPushable() {
        return !isRemoved() && !owner.isRemoved();
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean canBeHitByProjectile() {
        return false;
    }

    @Override
    public boolean canCollideWith(Entity other) {
        return shouldCollide(other) && owner.canCollideWith(other);
    }

    @Override
    public boolean canCollideWithBukkit(Entity other) {
        return shouldCollide(other) && owner.canCollideWithBukkit(other);
    }

    @Override
    public boolean canBeCollidedWith(Entity other) {
        return shouldCollide(other);
    }

    @Override
    public boolean isCollidable(boolean ignoreClimbing) {
        return owner.isCollidable(ignoreClimbing);
    }

    @Override
    public void push(Entity pushingEntity) {
        if (!shouldCollide(pushingEntity)) return;
        owner.push(pushingEntity);
    }

    @Override
    public void push(double x, double y, double z, Entity pushingEntity) {
        if (pushingEntity != null && !shouldCollide(pushingEntity)) return;
        owner.push(x, y, z, pushingEntity);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public void tick() {
        if (owner.isRemoved()) destroy();
    }

    @Override
    protected AABB makeBoundingBox(Vec3 position) {
        return initialized && exactBounds != null ? exactBounds : super.makeBoundingBox(position);
    }

    @Override
    public net.minecraft.world.entity.EntityDimensions getDefaultDimensions(Pose pose) {
        return initialized && exactBounds != null
                ? net.minecraft.world.entity.EntityDimensions.fixed(
                        (float) Math.max(0.001d, exactBounds.getXsize()),
                        (float) Math.max(0.001d, exactBounds.getYsize())
                )
                : super.getDefaultDimensions(pose);
    }
}
