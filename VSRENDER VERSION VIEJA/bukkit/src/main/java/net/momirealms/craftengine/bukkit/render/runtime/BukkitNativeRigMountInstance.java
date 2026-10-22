package net.momirealms.craftengine.bukkit.render.runtime;

import net.momirealms.craftengine.bukkit.plugin.BukkitCraftEngine;
import net.momirealms.craftengine.core.entity.Entity;
import net.momirealms.craftengine.core.render.compiled.IndexedRenderRig;
import net.momirealms.craftengine.core.render.compiled.RenderHitBoxBuffer;
import net.momirealms.craftengine.core.render.compiled.RenderMountBuffer;
import net.momirealms.craftengine.core.render.rig.RenderMountController;
import net.momirealms.craftengine.core.render.rig.RenderMountInput;
import net.momirealms.craftengine.core.render.rig.RenderMountMotion;
import net.momirealms.craftengine.core.render.rig.RenderMountPolicy;
import net.momirealms.craftengine.core.render.runtime.CompiledRenderRuntime;
import net.momirealms.craftengine.core.plugin.scheduler.PlatformExecutor;
import net.momirealms.craftengine.core.world.WorldPosition;
import net.momirealms.craftengine.proxy.bukkit.craftbukkit.entity.CraftEntityProxy;
import net.momirealms.craftengine.proxy.minecraft.world.entity.EntityProxy;
import net.momirealms.craftengine.proxy.minecraft.world.entity.LivingEntityProxy;
import net.momirealms.craftengine.proxy.minecraft.world.entity.MoverTypeProxy;
import net.momirealms.craftengine.proxy.minecraft.world.entity.ai.attributes.AttributeInstanceProxy;
import net.momirealms.craftengine.proxy.minecraft.world.entity.ai.attributes.AttributesProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.BlockGetterProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.state.BlockBehaviourProxy;
import net.momirealms.craftengine.proxy.minecraft.world.phys.Vec3Proxy;
import org.bukkit.Bukkit;
import org.bukkit.Input;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Server-side carriers and owner-control runtime for native rig SEAT/SUB_SEAT parts.
 * All methods that mutate Bukkit/NMS entities are called from the owner entity scheduler.
 */
final class BukkitNativeRigMountInstance implements AutoCloseable {
    private static final float DEG_TO_RAD = (float) (Math.PI / 180.0);
    private static final float MIN_DIMENSION = 0.1f;

    private final CompiledRenderRuntime runtime;
    private final Entity owner;
    private final PlatformExecutor scheduler;
    private final IndexedRenderRig rig;
    private final int[] parts;
    private final Carrier[] carriers;
    private final AtomicBoolean closed = new AtomicBoolean();

    private WorldPosition anchor;
    private BukkitDisplayOwnerTransform ownerTransform;
    private boolean controlSnapshot;
    private boolean originalCollidable;
    private boolean originalGravity;
    private boolean originalAi;
    private boolean onFly;
    private int jumpDelay;

    BukkitNativeRigMountInstance(CompiledRenderRuntime runtime, Entity owner,
                                 WorldPosition anchor, BukkitDisplayOwnerTransform ownerTransform,
                                 PlatformExecutor scheduler) {
        this.runtime = Objects.requireNonNull(runtime, "runtime");
        this.owner = Objects.requireNonNull(owner, "owner");
        this.scheduler = Objects.requireNonNull(scheduler, "scheduler");
        this.rig = runtime.state().asset().rig();
        this.parts = rig.mountIndices();
        this.carriers = new Carrier[parts.length];
        this.anchor = Objects.requireNonNull(anchor, "anchor");
        this.ownerTransform = Objects.requireNonNull(ownerTransform, "ownerTransform");
    }

    boolean empty() { return parts.length == 0; }

    void update(WorldPosition anchor, BukkitDisplayOwnerTransform ownerTransform) {
        if (closed.get()) return;
        this.anchor = Objects.requireNonNull(anchor, "anchor");
        this.ownerTransform = Objects.requireNonNull(ownerTransform, "ownerTransform");
        if (jumpDelay > 0) jumpDelay--;

        org.bukkit.entity.Entity platformOwner = platformOwner();
        World world = platformOwner.getWorld();
        RenderMountBuffer mounts = runtime.mounts();
        RenderHitBoxBuffer hitBoxes = runtime.hitBoxes();
        for (int slot = 0; slot < parts.length; slot++) {
            int part = parts[slot];
            WorldCenter center = worldCenter(mounts, part, anchor, ownerTransform);
            float width = MIN_DIMENSION;
            float height = MIN_DIMENSION;
            if (rig.hasHitBox(part)) {
                float scale = ownerTransform.scale();
                width = Math.max(MIN_DIMENSION, Math.max(hitBoxes.width(part), hitBoxes.depth(part)) * scale);
                height = Math.max(MIN_DIMENSION, hitBoxes.height(part) * scale);
            }
            double y = center.y - height * 0.5d;
            Carrier carrier = carriers[slot];
            if (carrier == null || !carrier.entity.isValid() || !carrier.worldUuid.equals(world.getUID())) {
                destroyCarrier(slot, true);
                carrier = spawnCarrier(slot, world, center.x, y, center.z, width, height);
            } else {
                updateCarrier(carrier, center.x, y, center.z, width, height);
            }
            reconcilePassenger(carrier);
        }
        applyOwnerControl();
    }

