package net.momirealms.craftengine.core.render.compiled;

import net.momirealms.craftengine.core.render.animation.RenderAnimationClip;
import net.momirealms.craftengine.core.render.animation.RenderAnimationFrame;
import net.momirealms.craftengine.core.render.animation.RenderAnimationMode;
import net.momirealms.craftengine.core.render.animation.RenderAnimationTrack;
import net.momirealms.craftengine.core.render.animation.RenderBonePose;
import net.momirealms.craftengine.core.render.animation.RenderInterpolation;
import net.momirealms.craftengine.core.render.animation.RenderVec3;
import net.momirealms.craftengine.core.render.rig.RenderPart;
import net.momirealms.craftengine.core.render.rig.RenderPartKind;
import net.momirealms.craftengine.core.render.rig.RenderRig;
import net.momirealms.craftengine.core.render.rig.RenderRigType;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class CompiledRenderAssetTest {

    @Test
    void compilesDeterministicParentBeforeChildIndices() {
        RenderPart hand = part("hand", "arm", List.of());
        RenderPart root = part("root", null, List.of("arm"));
        RenderPart arm = part("arm", "root", List.of("hand"));
        RenderRig rig = new RenderRig("native", "demo", RenderRigType.MODEL, List.of(hand, root, arm), Set.of("wave"));

        IndexedRenderRig indexed = IndexedRenderRig.compile(rig);
        assertEquals(0, indexed.indexOf("root"));
        assertEquals(1, indexed.indexOf("arm"));
        assertEquals(2, indexed.indexOf("hand"));
        assertEquals(-1, indexed.parentIndex(0));
        assertEquals(0, indexed.parentIndex(1));
        assertEquals(1, indexed.parentIndex(2));
    }

    @Test
    void compiledClipTargetsPartIndicesAndRejectsUnknownParts() {
        RenderRig rig = rig(part("root", null, List.of("arm")), part("arm", "root", List.of()));
        RenderAnimationClip valid = clip("wave", track("arm", 0f, 10f));
        CompiledRenderAsset asset = CompiledRenderAsset.compile("demo", rig, List.of(valid));

        IndexedAnimationClip compiled = asset.animation("wave").orElseThrow();
        assertEquals(1, compiled.trackCount());
        RenderAnimationBuffer buffer = asset.createAnimationBuffer();
        IndexedAnimationSampler.sample(compiled, 0f, RenderAnimationMode.ONCE, buffer);
        assertTrue(buffer.active(asset.rig().indexOf("arm")));
        assertFalse(buffer.active(asset.rig().indexOf("root")));

        RenderAnimationClip invalid = clip("bad", track("missing", 0f, 1f));
        assertThrows(IllegalArgumentException.class, () -> CompiledRenderAsset.compile("demo", rig, List.of(invalid)));
    }

    @Test
    void locatorIndicesArePrecomputed() {
        RenderPart root = part("root", null, List.of("weapon_socket"));
        RenderPart locator = new RenderPart("weapon_socket", "root", List.of(), RenderPartKind.LOCATOR, RenderBonePose.IDENTITY);
        IndexedRenderRig rig = IndexedRenderRig.compile(rig(root, locator));
        assertArrayEquals(new int[]{1}, rig.attachmentIndices());
    }

    private static RenderPart part(String id, String parent, List<String> children) {
        return new RenderPart(id, parent, children, RenderPartKind.GROUP, RenderBonePose.IDENTITY);
    }

    private static RenderRig rig(RenderPart... parts) {
        return new RenderRig("native", "demo", RenderRigType.MODEL, List.of(parts), Set.of());
    }

    private static RenderAnimationClip clip(String id, RenderAnimationTrack track) {
        Map<String, RenderAnimationTrack> tracks = new LinkedHashMap<>();
        tracks.put(track.partId(), track);
        return new RenderAnimationClip(id, 1f, RenderAnimationMode.ONCE, false, tracks);
    }

    private static RenderAnimationTrack track(String id, float from, float to) {
        return new RenderAnimationTrack(id, List.of(
                new RenderAnimationFrame(0f, pose(from), RenderInterpolation.LINEAR),
                new RenderAnimationFrame(1f, pose(to), RenderInterpolation.LINEAR)
        ));
    }

    private static RenderBonePose pose(float x) {
        return new RenderBonePose(new RenderVec3(x, 0, 0), RenderVec3.ONE, RenderVec3.ZERO);
    }
}
