package net.momirealms.craftengine.core.render.asset;

/** Semantic place where one native VsRender asset may be reused. */
public enum RenderUsageContext {
    ITEM(false),
    BLOCK(false),
    ENTITY(true),
    FURNITURE(false),
    PLAYER_ATTACHMENT(true),
    VEHICLE(true);

    private final boolean entityOwned;

    RenderUsageContext(boolean entityOwned) {
        this.entityOwned = entityOwned;
    }

    /** True when the render host follows a live owner entity instead of a fixed world position. */
    public boolean entityOwned() {
        return entityOwned;
    }
}
