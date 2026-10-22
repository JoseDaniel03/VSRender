package net.momirealms.craftengine.core.render.asset;

import net.momirealms.craftengine.core.render.animation.RenderVec3;
import net.momirealms.craftengine.core.render.rig.RenderPart;
import net.momirealms.craftengine.core.render.rig.RenderRig;
import net.momirealms.craftengine.core.render.rig.RenderRigType;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class NativeRenderVisualPlanTest {
    @Test
    void mapsOnlyGeometryPartsToDeterministicItemModels() {
        RenderRig rig = new RenderRig(
                NativeRenderAsset.PROVIDER_ID,
                "vsrender:chair",
                RenderRigType.MODEL,
                List.of(
                        new RenderPart("root", null, List.of("body", "socket")),
                        new RenderPart("body", "root", List.of()),
                        new RenderPart("socket", "root", List.of())
                ),
                Set.of()
        );
        EnumMap<NativeRenderFaceDirection, NativeRenderFace> faces = new EnumMap<>(NativeRenderFaceDirection.class);
        faces.put(NativeRenderFaceDirection.NORTH, new NativeRenderFace(
                "tex", new NativeRenderUv(0, 0, 16, 16), 0, null, -1
        ));
        NativeRenderCube cube = new NativeRenderCube(
                "cube", "body",
                RenderVec3.ZERO, new RenderVec3(1, 1, 1),
                RenderVec3.ZERO, RenderVec3.ZERO,
                true, false, 0, faces
        );
        NativeRenderAsset asset = new NativeRenderAsset(
                "vsrender:chair",
                "a".repeat(64),
                16, 16,
                rig, List.of(),
                Map.of("body", List.of(cube)),
                Map.of("tex", NativeRenderTexture.external("tex", "minecraft:block/stone")),
                NativeRenderRepresentation.allContexts()
        );

        NativeRenderVisualPlan plan = NativeRenderVisualPlan.compile(asset);
        assertEquals(1, plan.visualParts().size());
        var visual = plan.visualParts().getFirst();
        assertEquals(1, visual.partIndex());
        assertEquals("body", visual.partId());
        assertEquals("vsrender:native/chair_0c6b1ed025/body_230d8358", visual.itemModel());
    }
}
