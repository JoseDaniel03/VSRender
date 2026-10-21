package net.momirealms.craftengine.core.render.asset;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.momirealms.craftengine.core.render.animation.RenderBonePose;
import net.momirealms.craftengine.core.render.animation.RenderVec3;
import net.momirealms.craftengine.core.render.rig.RenderPart;
import net.momirealms.craftengine.core.render.rig.RenderPartKind;
import net.momirealms.craftengine.core.render.rig.RenderRig;
import net.momirealms.craftengine.core.render.rig.RenderRigType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class NativeResourcePackCompilerTest {
    @Test
    void emitsPerPartModelItemDefinitionTextureAndSharedContextManifest(@TempDir Path directory) throws Exception {
        RenderRig rig = new RenderRig(
                NativeRenderAsset.PROVIDER_ID,
                "vsrender:chair",
                RenderRigType.MODEL,
                List.of(
                        new RenderPart("root", null, List.of("body")),
                        new RenderPart("body", "root", List.of(), RenderPartKind.GROUP, RenderBonePose.IDENTITY)
                ),
                Set.of()
        );
        EnumMap<NativeRenderFaceDirection, NativeRenderFace> faces = new EnumMap<>(NativeRenderFaceDirection.class);
        faces.put(NativeRenderFaceDirection.NORTH, new NativeRenderFace(
                "tex_0", new NativeRenderUv(0, 0, 32, 16), 0, null, -1
        ));
        NativeRenderCube cube = new NativeRenderCube(
                "cube", "body",
                new RenderVec3(-8, -8, -8),
                new RenderVec3(8, 8, 8),
                RenderVec3.ZERO,
                new RenderVec3(0, 90, 0),
                true, false, 0, faces
        );
        NativeRenderAsset asset = new NativeRenderAsset(
                "vsrender:chair",
                "c".repeat(64),
                32,
                16,
                rig,
                List.of(),
                Map.of("body", List.of(cube)),
                Map.of("tex_0", NativeRenderTexture.embedded("tex_0", new byte[]{1, 2, 3})),
                NativeRenderRepresentation.allContexts()
        );

        NativeResourcePackCompiler.Result result = new NativeResourcePackCompiler().compile(asset, directory);
        String itemModel = result.itemModel(RenderUsageContext.FURNITURE, "body").orElseThrow();
        assertEquals("vsrender:native/chair_0c6b1ed025/body_230d8358", itemModel);
        for (RenderUsageContext context : RenderUsageContext.values()) {
            assertEquals(itemModel, result.itemModel(context, "body").orElseThrow());
        }

        Path modelFile = directory.resolve("assets/vsrender/models/native/chair_0c6b1ed025/body_230d8358.json");
        Path itemFile = directory.resolve("assets/vsrender/items/native/chair_0c6b1ed025/body_230d8358.json");
        Path textureFile = directory.resolve("assets/vsrender/textures/native/chair_0c6b1ed025/textures/tex_0.png");
        assertTrue(Files.isRegularFile(modelFile));
        assertTrue(Files.isRegularFile(itemFile));
        assertTrue(Files.isRegularFile(textureFile));
        assertTrue(Files.isRegularFile(result.manifestFile()));

        JsonObject model = JsonParser.parseString(Files.readString(modelFile)).getAsJsonObject();
        assertEquals(32, model.getAsJsonArray("texture_size").get(0).getAsInt());
        assertEquals("vsrender:native/chair_0c6b1ed025/textures/tex_0", model.getAsJsonObject("textures").get("tex_0").getAsString());
        JsonObject rotation = model.getAsJsonArray("elements").get(0).getAsJsonObject().getAsJsonObject("rotation");
        assertEquals(0f, rotation.get("x").getAsFloat(), 0.0001f);
        assertEquals(90f, rotation.get("y").getAsFloat(), 0.0001f);
        assertEquals(0f, rotation.get("z").getAsFloat(), 0.0001f);

        JsonObject item = JsonParser.parseString(Files.readString(itemFile)).getAsJsonObject();
        assertEquals("vsrender:native/chair_0c6b1ed025/body_230d8358", item.getAsJsonObject("model").get("model").getAsString());

        JsonObject manifest = JsonParser.parseString(Files.readString(result.manifestFile())).getAsJsonObject();
        assertEquals(6, manifest.getAsJsonArray("contexts").size());
        assertEquals("minecraft:paper", manifest.get("carrier_item").getAsString());
    }

    @Test
    void rejectsNonVanillaCubeRotationInsteadOfWritingInvalidModelJson(@TempDir Path directory) throws Exception {
        RenderRig rig = new RenderRig(
                NativeRenderAsset.PROVIDER_ID,
                "vsrender:bad_rotation",
                RenderRigType.MODEL,
                List.of(new RenderPart("root", null, List.of("body")), new RenderPart("body", "root", List.of())),
                Set.of()
        );
        EnumMap<NativeRenderFaceDirection, NativeRenderFace> faces = new EnumMap<>(NativeRenderFaceDirection.class);
        faces.put(NativeRenderFaceDirection.NORTH, new NativeRenderFace(
                "tex", new NativeRenderUv(0, 0, 16, 16), 0, null, -1
        ));
        NativeRenderCube cube = new NativeRenderCube(
                "bad", "body", RenderVec3.ZERO, new RenderVec3(1, 1, 1),
                RenderVec3.ZERO, new RenderVec3(0, 90, 0),
                true, false, 0, faces
        );
        NativeRenderAsset asset = new NativeRenderAsset(
                "vsrender:bad_rotation", "e".repeat(64), 16, 16, "1.21.10",
                rig, List.of(), Map.of("body", List.of(cube)),
                Map.of("tex", NativeRenderTexture.external("tex", "minecraft:block/stone")),
                NativeRenderRepresentation.allContexts()
        );

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> new NativeResourcePackCompiler().compile(asset, directory)
        );
        assertTrue(error.getMessage().contains("unsupported by java_block_version"));
    }

    @Test
    void contextGateIsAppliedWithoutDuplicatingGeneratedModel(@TempDir Path directory) throws Exception {
        RenderRig rig = new RenderRig(
                NativeRenderAsset.PROVIDER_ID,
                "vsrender:item",
                RenderRigType.MODEL,
                List.of(new RenderPart("root", null, List.of())),
                Set.of()
        );
        NativeRenderAsset asset = new NativeRenderAsset(
                "vsrender:item",
                "d".repeat(64),
                16, 16,
                rig, List.of(), Map.of(), Map.of(),
                NativeRenderRepresentation.of(RenderUsageContext.ITEM)
        );
        var result = new NativeResourcePackCompiler().compile(asset, directory);
        assertTrue(result.itemModel(RenderUsageContext.BLOCK, "root").isEmpty());
        assertTrue(result.itemModelsByPart().isEmpty());
    }
}