    void dismountAll() {
        if (closed.get()) return;
        for (Carrier carrier : carriers) {
            if (carrier == null || !carrier.entity.isValid()) continue;
            for (org.bukkit.entity.Entity passenger : List.copyOf(carrier.entity.getPassengers())) {
                if (passenger instanceof Player rider) forceDismount(carrier, rider);
                else carrier.entity.removePassenger(passenger);
            }
        }
        restoreOwnerControlState();
    }

    boolean toggle(int partIndex, Player rider) {
        if (closed.get() || rider == null || !rider.isValid()) return false;
        int slot = slotOf(partIndex);
        if (slot < 0) return false;
        Carrier carrier = carriers[slot];
        if (carrier == null || !carrier.entity.isValid()) return false;
        RenderMountPolicy policy = rig.mountPolicy(partIndex);
        if (!policy.canMount()) return false;

        if (rider.getVehicle() == carrier.entity) {
            if (!policy.canDismountBySelf()) return false;
            forceDismount(carrier, rider);
            return true;
        }
        if (rider.getVehicle() != null || !carrier.entity.getPassengers().isEmpty()) return false;
        if (policy.canControl() && controllingRider() != null) return false;
        if (!carrier.entity.addPassenger(rider)) return false;
        if (!BukkitNativeRigMountRegistry.mounted(carrier.target, rider)) {
            carrier.entity.removePassenger(rider);
            return false;
        }
        return true;
    }

    void requestDismount(UUID riderUuid) {
        if (closed.get() || riderUuid == null) return;
        scheduler.run(() -> dismountNow(riderUuid), () -> {}, owner);
    }

    private void dismountNow(UUID riderUuid) {
        if (closed.get()) return;
        for (Carrier carrier : carriers) {
            if (carrier == null || !carrier.entity.isValid()) continue;
            for (org.bukkit.entity.Entity passenger : List.copyOf(carrier.entity.getPassengers())) {
                if (riderUuid.equals(passenger.getUniqueId()) && passenger instanceof Player rider) {
                    forceDismount(carrier, rider);
                    return;
                }
            }
        }
    }

    private void applyOwnerControl() {
        Control control = controllingRider();
        if (control == null) {
            restoreOwnerControlState();
            return;
        }
        org.bukkit.entity.Entity platformOwner = platformOwner();
        if (!(platformOwner instanceof LivingEntity living)) {
            restoreOwnerControlState();
            return;
        }
        snapshotOwnerControlState(living);
        living.setCollidable(false);
        if (platformOwner instanceof Mob mob) mob.getPathfinder().stopPathfinding();

        Player rider = control.rider;
        RenderMountPolicy policy = control.policy;
        if (!policy.canFly() && living.isGliding()) return;

        Input current = rider.getCurrentInput();
        RenderMountInput input = new RenderMountInput(
                axis(current.isLeft(), current.isRight()),
                current.isJump() ? 1f : current.isSneak() ? -1f : 0f,
                axis(current.isForward(), current.isBackward()),
                current.isJump()
        );

        Object nmsOwner = owner.minecraftEntity();
        boolean fly = (input.jump() && policy.canFly()) || originalFlightOverride(platformOwner) || onFly;
        if (platformOwner instanceof Mob) {
            living.setAI(fly ? false : originalAi);
        } else {
            living.setGravity(fly ? false : originalGravity);
        }
        onFly = fly && !EntityProxy.INSTANCE.onGround(nmsOwner);
        if (onFly) EntityProxy.INSTANCE.resetFallDistance(nmsOwner);

        float speed = movementSpeed(nmsOwner, onFly);
        RenderMountMotion motion = RenderMountController.resolve(policy, input, speed, rider.getYaw(), onFly);
        if (motion.lengthSquared() > 0.0001f) {
            EntityProxy.INSTANCE.setRot(nmsOwner, rider.getYaw(), EntityProxy.INSTANCE.getXRot(nmsOwner));
            if (onFly) LivingEntityProxy.INSTANCE.setYHeadRot(nmsOwner, rider.getYaw());
            EntityProxy.INSTANCE.move(nmsOwner, MoverTypeProxy.SELF,
                    Vec3Proxy.INSTANCE.newInstance(motion.x(), motion.y(), motion.z()));
        }

        Object delta = EntityProxy.INSTANCE.getDeltaMovement(nmsOwner);
        float vertical = (float) (Vec3Proxy.INSTANCE.getY(delta) + EntityProxy.INSTANCE.getGravity(nmsOwner));
        if (!onFly && RenderMountController.shouldJump(
                policy, input, EntityProxy.INSTANCE.isHorizontalCollision(nmsOwner), vertical, jumpDelay)) {
            jumpDelay = 10;
            LivingEntityProxy.INSTANCE.jumpFromGround(nmsOwner);
        }
    }

