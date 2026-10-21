package net.momirealms.craftengine.bukkit.render.runtime.v26_2;

import net.momirealms.craftengine.bukkit.render.runtime.BukkitNativeRigPhysicalBodyBridge;

import java.util.concurrent.atomic.AtomicBoolean;

/** Installs the Canvas/Paper 26.2 native physical-hitbox backend exactly once. */
public final class VsRenderNative26Bootstrap {
    private static final AtomicBoolean INSTALLED = new AtomicBoolean();

    private VsRenderNative26Bootstrap() {
    }

    public static void install() {
        if (!INSTALLED.compareAndSet(false, true)) return;
        try {
            BukkitNativeRigPhysicalBodyBridge.install(Native26RigPhysicalBody::new);
        } catch (RuntimeException | Error failure) {
            INSTALLED.set(false);
            throw failure;
        }
    }
}