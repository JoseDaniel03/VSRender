package net.momirealms.craftengine.bukkit.render.runtime;

import net.momirealms.craftengine.core.render.animation.RenderBonePose;
import net.momirealms.craftengine.core.render.animation.RenderVec3;
import net.momirealms.craftengine.core.render.compiled.CompiledRenderAsset;
import net.momirealms.craftengine.core.render.instance.RenderTintLayer;
import net.momirealms.craftengine.core.render.rig.RenderMountPolicy;
import net.momirealms.craftengine.core.render.rig.RenderNameTagMode;
import net.momirealms.craftengine.core.render.rig.RenderPart;
import net.momirealms.craftengine.core.render.rig.RenderPartKind;
import net.momirealms.craftengine.core.render.rig.RenderPartRotationPolicy;
import net.momirealms.craftengine.core.render.rig.RenderPartSelector;
import net.momirealms.craftengine.core.render.rig.RenderRig;
import net.momirealms.craftengine.core.render.rig.RenderRigType;
import net.momirealms.craftengine.core.util.Color;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BukkitNativeDisplayAssetTintTest {

    @Test
    void resolvesChildrenAndLaterLayerWinsWithoutMutatingBaseAsset() {
        RenderPart root = part("root", null, List.of("child"));
        RenderPart child = part("child", "root", List.of("leaf"));
        RenderPart leaf = part("leaf", "child", List.of());
        RenderRig rig = new RenderRig("native", "tint", RenderRigType.MODEL, List.of(root, child, leaf), Set.of());
        CompiledRenderAsset compiled = CompiledRenderAsset.compile("tint", rig, List.of());
        BukkitDisplayPartPayload[] payloads = new BukkitDisplayPartPayload[compiled.rig().partCount()];
        java.util.Arrays.fill(payloads, BukkitDisplayPartPayload.transformOnly());
        BukkitNativeDisplayAsset base = new BukkitNativeDisplayAsset(compiled, payloads);

        BukkitNativeDisplayAsset tinted = base.withTintLayers(List.of(
                new RenderTintLayer(new Color(0xFF112233), RenderPartSelector.named(Set.of("root"), true)),
                new RenderTintLayer(new Color(0xFFABCDEF), RenderPartSelector.named(Set.of("child"), false))
        ));

        int rootIndex = compiled.rig().indexOf("root");
        int childIndex = compiled.rig().indexOf("child");
        int leafIndex = compiled.rig().indexOf("leaf");
        assertEquals(BukkitNativeDisplayAsset.NO_TINT, base.tintRgb(rootIndex));
        assertEquals(0x112233, tinted.tintRgb(rootIndex));
        assertEquals(0xABCDEF, tinted.tintRgb(childIndex));
        assertEquals(0x112233, tinted.tintRgb(leafIndex));
    }

    private static RenderPart part(String id, String parent, List<String> children) {
        return new RenderPart(
                id, parent, children, RenderPartKind.GROUP,
                RenderBonePose.IDENTITY, RenderPartRotationPolicy.NONE, null,
                RenderVec3.ZERO, RenderNameTagMode.NONE, RenderMountPolicy.DISABLED
        );
    }
}
