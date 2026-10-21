package net.momirealms.craftengine.core.render.asset;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class VsRenderNativeAssetCacheTest {
    @Test
    void cacheRoundTripsAndInvalidatesWhenSourceChanges(@TempDir Path directory) throws Exception {
        Path source = directory.resolve("simple.bbmodel");
        Files.writeString(source, model(0));
        VsRenderNativeAssetCache cache = new VsRenderNativeAssetCache(directory.resolve("cache"));
        NativeRenderRepresentation representation = NativeRenderRepresentation.allContexts();

        var first = cache.loadOrImport(source, "vsrender:simple", representation);
        assertFalse(first.cacheHit());
        assertTrue(Files.isRegularFile(first.directory().resolve(VsRenderNativeAssetCodec.MANIFEST_FILE)));

        var second = cache.loadOrImport(source, "vsrender:simple", representation);
        assertTrue(second.cacheHit());
        assertEquals(first.asset().sourceSha256(), second.asset().sourceSha256());
        assertEquals(first.asset().rig().parts().size(), second.asset().rig().parts().size());

        Files.writeString(source, model(1));
        var changed = cache.loadOrImport(source, "vsrender:simple", representation);
        assertFalse(changed.cacheHit());
        assertNotEquals(first.asset().sourceSha256(), changed.asset().sourceSha256());
        assertNotEquals(first.directory(), changed.directory());
    }

    @Test
    void representationChangeRewritesInsteadOfReturningWrongCachedContract(@TempDir Path directory) throws Exception {
        Path source = directory.resolve("simple.bbmodel");
        Files.writeString(source, model(0));
        VsRenderNativeAssetCache cache = new VsRenderNativeAssetCache(directory.resolve("cache"));

        var item = cache.loadOrImport(
                source,
                "vsrender:simple",
                NativeRenderRepresentation.of(RenderUsageContext.ITEM)
        );
        var block = cache.loadOrImport(
                source,
                "vsrender:simple",
                NativeRenderRepresentation.of(RenderUsageContext.BLOCK)
        );

        assertFalse(item.cacheHit());
        assertFalse(block.cacheHit());
        assertTrue(block.asset().representation().supports(RenderUsageContext.BLOCK));
        assertFalse(block.asset().representation().supports(RenderUsageContext.ITEM));
    }

    private static String model(int offset) {
        return """
                {
                  "textures":[],
                  "elements":[],
                  "outliner":[{"name":"Body","uuid":"body","origin":[%d,0,0],"children":[]}],
                  "animations":[]
                }
                """.formatted(offset);
    }
}
