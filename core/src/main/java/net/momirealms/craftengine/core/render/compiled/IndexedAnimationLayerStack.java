package net.momirealms.craftengine.core.render.compiled;

import net.momirealms.craftengine.core.render.animation.RenderAnimationMode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Objects;

/**
 * Indexed per-part animation layer stack.
 *
 * <p>Mutation (add/remove/retrigger) may allocate and sort; {@link #tick(float, RenderAnimationBuffer)}
 * performs no per-part allocation and no string/map lookup. Layers are ordered by priority descending,
 * then insertion order descending, mirroring BetterModel's PriorityMap ordering.</p>
 */
public final class IndexedAnimationLayerStack {
    private static final Comparator<Layer> ORDER = Comparator
            .comparingInt((Layer layer) -> layer.priority).reversed()
            .thenComparing(Comparator.comparingLong((Layer layer) -> layer.insertion).reversed());

    private final IndexedRenderRig rig;
    private final ArrayList<Layer> layers = new ArrayList<>();
    private final Layer[] selectedByPart;
    private long insertionCounter;

    public IndexedAnimationLayerStack(IndexedRenderRig rig) {
        this.rig = Objects.requireNonNull(rig, "rig");
        this.selectedByPart = new Layer[rig.partCount()];
    }

    /** Adds or retriggers a layer. Reusing an ID creates a fresh, newest insertion. */
    public boolean put(String layerId, IndexedAnimationClip clip, RenderAnimationMode mode, int priority) {
        Objects.requireNonNull(layerId, "layerId");
        if (layerId.isBlank()) throw new IllegalArgumentException("layerId must not be blank");
        Objects.requireNonNull(clip, "clip");
        Objects.requireNonNull(mode, "mode");
        if (clip.rig() != rig) throw new IllegalArgumentException("clip/stack rig mismatch");
        remove(layerId);
        Layer layer = new Layer(layerId, clip, mode, priority, insertionCounter++);
        if (!layer.affectsAny()) return false;
        layers.add(layer);
        layers.sort(ORDER);
        return true;
    }

    public boolean remove(String layerId) {
        Objects.requireNonNull(layerId, "layerId");
        for (int i = 0; i < layers.size(); i++) {
            Layer layer = layers.get(i);
            if (!layer.id.equals(layerId)) continue;
            layers.remove(i);
            return true;
        }
        return false;
    }

    public boolean contains(String layerId) {
        return find(layerId) != null;
    }

    public boolean completed(String layerId) {
        Layer layer = find(layerId);
        return layer != null && layer.completed();
    }

    public int size() {
        return layers.size();
    }

    /** Removes every layer and forgets per-part selection/preemption state. */
    public void clear() {
        layers.clear();
        Arrays.fill(selectedByPart, null);
    }

    /** True when at least one currently visible layer can still advance on some part. */
    public boolean needsTick() {
        for (int part = 0; part < rig.partCount(); part++) {
            Layer selected = select(part);
            if (selected != null && selected.needsAdvance(part)) return true;
        }
        return false;
    }

    /** Debug/inspection summary of the highest-priority registered layer. */
    public String topAnimationId() {
        Layer layer = topLayer();
        return layer == null ? null : layer.clip.animationId();
    }

    /** Debug/inspection summary of the highest-priority registered layer. */
    public RenderAnimationMode topMode() {
        Layer layer = topLayer();
        return layer == null ? null : layer.mode;
    }

    /** Representative clock for diagnostics; layered parts may intentionally diverge after preemption. */
    public float topTimeSeconds() {
        Layer layer = topLayer();
        return layer == null ? 0f : layer.representativeTime();
    }

    public boolean topCompleted() {
        Layer layer = topLayer();
        return layer != null && layer.completed();
    }

    public boolean topHolding() {
        Layer layer = topLayer();
        return layer != null && layer.holding();
    }

