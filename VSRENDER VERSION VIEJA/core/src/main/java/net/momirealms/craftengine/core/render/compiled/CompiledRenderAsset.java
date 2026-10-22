package net.momirealms.craftengine.core.render.compiled;

import net.momirealms.craftengine.core.render.animation.RenderAnimationClip;
import net.momirealms.craftengine.core.render.rig.RenderRig;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Immutable compiled asset shared by many runtime render instances. */
public final class CompiledRenderAsset {
    private final String assetId;
    private final IndexedRenderRig rig;
    private final Map<String, IndexedAnimationClip> animations;

    private CompiledRenderAsset(String assetId, IndexedRenderRig rig, Map<String, IndexedAnimationClip> animations) {
        this.assetId = assetId;
        this.rig = rig;
        this.animations = animations;
    }

    public static CompiledRenderAsset compile(String assetId, RenderRig rig, Collection<RenderAnimationClip> clips) {
        Objects.requireNonNull(assetId, "assetId");
        if (assetId.isBlank()) throw new IllegalArgumentException("assetId must not be blank");
        Objects.requireNonNull(clips, "clips");
        IndexedRenderRig indexedRig = IndexedRenderRig.compile(Objects.requireNonNull(rig, "rig"));
        LinkedHashMap<String, IndexedAnimationClip> indexedClips = new LinkedHashMap<>();
        for (RenderAnimationClip clip : clips) {
            Objects.requireNonNull(clip, "clip");
            IndexedAnimationClip compiled = IndexedAnimationClip.compile(clip, indexedRig);
            if (indexedClips.putIfAbsent(compiled.animationId(), compiled) != null) {
                throw new IllegalArgumentException("duplicate animation: " + compiled.animationId());
            }
        }
        return new CompiledRenderAsset(assetId, indexedRig, Collections.unmodifiableMap(indexedClips));
    }

    public String assetId() { return assetId; }
    public IndexedRenderRig rig() { return rig; }
    public Collection<IndexedAnimationClip> animations() { return animations.values(); }
    public Optional<IndexedAnimationClip> animation(String animationId) { return Optional.ofNullable(animations.get(animationId)); }
    IndexedAnimationClip animationOrNull(String animationId) { return animations.get(animationId); }
    public RenderAnimationBuffer createAnimationBuffer() { return new RenderAnimationBuffer(rig); }
    public RenderPoseBuffer createPoseBuffer() { return new RenderPoseBuffer(rig); }
    public RenderHitBoxBuffer createHitBoxBuffer() { return new RenderHitBoxBuffer(rig); }
    public RenderMountBuffer createMountBuffer() { return new RenderMountBuffer(rig); }
}
