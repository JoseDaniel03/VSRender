package net.momirealms.craftengine.core.render.asset;

import net.momirealms.craftengine.core.pack.Pack;
import net.momirealms.craftengine.core.pack.PackMeta;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class NativeRenderPackLoaderTest {
    @Test
    void scansReservedBlueprintFolderStagesPackOutputAndPopulatesRegistry(@TempDir Path directory) throws Exception {
        Path packFolder = directory.resolve("demo");
        Path nativeBlueprints = packFolder.resolve("blueprint/native");
        Files.createDirectories(nativeBlueprints);
        Path source = nativeBlueprints.resolve("chair.bbmodel");
        Files.writeString(source, minimalModel(0));

        Pack pack = new Pack(packFolder, new PackMeta("test", "test", "1", "demo"), true, new String[0]);
        NativeRenderAssetRegistry registry = new NativeRenderAssetRegistry();
        NativeRenderPackLoader loader = new NativeRenderPackLoader(directory.resolve("native-assets"), registry);

        var first = loader.load(List.of(pack));
        assertEquals(1, first.loadedCount());
        assertEquals(0, first.cacheHits());
        assertTrue(registry.resolve("demo:chair", RenderUsageContext.ITEM).isPresent());

        String isolated = "chair_" + NativeAssetHashes.sha256("demo:chair".getBytes(java.nio.charset.StandardCharsets.UTF_8)).substring(0, 10);
        Path manifest = packFolder.resolve("resourcepack/assets/demo/vsrender/native")
                .resolve(isolated).resolve("manifest.json");
        assertTrue(Files.isRegularFile(manifest));

        var second = loader.load(List.of(pack));
        assertEquals(1, second.cacheHits());
        assertEquals(1, registry.assets().size());
    }

    @Test
    void failedReloadKeepsPreviousRegistryAndDoesNotInstallPartialResourcePack(@TempDir Path directory) throws Exception {
        Path packFolder = directory.resolve("demo");
        Path nativeBlueprints = packFolder.resolve("blueprint/native");
        Files.createDirectories(nativeBlueprints);
        Files.writeString(nativeBlueprints.resolve("good.bbmodel"), minimalModel(0));

        Pack pack = new Pack(packFolder, new PackMeta(null, null, null, "demo"), true, new String[0]);
        NativeRenderAssetRegistry registry = new NativeRenderAssetRegistry();
        NativeRenderPackLoader loader = new NativeRenderPackLoader(directory.resolve("native-assets"), registry);
        loader.load(List.of(pack));
        assertTrue(registry.get("demo:good").isPresent());

        Files.writeString(nativeBlueprints.resolve("bad.bbmodel"), """
                {
                  "textures":[],
                  "elements":[],
                  "outliner":[{"name":"Body","uuid":"body","origin":[0,0,0],"children":[]}],
                  "animations":[{"name":"bad","length":1,"animators":{"body":{"type":"bone","keyframes":[
                    {"channel":"rotation","time":0,"interpolation":"bezier","data_points":[{"x":"0","y":"0","z":"0"}]}
                  ]}}}]
                }
                """);

        assertThrows(Exception.class, () -> loader.load(List.of(pack)));
        assertTrue(registry.get("demo:good").isPresent(), "previous live registry must survive failed reload");
    }

    @Test
    void removingSourceRemovesReservedGeneratedOutputAndRegistryEntry(@TempDir Path directory) throws Exception {
        Path packFolder = directory.resolve("demo");
        Path nativeBlueprints = packFolder.resolve("blueprint/native");
        Files.createDirectories(nativeBlueprints);
        Path source = nativeBlueprints.resolve("old.bbmodel");
        Files.writeString(source, minimalModel(0));

        Pack pack = new Pack(packFolder, new PackMeta(null, null, null, "demo"), true, new String[0]);
        NativeRenderAssetRegistry registry = new NativeRenderAssetRegistry();
        NativeRenderPackLoader loader = new NativeRenderPackLoader(directory.resolve("native-assets"), registry);
        loader.load(List.of(pack));
        assertFalse(registry.assets().isEmpty());

        Files.delete(source);
        var result = loader.load(List.of(pack));
        assertEquals(0, result.loadedCount());
        assertTrue(registry.assets().isEmpty());
        assertFalse(Files.exists(packFolder.resolve("resourcepack/assets/demo/models/native")));
        assertFalse(Files.exists(packFolder.resolve("resourcepack/assets/demo/items/native")));
    }

    private static String minimalModel(int x) {
        return """
                {
                  "textures":[],
                  "elements":[],
                  "outliner":[{"name":"Body","uuid":"body","origin":[%d,0,0],"children":[]}],
                  "animations":[]
                }
                """.formatted(x);
    }
}
