package net.momirealms.craftengine.bukkit.render.runtime;

import net.momirealms.craftengine.bukkit.plugin.BukkitCraftEngine;
import net.momirealms.craftengine.core.entity.Entity;
import net.momirealms.craftengine.core.attribute.vanilla.VanillaAttributeInstance;
import net.momirealms.craftengine.core.attribute.vanilla.VanillaAttributes;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.plugin.scheduler.PlatformExecutor;
import net.momirealms.craftengine.core.plugin.scheduler.SchedulerTask;
import net.momirealms.craftengine.core.render.animation.RenderAnimationController;
import net.momirealms.craftengine.core.render.attachment.RenderAttachmentBinding;
import net.momirealms.craftengine.core.render.attachment.RenderPartTransform;
import net.momirealms.craftengine.core.render.compiled.CompiledRenderAnimationController;
import net.momirealms.craftengine.core.render.instance.RenderInstance;
import net.momirealms.craftengine.core.render.runtime.RenderSightVisibility;
import net.momirealms.craftengine.core.world.WorldPosition;
import net.momirealms.craftengine.proxy.minecraft.world.entity.EntityAttachmentProxy;
import net.momirealms.craftengine.proxy.minecraft.world.entity.EntityAttachmentsProxy;
import net.momirealms.craftengine.proxy.minecraft.world.entity.EntityProxy;
import net.momirealms.craftengine.proxy.minecraft.world.phys.Vec3Proxy;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffectType;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Entity-owned native render host. The repeating task follows the entity scheduler instead of a
 * fixed region, so Folia migrations/teleports cannot leave the renderer ticking on a stale chunk.
 */
public final class BukkitEntityNativeRenderInstance implements RenderInstance {
    private static final float TICK_SECONDS = 1f / 20f;
    private static final float HEAD_LIMIT_DEGREES = 75f;
    private static final float HEAD_SMOOTH_SECONDS = 0.15f;
    private static final float DEG_TO_RAD = (float) (Math.PI / 180.0);

    private final Entity owner;
    private final boolean includeOwnerViewer;
    private final boolean followOwnerInvisibility;
    private final boolean damageTintEnabled;
    private final boolean sightTrace;
    private final double minSight;
    private final double maxSight;
    private final PlatformExecutor scheduler;
    private final BukkitNativeDisplayInstance display;
    private final BukkitNativeRigHitBoxInstance hitBoxes;
    private final BukkitNativeRigNameTagInstance nameTags;
    private final BukkitNativeRigMountInstance mounts;
    private final CompiledRenderAnimationController animations;
    private final AtomicReference<SchedulerTask> tickTask = new AtomicReference<>();
    private final AtomicBoolean closed = new AtomicBoolean();
    private final AtomicBoolean registryRegistered = new AtomicBoolean();
    private final Object lifecycleLock = new Object();
    private final boolean followsOwnerHead;
    private final VanillaAttributeInstance scaleAttribute;
    private boolean headInitialized;
    private float smoothedHeadPitch;
    private float smoothedHeadYaw;
    private boolean terminalDeath;
    private boolean terminalDeathCompleted;

    public BukkitEntityNativeRenderInstance(BukkitNativeDisplayAsset asset, Entity owner, boolean includeOwnerViewer) {
        this(asset, owner, includeOwnerViewer, true, true, false, 5d, Double.POSITIVE_INFINITY, BukkitCraftEngine.instance().scheduler().platform());
    }

    public BukkitEntityNativeRenderInstance(BukkitNativeDisplayAsset asset, Entity owner, boolean includeOwnerViewer,
                                            boolean followOwnerInvisibility, boolean damageTintEnabled) {
        this(asset, owner, includeOwnerViewer, followOwnerInvisibility, damageTintEnabled, false, 5d, Double.POSITIVE_INFINITY,
                BukkitCraftEngine.instance().scheduler().platform());
    }

    /** Injection-friendly constructor for validation and alternate platform hosts. */
    public BukkitEntityNativeRenderInstance(BukkitNativeDisplayAsset asset, Entity owner,
                                            boolean includeOwnerViewer, PlatformExecutor scheduler) {
        this(asset, owner, includeOwnerViewer, true, true, false, 5d, Double.POSITIVE_INFINITY, scheduler);
    }

    /** Full injection-friendly constructor including imported owner-visual semantics. */
    public BukkitEntityNativeRenderInstance(BukkitNativeDisplayAsset asset, Entity owner,
                                            boolean includeOwnerViewer, boolean followOwnerInvisibility,
                                            boolean damageTintEnabled, PlatformExecutor scheduler) {
        this(asset, owner, includeOwnerViewer, followOwnerInvisibility, damageTintEnabled, false, 5d, Double.POSITIVE_INFINITY, scheduler);
    }

