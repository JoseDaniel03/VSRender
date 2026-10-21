package net.momirealms.craftengine.core.render.asset;

import net.momirealms.craftengine.core.render.animation.RenderAnimationClip;
import net.momirealms.craftengine.core.render.animation.RenderAnimationMode;
import net.momirealms.craftengine.core.render.rig.RenderPart;
import net.momirealms.craftengine.core.render.rig.RenderRig;
import net.momirealms.craftengine.core.render.rig.RenderRigType;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class NativeRenderAssetTest {
    @Test
    void oneCanonicalAssetCanServeAllSixContextsAndCompileOnce() {
        RenderAnimationClip idle = new RenderAnimationClip("idle", 0f, RenderAnimationMode.HOLD, false, Map.of());
        RenderRig rig = new RenderRig(
                NativeRenderAsset.PROVIDER_ID,
                "demo",
                RenderRigType.MODEL,
                List.of(new RenderPart("root", null, List.of())),
                Set.of("idle")
        );
        NativeRenderAsset asset = new NativeRenderAsset(
                "vsrender:demo",
                "a".repeat(64),
                16,
                16,
                rig,
                List.of(idle),
                Map.of(),
                Map.of(),
                NativeRenderRepresentation.allContexts()
        );

        NativeRenderAssetRegistry registry = new NativeRenderAssetRegistry();
        registry.register(asset);
        for (RenderUsageContext context : RenderUsageContext.values()) {
            assertSame(asset, registry.resolve("vsrender:demo", context).orElseThrow());
        }
        assertEquals(EnumSet.allOf(RenderUsageContext.class), asset.representation().contexts());
        assertEquals("vsrender:demo", asset.compile().assetId());
        assertEquals(1, asset.compile().rig().partCount());
    }

    @Test
    void registryRejectsUnsupportedContextWithoutDuplicatingAsset() {
        RenderRig rig = new RenderRig(
                NativeRenderAsset.PROVIDER_ID,
                "item",
                RenderRigType.MODEL,
                List.of(new RenderPart("root", null, List.of())),
                Set.of()
        );
        NativeRenderAsset asset = new NativeRenderAsset(
                "vsrender:item",
                "b".repeat(64),
                16,
                16,
                rig,
                List.of(),
                Map.of(),
                Map.of(),
                NativeRenderRepresentation.of(RenderUsageContext.ITEM, RenderUsageContext.PLAYER_ATTACHMENT)
        );
        NativeRenderAssetRegistry registry = new NativeRenderAssetRegistry();
        registry.register(asset);

        assertTrue(registry.resolve("vsrender:item", RenderUsageContext.ITEM).isPresent());
        assertTrue(registry.resolve("vsrender:item", RenderUsageContext.PLAYER_ATTACHMENT).isPresent());
        assertTrue(registry.resolve("vsrender:item", RenderUsageContext.BLOCK).isEmpty());
        assertEquals(1, registry.assets().size());
    }
}