    private float movementSpeed(Object nmsOwner, boolean flying) {
        Object attribute = LivingEntityProxy.INSTANCE.getAttribute(nmsOwner, AttributesProxy.MOVEMENT_SPEED);
        float speed = attribute == null ? 0f : (float) AttributeInstanceProxy.INSTANCE.getValue(attribute);
        if (speed <= 0f || flying || LivingEntityProxy.INSTANCE.shouldDiscardFriction(nmsOwner)) return Math.max(0f, speed);
        Object blockPos = EntityProxy.INSTANCE.getBlockPosBelowThatAffectsMyMovement(nmsOwner);
        Object state = BlockGetterProxy.INSTANCE.getBlockState(EntityProxy.INSTANCE.getLevel(nmsOwner), blockPos);
        Object block = BlockBehaviourProxy.BlockStateBaseProxy.INSTANCE.getBlock(state);
        return Math.max(0f, speed * BlockBehaviourProxy.INSTANCE.getFriction(block));
    }

    private void snapshotOwnerControlState(LivingEntity living) {
        if (controlSnapshot) return;
        controlSnapshot = true;
        originalCollidable = living.isCollidable();
        originalGravity = living.hasGravity();
        originalAi = living.hasAI();
    }

    private boolean originalFlightOverride(org.bukkit.entity.Entity platformOwner) {
        if (!controlSnapshot) return false;
        return platformOwner instanceof Mob ? !originalAi : !originalGravity;
    }

    private void restoreOwnerControlState() {
        if (!controlSnapshot) {
            onFly = false;
            jumpDelay = 0;
            return;
        }
        org.bukkit.entity.Entity platformOwner = platformOwner();
        if (platformOwner instanceof LivingEntity living && living.isValid()) {
            living.setCollidable(originalCollidable);
            living.setGravity(originalGravity);
            if (living instanceof Mob) living.setAI(originalAi);
        }
        controlSnapshot = false;
        onFly = false;
        jumpDelay = 0;
    }

    private Control controllingRider() {
        for (Carrier carrier : carriers) {
            if (carrier == null || !carrier.entity.isValid() || !carrier.policy.canControl()) continue;
            for (org.bukkit.entity.Entity passenger : carrier.entity.getPassengers()) {
                if (passenger instanceof Player player && player.isValid()) {
                    return new Control(player, carrier.policy);
                }
            }
        }
        return null;
    }

    private Carrier spawnCarrier(int slot, World world, double x, double y, double z, float width, float height) {
        int part = parts[slot];
        Location location = new Location(world, x, y, z, anchor.yRot, anchor.xRot);
        Interaction entity = world.spawn(location, Interaction.class, interaction -> {
            interaction.setPersistent(false);
            interaction.setResponsive(false);
            interaction.setInteractionWidth(0f);
            interaction.setInteractionHeight(height);
        });
        Carrier carrier = new Carrier(entity, world.getUID(), part, rig.mountPolicy(part), width, height);
        carrier.target = BukkitNativeRigMountRegistry.register(entity, owner.uuid(), part, carrier.policy, this);
        carriers[slot] = carrier;
        return carrier;
    }

    private void updateCarrier(Carrier carrier, double x, double y, double z, float width, float height) {
        Object nms = CraftEntityProxy.INSTANCE.getEntity(carrier.entity);
        EntityProxy.INSTANCE.setPos(nms, x, y, z);
        EntityProxy.INSTANCE.setRot(nms, anchor.yRot, anchor.xRot);
        if (Float.floatToIntBits(carrier.height) != Float.floatToIntBits(height)) {
            carrier.entity.setInteractionHeight(height);
            carrier.height = height;
        }
        carrier.width = width;
    }

