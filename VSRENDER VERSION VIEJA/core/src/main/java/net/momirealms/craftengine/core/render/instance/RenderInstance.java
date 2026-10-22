package net.momirealms.craftengine.core.render.instance;

import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.render.animation.RenderAnimationController;
import net.momirealms.craftengine.core.render.attachment.RenderAttachmentBinding;

import java.util.Optional;

/**
 * Viewer-facing lifecycle for a concrete rendered representation.
 * Provider-specific tracker/entity implementations stay behind this contract.
 */
public interface RenderInstance extends AutoCloseable {

    void show(Player viewer);

    void hide(Player viewer);

    default void refresh(Player viewer) {
        hide(viewer);
        show(viewer);
    }

    /** Optional named attachment capability for rigged instances. */
    default Optional<RenderAttachmentBinding> attachment(String partId) {
        return Optional.empty();
    }

    /** Optional animation capability for this concrete instance. */
    default Optional<RenderAnimationController> animations() {
        return Optional.empty();
    }

    @Override
    void close();
}
