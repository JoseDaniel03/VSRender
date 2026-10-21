package net.momirealms.craftengine.core.render.compiled;

import net.momirealms.craftengine.core.render.animation.RenderAnimationClip;
import net.momirealms.craftengine.core.render.animation.RenderAnimationMode;
import net.momirealms.craftengine.core.render.animation.RenderAnimationTrack;

import java.util.Objects;

/** Animation clip compiled against one indexed rig. */
public final class IndexedAnimationClip {
    private final IndexedRenderRig rig;
    private final String animationId;
    private final float lengthSeconds;
    private final RenderAnimationMode defaultMode;
    private final boolean override;
    private final IndexedAnimationTrack[] tracks;
    private final IndexedAnimationTrack[] trackByPart;

    private IndexedAnimationClip(IndexedRenderRig rig,
                                 String animationId,
                                 float lengthSeconds,
                                 RenderAnimationMode defaultMode,
                                 boolean override,
                                 IndexedAnimationTrack[] tracks,
                                 IndexedAnimationTrack[] trackByPart) {
        this.rig = rig;
        this.animationId = animationId;
        this.lengthSeconds = lengthSeconds;
        this.defaultMode = defaultMode;
        this.override = override;
        this.tracks = tracks;
        this.trackByPart = trackByPart;
    }

    public static IndexedAnimationClip compile(RenderAnimationClip clip, IndexedRenderRig rig) {
        Objects.requireNonNull(clip, "clip");
        Objects.requireNonNull(rig, "rig");
        IndexedAnimationTrack[] dense = new IndexedAnimationTrack[clip.tracks().size()];
        IndexedAnimationTrack[] byPart = new IndexedAnimationTrack[rig.partCount()];
        int count = 0;
        for (RenderAnimationTrack track : clip.tracks().values()) {
            int partIndex = rig.indexOf(track.partId());
            if (partIndex < 0) {
                throw new IllegalArgumentException("animation " + clip.animationId() + " references missing rig part " + track.partId());
            }
            IndexedAnimationTrack compiled = IndexedAnimationTrack.compile(partIndex, track);
            dense[count++] = compiled;
            byPart[partIndex] = compiled;
        }
        return new IndexedAnimationClip(rig, clip.animationId(), clip.lengthSeconds(), clip.defaultMode(), clip.override(), dense, byPart);
    }

    public String animationId() { return animationId; }
    IndexedRenderRig rig() { return rig; }
    public float lengthSeconds() { return lengthSeconds; }
    public RenderAnimationMode defaultMode() { return defaultMode; }
    public boolean override() { return override; }
    public int partCount() { return rig.partCount(); }
    public int trackCount() { return tracks.length; }

    IndexedAnimationTrack[] tracks() { return tracks; }
    IndexedAnimationTrack trackForPart(int partIndex) { return trackByPart[partIndex]; }
}