    /**
     * Advances the currently selected layer independently for every part and emits one dense local pose.
     * Missing tracks inside an affected override subtree emit identity, intentionally masking lower layers.
     */
    public void tick(float deltaSeconds, RenderAnimationBuffer out) {
        if (!Float.isFinite(deltaSeconds) || deltaSeconds < 0f) {
            throw new IllegalArgumentException("deltaSeconds must be finite and >= 0");
        }
        Objects.requireNonNull(out, "out");
        if (out.rig() != rig) throw new IllegalArgumentException("buffer/stack rig mismatch");
        out.reset();

        for (int part = 0; part < rig.partCount(); part++) {
            Layer next = select(part);
            Layer previous = selectedByPart[part];
            if (previous != next) {
                if (previous != null) previous.preempt(part);
                selectedByPart[part] = next;
            }
            if (next == null) continue;

            float time = next.advance(part, deltaSeconds);
            IndexedAnimationTrack track = next.clip.trackForPart(part);
            if (track == null) {
                out.set(part, 0f, 0f, 0f, 1f, 1f, 1f, 0f, 0f, 0f, false);
            } else {
                IndexedAnimationSampler.samplePart(track, time, out);
            }
            next.finishIfNeeded(part);
        }
    }

    private Layer select(int part) {
        for (Layer layer : layers) {
            if (layer.affected[part] && !layer.ended[part]) return layer;
        }
        return null;
    }

    private Layer find(String id) {
        Objects.requireNonNull(id, "id");
        for (Layer layer : layers) if (layer.id.equals(id)) return layer;
        return null;
    }

    private Layer topLayer() {
        return layers.isEmpty() ? null : layers.getFirst();
    }

    private final class Layer {
        private final String id;
        private final IndexedAnimationClip clip;
        private final RenderAnimationMode mode;
        private final int priority;
        private final long insertion;
        private final boolean[] affected;
        private final boolean[] ended;
        private final float[] timeByPart;

        private Layer(String id, IndexedAnimationClip clip, RenderAnimationMode mode, int priority, long insertion) {
            this.id = id;
            this.clip = clip;
            this.mode = mode;
            this.priority = priority;
            this.insertion = insertion;
            this.affected = affectedMask(clip);
            this.ended = new boolean[rig.partCount()];
            this.timeByPart = new float[rig.partCount()];
        }

        private boolean[] affectedMask(IndexedAnimationClip clip) {
            boolean[] affected = new boolean[rig.partCount()];
            if (!clip.override()) {
                java.util.Arrays.fill(affected, true);
                return affected;
            }
            for (int part = 0; part < affected.length; part++) {
                int parent = rig.parentIndex(part);
                affected[part] = clip.trackForPart(part) != null || (parent >= 0 && affected[parent]);
            }
            return affected;
        }

        private void preempt(int part) {
            if (ended[part]) return;
            if (mode == RenderAnimationMode.ONCE) {
                ended[part] = true;
            } else {
                timeByPart[part] = 0f;
            }
        }

        private float advance(int part, float deltaSeconds) {
            float length = clip.lengthSeconds();
            float time = timeByPart[part];
            switch (mode) {
                case ONCE, HOLD -> time = Math.min(length, time + deltaSeconds);
                case LOOP -> {
                    if (length == 0f) time = 0f;
                    else {
                        time = (time + deltaSeconds) % length;
                        if (time < 0f) time += length;
                    }
                }
            }
            timeByPart[part] = time;
            return time;
        }

        private void finishIfNeeded(int part) {
            if (mode == RenderAnimationMode.ONCE && timeByPart[part] >= clip.lengthSeconds()) {
                ended[part] = true;
            }
        }

        private boolean completed() {
            if (mode != RenderAnimationMode.ONCE) return false;
            for (int part = 0; part < affected.length; part++) {
                if (affected[part] && !ended[part]) return false;
            }
            return true;
        }

        private boolean holding() {
            if (mode != RenderAnimationMode.HOLD) return false;
            float length = clip.lengthSeconds();
            for (int part = 0; part < affected.length; part++) {
                if (affected[part] && timeByPart[part] < length) return false;
            }
            return true;
        }

        private boolean needsAdvance(int part) {
            return switch (mode) {
                case LOOP -> true;
                case ONCE -> !ended[part];
                case HOLD -> timeByPart[part] < clip.lengthSeconds();
            };
        }

        private float representativeTime() {
            for (int part = 0; part < affected.length; part++) {
                if (affected[part]) return timeByPart[part];
            }
            return 0f;
        }

        private boolean affectsAny() {
            for (boolean value : affected) if (value) return true;
            return false;
        }
    }
}
