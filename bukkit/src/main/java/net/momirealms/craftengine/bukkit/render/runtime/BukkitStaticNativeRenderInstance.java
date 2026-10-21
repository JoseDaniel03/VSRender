package net.momirealms.craftengine.bukkit.render.runtime;

import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.render.instance.RenderInstance;
import net.momirealms.craftengine.core.world.WorldPosition;

import java.util.Objects;

/** RenderInstance bridge for a fixed native Item Display model that requires no runtime animation tick. */
public final class BukkitStaticNativeRenderInstance implements RenderInstance {
    private final BukkitNativeDisplayInstance display;

    public BukkitStaticNativeRenderInstance(BukkitNativeDisplayAsset asset, WorldPosition position) {
        Objects.requireNonNull(position, "position");
        this.display = new BukkitNativeDisplayInstance(
                Objects.requireNonNull(asset, "asset"), position
        );
    }

    @Override
    public void show(Player viewer) {
        display.show(viewer);
    }

    @Override
    public void hide(Player viewer) {
        display.hide(viewer);
    }

    @Override
    public void close() {
        display.close();
    }

    public BukkitNativeDisplayInstance display() {
        return display;
    }
}
