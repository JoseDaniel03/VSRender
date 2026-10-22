package net.momirealms.craftengine.core.render.asset;

import net.momirealms.craftengine.core.render.animation.RenderAnimationClip;
import net.momirealms.craftengine.core.render.animation.RenderAnimationTrack;
import net.momirealms.craftengine.core.render.compiled.CompiledRenderAsset;
import net.momirealms.craftengine.core.render.rig.RenderRig;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Canonical provider-independent VsRender asset produced by native importers. */
public final class NativeRenderAsset {
    public static final int SCHEMA_VERSION = 1;
    public static final String PROVIDER_ID = "vsrender_native";

    private final String assetId;
    private final String sourceSha256;
    private final int textureWidth;
    private final int textureHeight;
    private final String javaBlockVersion;
    private final RenderRig rig;
    private final List<RenderAnimationClip> animations;
    private final Map<String, List<NativeRenderCube>> geometry;
    private final Map<String, NativeRenderTexture> textures;
    private final NativeRenderRepresentation representation;

    public NativeRenderAsset(String assetId,
                             String sourceSha256,
                             int textureWidth,
                             int textureHeight,
                             RenderRig rig,
                             Collection<RenderAnimationClip> animations,
                             Map<String, ? extends Collection<NativeRenderCube>> geometry,
                             Map<String, NativeRenderTexture> textures,
                             NativeRenderRepresentation representation) {
        this(assetId, sourceSha256, textureWidth, textureHeight, "", rig, animations, geometry, textures, representation);
    }

    public NativeRenderAsset(String assetId,
                             String sourceSha256,
                             int textureWidth,
                             int textureHeight,
                             String javaBlockVersion,
                             RenderRig rig,
                             Collection<RenderAnimationClip> animations,
                             Map<String, ? extends Collection<NativeRenderCube>> geometry,
                             Map<String, NativeRenderTexture> textures,
                             NativeRenderRepresentation representation) {
        this.assetId = requireId(assetId, "assetId");
        this.sourceSha256 = requireSha256(sourceSha256);
        if (textureWidth <= 0 || textureHeight <= 0) {
            throw new IllegalArgumentException("texture resolution must be positive");
        }
        this.textureWidth = textureWidth;
        this.textureHeight = textureHeight;
        this.javaBlockVersion = Objects.requireNonNull(javaBlockVersion, "javaBlockVersion").trim();
        this.rig = Objects.requireNonNull(rig, "rig");
        if (!PROVIDER_ID.equals(rig.providerId())) {
            throw new IllegalArgumentException("native rig provider must be " + PROVIDER_ID);
        }
        Objects.requireNonNull(animations, "animations");
        this.animations = List.copyOf(animations);
        validateAnimations(this.animations, rig);

        Objects.requireNonNull(geometry, "geometry");
        LinkedHashMap<String, List<NativeRenderCube>> geometryCopy = new LinkedHashMap<>();
        for (Map.Entry<String, ? extends Collection<NativeRenderCube>> entry : geometry.entrySet()) {
            String partId = requireId(entry.getKey(), "geometry partId");
            if (rig.part(partId).isEmpty()) throw new IllegalArgumentException("geometry references unknown part: " + partId);
            ArrayList<NativeRenderCube> cubes = new ArrayList<>();
            for (NativeRenderCube cube : Objects.requireNonNull(entry.getValue(), "cubes")) {
                NativeRenderCube value = Objects.requireNonNull(cube, "cube");
                if (!partId.equals(value.partId())) {
                    throw new IllegalArgumentException("cube part mismatch: " + value.id());
                }
                cubes.add(value);
            }
            geometryCopy.put(partId, List.copyOf(cubes));
        }
        this.geometry = Collections.unmodifiableMap(geometryCopy);

        Objects.requireNonNull(textures, "textures");
        LinkedHashMap<String, NativeRenderTexture> textureCopy = new LinkedHashMap<>();
        for (Map.Entry<String, NativeRenderTexture> entry : textures.entrySet()) {
            String textureId = requireId(entry.getKey(), "texture id");
            NativeRenderTexture texture = Objects.requireNonNull(entry.getValue(), "texture");
            if (!textureId.equals(texture.id())) throw new IllegalArgumentException("texture key/id mismatch: " + textureId);
            if (textureCopy.putIfAbsent(textureId, texture) != null) {
                throw new IllegalArgumentException("duplicate texture: " + textureId);
            }
        }
        validateFaceTextures(this.geometry, textureCopy);
        this.textures = Collections.unmodifiableMap(textureCopy);
        this.representation = Objects.requireNonNull(representation, "representation");
    }

    private static void validateAnimations(List<RenderAnimationClip> clips, RenderRig rig) {
        LinkedHashMap<String, Boolean> names = new LinkedHashMap<>();
        for (RenderAnimationClip clip : clips) {
            Objects.requireNonNull(clip, "clip");
            if (names.putIfAbsent(clip.animationId(), Boolean.TRUE) != null) {
                throw new IllegalArgumentException("duplicate animation: " + clip.animationId());
            }
            if (!rig.hasAnimation(clip.animationId())) {
                throw new IllegalArgumentException("rig animation catalog missing clip: " + clip.animationId());
            }
            for (RenderAnimationTrack track : clip.tracks().values()) {
                if (rig.part(track.partId()).isEmpty()) {
                    throw new IllegalArgumentException("animation references unknown part: " + track.partId());
                }
            }
        }
        if (names.size() != rig.animations().size() || !names.keySet().containsAll(rig.animations())) {
            throw new IllegalArgumentException("rig animation catalog and clips differ");
        }
    }

    private static void validateFaceTextures(Map<String, List<NativeRenderCube>> geometry,
                                             Map<String, NativeRenderTexture> textures) {
        for (List<NativeRenderCube> cubes : geometry.values()) {
            for (NativeRenderCube cube : cubes) {
                for (NativeRenderFace face : cube.faces().values()) {
                    if (!textures.containsKey(face.textureId())) {
                        throw new IllegalArgumentException("cube " + cube.id() + " references unknown texture " + face.textureId());
                    }
                }
            }
        }
    }

    public String assetId() { return assetId; }
    public String sourceSha256() { return sourceSha256; }
    public int textureWidth() { return textureWidth; }
    public int textureHeight() { return textureHeight; }
    public String javaBlockVersion() { return javaBlockVersion; }
    public RenderRig rig() { return rig; }
    public List<RenderAnimationClip> animations() { return animations; }
    public Map<String, List<NativeRenderCube>> geometry() { return geometry; }
    public List<NativeRenderCube> geometry(String partId) { return geometry.getOrDefault(partId, List.of()); }
    public Map<String, NativeRenderTexture> textures() { return textures; }
    public NativeRenderRepresentation representation() { return representation; }

    public CompiledRenderAsset compile() {
        return CompiledRenderAsset.compile(assetId, rig, animations);
    }

    private static String requireId(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) throw new IllegalArgumentException(name + " must not be blank");
        return value;
    }

    private static String requireSha256(String value) {
        Objects.requireNonNull(value, "sourceSha256");
        if (!value.matches("[0-9a-fA-F]{64}")) {
            throw new IllegalArgumentException("sourceSha256 must contain 64 hexadecimal characters");
        }
        return value.toLowerCase(java.util.Locale.ROOT);
    }
}