    public BukkitEntityNativeRenderInstance(BukkitNativeDisplayAsset asset, Entity owner,
                                            boolean includeOwnerViewer, boolean followOwnerInvisibility,
                                            boolean damageTintEnabled, boolean sightTrace,
                                            double minSight, double maxSight, PlatformExecutor scheduler) {
        Objects.requireNonNull(asset, "asset");
        this.owner = Objects.requireNonNull(owner, "owner");
        this.includeOwnerViewer = includeOwnerViewer;
        this.followOwnerInvisibility = followOwnerInvisibility;
        this.damageTintEnabled = damageTintEnabled;
        this.sightTrace = sightTrace;
        this.minSight = minSight;
        this.maxSight = maxSight;
        this.scheduler = Objects.requireNonNull(scheduler, "scheduler");
        if (!(minSight >= 0d) || Double.isNaN(minSight)) throw new IllegalArgumentException("minSight");
        if (!(maxSight >= minSight) || Double.isNaN(maxSight)) throw new IllegalArgumentException("maxSight");
        this.scaleAttribute = owner instanceof net.momirealms.craftengine.core.entity.LivingEntity living
                ? living.getVanillaAttribute(VanillaAttributes.SCALE) : null;
        WorldPosition initialAnchor = currentAnchor();
        BukkitDisplayOwnerTransform initialOwnerTransform = currentOwnerTransform();
        this.display = new BukkitNativeDisplayInstance(asset, initialAnchor);
        this.display.updateOwnerTransform(initialOwnerTransform);
        this.display.updateEntityVisualState(currentEntityVisualState());
        this.display.refreshBoundPayloads(owner);
        this.followsOwnerHead = asset.asset().rig().hasOwnerHeadRotation();
        this.animations = asset.asset().animations().isEmpty()
                ? null
                : new CompiledRenderAnimationController(asset.rigId(), display.runtime());
        this.mounts = display.runtime().mounts().mountCount() == 0
                ? null
                : new BukkitNativeRigMountInstance(display.runtime(), owner, initialAnchor, initialOwnerTransform, scheduler);
        this.hitBoxes = display.runtime().hitBoxes().hitBoxCount() == 0
                ? null
                : new BukkitNativeRigHitBoxInstance(display.runtime(), owner, initialAnchor, initialOwnerTransform, this::requestMount);
        this.nameTags = asset.asset().rig().nameTagIndices().length == 0
                ? null
                : new BukkitNativeRigNameTagInstance(display.runtime(), owner, initialAnchor, initialOwnerTransform);

        SchedulerTask scheduled = null;
        BukkitNativeEntityRenderRegistry.register(owner.uuid(), this);
        registryRegistered.set(true);
        try {
            scheduled = Objects.requireNonNull(
                    scheduler.runRepeating(this::tickOwnerEntity, this::retired, 1L, 1L, owner),
                    "scheduler returned null task"
            );
            tickTask.set(scheduled);
            if (closed.get() && tickTask.compareAndSet(scheduled, null)) scheduled.cancel();
        } catch (RuntimeException | Error failure) {
            cleanup(false, failure);
            throw failure;
        }
    }

    private BukkitDisplayOwnerTransform currentOwnerTransform() {
        float scale = 1f;
        if (scaleAttribute != null) {
            double value = scaleAttribute.getValue();
            if (Double.isFinite(value) && value > 0d) scale = (float) value;
        }

        float offsetX = 0f, offsetY = 0f, offsetZ = 0f;
        Object nmsEntity = owner.minecraftEntity();
        Object attachments = EntityProxy.INSTANCE.getAttachments(nmsEntity);
        if (attachments != null) {
            Object point = EntityAttachmentsProxy.INSTANCE.get(
                    attachments, EntityAttachmentProxy.PASSENGER, 0, owner.yRot());
            if (point != null) {
                offsetX = -(float) Vec3Proxy.INSTANCE.getX(point);
                offsetY = -(float) Vec3Proxy.INSTANCE.getY(point);
                offsetZ = -(float) Vec3Proxy.INSTANCE.getZ(point);
            }
        }
        return new BukkitDisplayOwnerTransform(scale, offsetX, offsetY, offsetZ);
    }

    private BukkitDisplayEntityVisualState currentEntityVisualState() {
        Object platform = owner.platformEntity();
        if (!(platform instanceof LivingEntity living)) return BukkitDisplayEntityVisualState.DEFAULT;
        boolean invisible = followOwnerInvisibility
                && (living.isInvisible() || living.hasPotionEffect(PotionEffectType.INVISIBILITY));
        return new BukkitDisplayEntityVisualState(invisible, living.isGlowing());
    }

