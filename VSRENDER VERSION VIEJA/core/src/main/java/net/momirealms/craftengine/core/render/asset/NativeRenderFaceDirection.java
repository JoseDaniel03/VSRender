package net.momirealms.craftengine.core.render.asset;

public enum NativeRenderFaceDirection {
    NORTH("north"),
    SOUTH("south"),
    EAST("east"),
    WEST("west"),
    UP("up"),
    DOWN("down");

    private final String key;

    NativeRenderFaceDirection(String key) {
        this.key = key;
    }

    public String key() {
        return key;
    }

    public static NativeRenderFaceDirection parse(String value) {
        for (NativeRenderFaceDirection direction : values()) {
            if (direction.key.equalsIgnoreCase(value)) return direction;
        }
        throw new IllegalArgumentException("unsupported face direction: " + value);
    }
}
