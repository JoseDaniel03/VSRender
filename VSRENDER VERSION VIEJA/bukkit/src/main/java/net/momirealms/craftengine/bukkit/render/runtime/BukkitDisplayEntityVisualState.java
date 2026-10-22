package net.momirealms.craftengine.bukkit.render.runtime;

/** Immutable owner visual state mirrored onto packet-only Item Displays. */
public record BukkitDisplayEntityVisualState(boolean invisible, boolean glowing) {
    public static final BukkitDisplayEntityVisualState DEFAULT = new BukkitDisplayEntityVisualState(false, false);
}