    private WorldPosition currentAnchor() {
        float bodyYaw = owner.yRot();
        Object platform = owner.platformEntity();
        if (platform instanceof LivingEntity living) bodyYaw = living.getBodyYaw();
        return new WorldPosition(owner.world(), owner.x(), owner.y(), owner.z(), owner.xRot(), bodyYaw);
    }

    private void tickOwnerEntity() {
        synchronized (lifecycleLock) {
            if (closed.get()) return;
            if (!owner.isValid()) {
                // This is an owner-state observation from our still-running task, not the
                // EntityScheduler retirement callback. Cancel our repeating handle explicitly.
                close();
                return;
            }
            if (!terminalDeath && !owner.isAlive()) {
                close();
                return;
            }

            BukkitDisplayOwnerTransform ownerTransform = currentOwnerTransform();
            display.updateOwnerTransform(ownerTransform);
            display.updateEntityVisualState(currentEntityVisualState());
            display.refreshBoundPayloads(owner);
            display.tickDamageTint();
            WorldPosition anchor = currentAnchor();
            display.updateAnchor(anchor);
            updateOwnerHeadRotation(anchor.yRot);

            // Gameplay geometry owns the exact 20 TPS animation sample. Visual LOD then publishes the
            // already-current shared pose with delta 0, so there is never a second animation clock.
            if (hitBoxes != null || nameTags != null || mounts != null) {
                display.runtime().advanceForGameplay(TICK_SECONDS);
                if (hitBoxes != null) hitBoxes.update(anchor, ownerTransform);
                if (nameTags != null) nameTags.update(anchor, ownerTransform);
                if (mounts != null) mounts.update(anchor, ownerTransform);
            }

            LinkedHashSet<Player> desired = new LinkedHashSet<>(owner.getTrackedBy());
            if (includeOwnerViewer && owner instanceof Player player) desired.add(player);
            if (sightTrace) desired.removeIf(viewer -> !canSee(viewer, anchor));
            display.syncViewers(desired);
            if (hitBoxes != null) hitBoxes.syncViewers(desired);
            if (nameTags != null) nameTags.syncViewers(desired);

            if (display.runtime().needsTick()) {
                display.tick((hitBoxes == null && nameTags == null && mounts == null) ? TICK_SECONDS : 0f, display.nearestViewerDistance(), terminalDeath);
            }
            if (animations != null) animations.afterTick();
            if (terminalDeathCompleted) close();
        }
    }

    private boolean canSee(Player viewer, WorldPosition target) {
        return RenderSightVisibility.canSee(
                Objects.equals(viewer.world(), target.world),
                viewer.x(), viewer.y(), viewer.z(), viewer.yRot(), viewer.xRot(),
                target.x, target.y, target.z, minSight, maxSight);
    }

