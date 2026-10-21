package net.momirealms.craftengine.core.render.asset;

import net.momirealms.craftengine.core.render.animation.RenderVec3;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/**
 * Provider-independent cube geometry stored in part-local Blockbench pixel coordinates.
 * The owning part pivot is (0,0,0); resource-pack compilation translates that pivot to model-space (8,8,8).
 */
public final class NativeRenderCube {
    private final String id;
    private final String partId;
    private final RenderVec3 from;
    private final RenderVec3 to;
    private final RenderVec3 origin;
    private final RenderVec3 rotation;
    private final boolean shade;
    private final boolean rescale;
    private final int lightEmission;
    private final Map<NativeRenderFaceDirection, NativeRenderFace> faces;

    public NativeRenderCube(String id,
                            String partId,
                            RenderVec3 from,
                            RenderVec3 to,
                            RenderVec3 origin,
                            RenderVec3 rotation,
                            boolean shade,
                            boolean rescale,
                            int lightEmission,
                            Map<NativeRenderFaceDirection, NativeRenderFace> faces) {
        this.id = requireId(id, "id");
        this.partId = requireId(partId, "partId");
        this.from = Objects.requireNonNull(from, "from");
        this.to = Objects.requireNonNull(to, "to");
        this.origin = Objects.requireNonNull(origin, "origin");
        this.rotation = Objects.requireNonNull(rotation, "rotation");
        if (to.x() < from.x() || to.y() < from.y() || to.z() < from.z()) {
            throw new IllegalArgumentException("cube to must be >= from on every axis");
        }
        if (lightEmission < 0 || lightEmission > 15) {
            throw new IllegalArgumentException("lightEmission must be between 0 and 15");
        }
        this.shade = shade;
        this.rescale = rescale;
        this.lightEmission = lightEmission;
        Objects.requireNonNull(faces, "faces");
        EnumMap<NativeRenderFaceDirection, NativeRenderFace> copy = new EnumMap<>(NativeRenderFaceDirection.class);
        faces.forEach((direction, face) -> copy.put(
                Objects.requireNonNull(direction, "face direction"),
                Objects.requireNonNull(face, "face")
        ));
        if (copy.isEmpty()) throw new IllegalArgumentException("cube must have at least one textured face");
        this.faces = Collections.unmodifiableMap(copy);
    }

    public String id() { return id; }
    public String partId() { return partId; }
    public RenderVec3 from() { return from; }
    public RenderVec3 to() { return to; }
    public RenderVec3 origin() { return origin; }
    public RenderVec3 rotation() { return rotation; }
    public boolean shade() { return shade; }
    public boolean rescale() { return rescale; }
    public int lightEmission() { return lightEmission; }
    public Map<NativeRenderFaceDirection, NativeRenderFace> faces() { return faces; }

    private static String requireId(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) throw new IllegalArgumentException(name + " must not be blank");
        return value;
    }
}
