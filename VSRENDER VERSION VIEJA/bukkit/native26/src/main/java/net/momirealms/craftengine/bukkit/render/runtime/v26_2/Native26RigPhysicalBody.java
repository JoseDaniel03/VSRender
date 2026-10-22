package net.momirealms.craftengine.bukkit.render.runtime.v26_2;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.momirealms.craftengine.bukkit.render.runtime.BukkitNativeRigPhysicalBody;
import net.momirealms.craftengine.core.world.collision.AABB;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/** One server-side physical collision entity per compiled rig hitbox. */
final class Native26RigPhysicalBody implements BukkitNativeRigPhysicalBody {
    private final Entity owner;
    private final UUID ownerUuid;
    private final Native26RigCollisionEntity[] bodies;
    private final AtomicBoolean closed = new AtomicBoolean();

    Native26RigPhysicalBody(Object minecraftOwner, UUID ownerUuid, AABB[] initialBounds) {
        if (!(minecraftOwner instanceof Entity entity)) {
            throw new IllegalArgumentException("minecraftOwner is not a 26.2 NMS Entity: " + minecraftOwner);
        }
        this.owner = entity;
        this.ownerUuid = Objects.requireNonNull(ownerUuid, "ownerUuid");
        Objects.requireNonNull(initialBounds, "initialBounds");
        if (!(owner.level() instanceof ServerLevel level)) {
            throw new IllegalStateException("VsRender physical hitboxes require a ServerLevel owner");
        }
        this.bodies = new Native26RigCollisionEntity[initialBounds.length];
        try {
            for (int i = 0; i < initialBounds.length; i++) {
                AABB bounds = Objects.requireNonNull(initialBounds[i], "initialBounds[" + i + "]");
                Native26RigCollisionEntity body = new Native26RigCollisionEntity(level, owner, ownerUuid, toMinecraft(bounds));
                bodies[i] = body;
                body.spawn();
            }
        } catch (RuntimeException | Error failure) {
            close();
            throw failure;
        }
    }

    @Override
    public void sync(AABB[] bounds) {
        Objects.requireNonNull(bounds, "bounds");
        if (closed.get()) return;
        if (bounds.length != bodies.length) {
            throw new IllegalArgumentException("physical hitbox count changed: expected " + bodies.length + ", got " + bounds.length);
        }
        if (owner.isRemoved()) {
            close();
            return;
        }
        if (!(owner.level() instanceof ServerLevel currentLevel)) {
            close();
            return;
        }
        for (int i = 0; i < bodies.length; i++) {
            Native26RigCollisionEntity body = bodies[i];
            if (body == null) continue;
            if (body.level() != currentLevel) {
                throw new IllegalStateException("physical hitbox world changed without backend recreation");
            }
            body.sync(toMinecraft(Objects.requireNonNull(bounds[i], "bounds[" + i + "]")));
        }
    }

    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) return;
        RuntimeException failure = null;
        for (Native26RigCollisionEntity body : bodies) {
            if (body == null) continue;
            try {
                body.destroy();
            } catch (RuntimeException error) {
                if (failure == null) failure = error;
                else failure.addSuppressed(error);
            }
        }
        if (failure != null) throw failure;
    }

    private static net.minecraft.world.phys.AABB toMinecraft(AABB box) {
        return new net.minecraft.world.phys.AABB(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ);
    }
}