    private void updateOwnerHeadRotation(float bodyYaw) {
        if (!followsOwnerHead) return;
        float targetPitch = clamp(owner.xRot(), -HEAD_LIMIT_DEGREES, HEAD_LIMIT_DEGREES);
        float targetYaw = clamp(wrapDegrees(bodyYaw - owner.yRot()), -HEAD_LIMIT_DEGREES, HEAD_LIMIT_DEGREES);
        if (!headInitialized) {
            headInitialized = true;
            smoothedHeadPitch = targetPitch;
            smoothedHeadYaw = targetYaw;
        } else {
            float alpha = Math.min(1f, TICK_SECONDS / HEAD_SMOOTH_SECONDS);
            smoothedHeadPitch += (targetPitch - smoothedHeadPitch) * alpha;
            smoothedHeadYaw += (targetYaw - smoothedHeadYaw) * alpha;
        }

        float hx = smoothedHeadPitch * DEG_TO_RAD * 0.5f;
        float hy = smoothedHeadYaw * DEG_TO_RAD * 0.5f;
        float sinX = (float) Math.sin(hx), cosX = (float) Math.cos(hx);
        float sinY = (float) Math.sin(hy), cosY = (float) Math.cos(hy);
        display.runtime().ownerHeadRotation(
                cosY * sinX,
                sinY * cosX,
                -sinY * sinX,
                cosY * cosX
        );
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private static float wrapDegrees(float value) {
        float wrapped = value % 360f;
        if (wrapped >= 180f) wrapped -= 360f;
        if (wrapped < -180f) wrapped += 360f;
        return wrapped;
    }

    void onOwnerDamage() {
        synchronized (lifecycleLock) {
            if (closed.get() || terminalDeath) return;
            if (animations != null) animations.triggerFullRigEventOnce("damage");
            if (damageTintEnabled) display.triggerDamageTint(0xFF8080);
        }
    }

    void onOwnerJump() {
        synchronized (lifecycleLock) {
            if (closed.get() || terminalDeath) return;
            if (animations != null) animations.triggerFullRigEventOnce("jump");
        }
    }

    void onOwnerDeath() {
        synchronized (lifecycleLock) {
            if (closed.get() || terminalDeath) return;
            if (mounts != null) mounts.dismountAll();
            if (animations != null && animations.triggerTerminalFullRigEventOnce(
                    "death", () -> terminalDeathCompleted = true)) {
                terminalDeath = true;
            }
        }
    }

    private void requestMount(int partIndex, org.bukkit.entity.Player rider) {
        if (mounts == null || rider == null || closed.get()) return;
        scheduler.run(() -> {
            synchronized (lifecycleLock) {
                if (!closed.get()) mounts.toggle(partIndex, rider);
            }
        }, () -> {}, owner);
    }

    private void retired() {
        if (!closed.compareAndSet(false, true)) return;
        // The platform has already retired this task; release our handle without cancelling it again.
        tickTask.set(null);
        synchronized (lifecycleLock) {
            cleanup(false, null);
        }
    }

    @Override
    public void show(Player viewer) {
        synchronized (lifecycleLock) {
            if (!closed.get()) {
                display.show(viewer);
                if (hitBoxes != null) hitBoxes.show(viewer);
                if (nameTags != null) nameTags.show(viewer);
            }
        }
    }

    @Override
    public void hide(Player viewer) {
        synchronized (lifecycleLock) {
            if (!closed.get()) {
                display.hide(viewer);
                if (hitBoxes != null) hitBoxes.hide(viewer);
                if (nameTags != null) nameTags.hide(viewer);
            }
        }
    }

    /** Refreshes only platform visual payloads while preserving animation state and display IDs when possible. */
    public boolean refreshPayloads(BukkitDisplayPartPayload[] payloads) {
        synchronized (lifecycleLock) {
            return !closed.get() && display.refreshPayloads(payloads);
        }
    }

    @Override
    public Optional<RenderAnimationController> animations() {
        return Optional.ofNullable(animations);
    }

    @Override
    public Optional<RenderAttachmentBinding> attachment(String partId) {
        Objects.requireNonNull(partId, "partId");
        int partIndex = display.asset().asset().rig().indexOf(partId);
        if (partIndex < 0 || !isAttachmentIndex(partIndex)) return Optional.empty();
        return Optional.of(new BukkitNativeAttachmentBinding(partId, () -> attachmentSnapshot(partIndex)));
    }

    private boolean isAttachmentIndex(int partIndex) {
        for (int index : display.asset().asset().rig().attachmentIndices()) if (index == partIndex) return true;
        return false;
    }

    private Optional<RenderPartTransform> attachmentSnapshot(int partIndex) {
        synchronized (lifecycleLock) {
            if (closed.get()) return Optional.empty();
            return Optional.of(BukkitNativeAttachmentSnapshots.local(display.runtime().poses(), partIndex));
        }
    }

    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) return;
        RuntimeException failure = null;
        SchedulerTask task = tickTask.getAndSet(null);
        if (task != null) {
            try {
                task.cancel();
            } catch (RuntimeException error) {
                failure = error;
            }
        }
        synchronized (lifecycleLock) {
            failure = cleanup(false, failure);
        }
        if (failure != null) throw failure;
    }

    private RuntimeException cleanup(boolean cancelTask, Throwable primary) {
        RuntimeException failure = primary instanceof RuntimeException r ? r : null;
        if (registryRegistered.compareAndSet(true, false)) {
            BukkitNativeEntityRenderRegistry.unregister(owner.uuid(), this);
        }
        if (cancelTask) {
            SchedulerTask task = tickTask.getAndSet(null);
            if (task != null) {
                try { task.cancel(); }
                catch (RuntimeException error) {
                    if (failure == null) failure = error; else failure.addSuppressed(error);
                }
            }
        }
        if (hitBoxes != null) {
            try { hitBoxes.close(); }
            catch (RuntimeException error) {
                if (failure == null) failure = error; else failure.addSuppressed(error);
            }
        }
        if (mounts != null) {
            try { mounts.close(); }
            catch (RuntimeException error) {
                if (failure == null) failure = error; else failure.addSuppressed(error);
            }
        }
        if (nameTags != null) {
            try { nameTags.close(); }
            catch (RuntimeException error) {
                if (failure == null) failure = error; else failure.addSuppressed(error);
            }
        }
        if (animations != null) {
            try { animations.close(); }
            catch (RuntimeException error) {
                if (failure == null) failure = error; else failure.addSuppressed(error);
            }
        }
        try { display.close(); }
        catch (RuntimeException error) {
            if (failure == null) failure = error; else failure.addSuppressed(error);
        }
        return failure;
    }

    public boolean closed() { return closed.get(); }
    boolean terminalDeath() { synchronized (lifecycleLock) { return terminalDeath; } }
    public Entity owner() { return owner; }
    public BukkitNativeDisplayInstance display() { return display; }
}