    private void forceDismount(Carrier carrier, Player rider) {
        BukkitNativeRigMountRegistry.Target target = carrier.target;
        target.forceDismount(true);
        try {
            carrier.entity.removePassenger(rider);
            BukkitNativeRigMountRegistry.dismounted(target, rider.getUniqueId());
        } finally {
            target.forceDismount(false);
        }
    }

    private void reconcilePassenger(Carrier carrier) {
        UUID actual = null;
        Player actualPlayer = null;
        for (org.bukkit.entity.Entity passenger : carrier.entity.getPassengers()) {
            if (passenger instanceof Player player) {
                actual = player.getUniqueId();
                actualPlayer = player;
                break;
            }
        }
        UUID registered = carrier.target.riderUuid();
        if (registered != null && !registered.equals(actual)) {
            BukkitNativeRigMountRegistry.dismounted(carrier.target, registered);
        }
        if (actual != null && !actual.equals(registered)
                && !BukkitNativeRigMountRegistry.mounted(carrier.target, actualPlayer)) {
            carrier.entity.removePassenger(actualPlayer);
        }
    }

    private void destroyCarrier(int slot, boolean removeEntity) {
        Carrier carrier = carriers[slot];
        if (carrier == null) return;
        carriers[slot] = null;
        BukkitNativeRigMountRegistry.unregister(carrier.target);
        if (!removeEntity || !carrier.entity.isValid()) return;
        if (Bukkit.isOwnedByCurrentRegion(carrier.entity)) {
            removeCarrierEntity(carrier);
            return;
        }
        carrier.entity.getScheduler().run(
                BukkitCraftEngine.instance().javaPlugin(),
                task -> removeCarrierEntity(carrier),
                null
        );
    }

    private static void removeCarrierEntity(Carrier carrier) {
        if (!carrier.entity.isValid()) return;
        for (org.bukkit.entity.Entity passenger : List.copyOf(carrier.entity.getPassengers())) {
            carrier.entity.removePassenger(passenger);
        }
        carrier.entity.remove();
    }

    private org.bukkit.entity.Entity platformOwner() {
        Object platform = owner.platformEntity();
        if (!(platform instanceof org.bukkit.entity.Entity entity)) {
            throw new IllegalStateException("native mount owner is not a Bukkit entity");
        }
        return entity;
    }

    private int slotOf(int partIndex) {
        for (int i = 0; i < parts.length; i++) if (parts[i] == partIndex) return i;
        return -1;
    }

    private static float axis(boolean positive, boolean negative) {
        return positive == negative ? 0f : positive ? 1f : -1f;
    }

    private static WorldCenter worldCenter(RenderMountBuffer mounts, int part, WorldPosition anchor,
                                           BukkitDisplayOwnerTransform ownerTransform) {
        float x = mounts.x(part) * ownerTransform.scale() + ownerTransform.offsetX();
        float y = mounts.y(part) * ownerTransform.scale() + ownerTransform.offsetY();
        float z = mounts.z(part) * ownerTransform.scale() + ownerTransform.offsetZ();

        float pitch = -anchor.xRot * DEG_TO_RAD;
        float cp = (float) Math.cos(pitch), sp = (float) Math.sin(pitch);
        float py = y * cp - z * sp;
        float pz = y * sp + z * cp;
        y = py; z = pz;

        float yaw = -anchor.yRot * DEG_TO_RAD;
        float cy = (float) Math.cos(yaw), sy = (float) Math.sin(yaw);
        float px = x * cy + z * sy;
        float pzz = -x * sy + z * cy;
        return new WorldCenter(anchor.x + px, anchor.y + y, anchor.z + pzz);
    }

    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) return;
        org.bukkit.entity.Entity platformOwner = platformOwner();
        if (Bukkit.isOwnedByCurrentRegion(platformOwner)) {
            restoreOwnerControlState();
        } else {
            scheduler.run(this::restoreOwnerControlState, this::retiredOwnerCleanup, owner);
        }
        for (int i = 0; i < carriers.length; i++) destroyCarrier(i, true);
    }

    private void retiredOwnerCleanup() {
        controlSnapshot = false;
        onFly = false;
        jumpDelay = 0;
    }

    private static final class Carrier {
        private final Interaction entity;
        private final UUID worldUuid;
        private final int partIndex;
        private final RenderMountPolicy policy;
        private float width;
        private float height;
        private BukkitNativeRigMountRegistry.Target target;

        private Carrier(Interaction entity, UUID worldUuid, int partIndex, RenderMountPolicy policy, float width, float height) {
            this.entity = entity;
            this.worldUuid = worldUuid;
            this.partIndex = partIndex;
            this.policy = policy;
            this.width = width;
            this.height = height;
        }
    }

    private record Control(Player rider, RenderMountPolicy policy) {}
    private record WorldCenter(double x, double y, double z) {}
}
