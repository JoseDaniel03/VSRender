package net.momirealms.craftengine.bukkit.render.runtime;

/** Platform-side source of a display payload after the model has been imported. */
public enum BukkitDisplayPayloadBinding {
    STATIC,
    MAIN_HAND,
    OFF_HAND;

    public boolean dynamic() {
        return this != STATIC;
    }
}
