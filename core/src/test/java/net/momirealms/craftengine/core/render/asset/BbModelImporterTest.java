package net.momirealms.craftengine.core.render.asset;

import net.momirealms.craftengine.core.render.animation.RenderAnimationMode;
import net.momirealms.craftengine.core.render.animation.RenderInterpolation;
import net.momirealms.craftengine.core.render.rig.RenderPartKind;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BbModelImporterTest {
    private static final String PNG = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII=";

    @Test
    void importsHierarchyGeometryTextureAndLinearAnimation(@TempDir Path directory) throws Exception {
        Path source = directory.resolve("chair.bbmodel");
        Files.writeString(source, """
                {
                  "resolution":{"width":16,"height":16},
                  "textures":[{"uuid":"tex-uuid","name":"chair.png","source":"data:image/png;base64,%s"}],
                  "elements":[{
                    "type":"cube","uuid":"cube-1","name":"seat",
                    "from":[8,8,8],"to":[16,16,16],"origin":[8,8,8],
                    "faces":{"north":{"uv":[0,0,16,16],"texture":0}}
                  }],
                  "outliner":[{
                    "name":"Body","uuid":"bone-body","origin":[8,8,8],"rotation":[0,10,0],
                    "children":["cube-1",{"name":"Socket","uuid":"locator-1","type":"locator","position":[8,24,8]}]
                  }],
                  "animations":[{
                    "name":"move","loop":"loop","length":1,
                    "animators":{"bone-body":{"type":"bone","keyframes":[
                      {"channel":"position","time":0,"interpolation":"linear","data_points":[{"x":"0","y":"0","z":"0"}]},
                      {"channel":"position","time":1,"interpolation":"linear","data_points":[{"x":"16","y":"0","z":"0"}]}
                    ]}}
                  }]
                }
                """.formatted(PNG));

        NativeRenderAsset asset = new BbModelImporter().importFile(
                source,
                "vsrender:chair",
                NativeRenderRepresentation.allContexts()
        );

        assertEquals("vsrender:chair", asset.assetId());
        assertEquals(NativeRenderAsset.PROVIDER_ID, asset.rig().providerId());
        assertEquals(List.of("body"), asset.rig().part("root").orElseThrow().children());
        assertEquals(RenderPartKind.GROUP, asset.rig().part("body").orElseThrow().kind());
        assertEquals(RenderPartKind.LOCATOR, asset.rig().part("socket").orElseThrow().kind());
        assertEquals(0.5f, asset.rig().part("body").orElseThrow().restPose().translation().x(), 0.0001f);
        assertEquals(1f, asset.rig().part("socket").orElseThrow().restPose().translation().y(), 0.0001f);

        NativeRenderCube cube = asset.geometry("body").getFirst();
        assertEquals(0f, cube.from().x(), 0.0001f);
        assertEquals(8f, cube.to().x(), 0.0001f);
        assertEquals("tex_0", cube.faces().get(NativeRenderFaceDirection.NORTH).textureId());
        assertTrue(asset.textures().get("tex_0").embedded());

        var clip = asset.animations().getFirst();
        assertEquals(RenderAnimationMode.LOOP, clip.defaultMode());
        var frames = clip.tracks().get("body").frames();
        assertEquals(2, frames.size());
        assertEquals(RenderInterpolation.LINEAR, frames.getFirst().interpolation());
        assertEquals(1f, frames.getLast().pose().translation().x(), 0.0001f);
        assertEquals("vsrender:chair", asset.compile().assetId());
    }

    @Test
    void rejectsUnsupportedBezierInsteadOfSilentlyChangingAnimation(@TempDir Path directory) throws Exception {
        Path source = directory.resolve("bad.bbmodel");
        Files.writeString(source, """
                {
                  "textures":[],
                  "elements":[],
                  "outliner":[{"name":"Body","uuid":"bone","origin":[0,0,0],"children":[]}],
                  "animations":[{
                    "name":"bad","length":1,
                    "animators":{"bone":{"type":"bone","keyframes":[
                      {"channel":"rotation","time":0,"interpolation":"bezier","data_points":[{"x":"0","y":"0","z":"0"}]}
                    ]}}
                  }]
                }
                """);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () ->
                new BbModelImporter().importFile(source, "vsrender:bad", NativeRenderRepresentation.allContexts())
        );
        assertTrue(error.getMessage().contains("interpolation"));
    }
}
