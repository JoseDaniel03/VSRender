package net.momirealms.craftengine.bukkit.plugin.web;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class NativeBlueprintPackValidatorTest {
    @Test
    void allowsOnlyReservedBbmodelAndRelativePngPaths() {
        assertTrue(NativeBlueprintPackValidator.allowedFile("blueprint/native/faro12.bbmodel"));
        assertTrue(NativeBlueprintPackValidator.allowedFile("blueprint/native/workbench/part.bbmodel"));
        assertTrue(NativeBlueprintPackValidator.allowedFile("blueprint/native/workbench/palette.png"));
        assertFalse(NativeBlueprintPackValidator.allowedFile("models/faro12.bbmodel"));
        assertFalse(NativeBlueprintPackValidator.allowedFile("blueprint/native/../secret.bbmodel"));
        assertFalse(NativeBlueprintPackValidator.allowedFile("blueprint/native/evil.js"));
        assertFalse(NativeBlueprintPackValidator.allowedFile("blueprint/native/Upper.bbmodel"));
    }

    @Test
    void importsModelsWithCompatibleLinearAnimation(@TempDir Path stage) throws Exception {
        Path nativeRoot = stage.resolve("blueprint/native");
        Files.createDirectories(nativeRoot.resolve("weapon"));
        Files.writeString(nativeRoot.resolve("weapon/faro12.bbmodel"), """
                {
                  "textures":[],
                  "elements":[{"uuid":"cube","type":"cube","name":"part","from":[0,0,0],
                    "to":[4,4,4],"faces":{}}],
                  "outliner":[{"name":"Body","uuid":"body","origin":[0,0,0],"children":["cube"]}],
                  "animations":[{"name":"assemble","length":1,"loop":"once",
                    "animators":{"body":{"type":"bone","keyframes":[
                      {"channel":"position","time":0,"interpolation":"linear","data_points":[{"x":"0","y":"0","z":"0"}]},
                      {"channel":"position","time":1,"interpolation":"linear","data_points":[{"x":"1","y":"0","z":"0"}]}
                    ]}}}]
                }
                """);
        assertEquals(1, NativeBlueprintPackValidator.validate(stage, "faro12"));
    }

    @Test
    void refusesAbsoluteAndTraversalTextures(@TempDir Path stage) throws Exception {
        Path root = stage.resolve("blueprint/native");
        Files.createDirectories(root);
        Path model = root.resolve("bad.bbmodel");
        Files.writeString(model, withTexture("file:///etc/passwd"));
        assertThrows(IllegalArgumentException.class, () -> NativeBlueprintPackValidator.validate(stage, "faro12"));
        Files.writeString(model, withTexture("../outside.png"));
        assertThrows(IllegalArgumentException.class, () -> NativeBlueprintPackValidator.validate(stage, "faro12"));
    }

    @Test
    void rejectsUnsupportedBezierBeforePublishingPack(@TempDir Path stage) throws Exception {
        Path nativeRoot = stage.resolve("blueprint/native");
        Files.createDirectories(nativeRoot);
        Files.writeString(nativeRoot.resolve("invalid.bbmodel"), """
                {
                  "textures":[],
                  "elements":[],
                  "outliner":[{"name":"Body","uuid":"body","origin":[0,0,0],"children":[]}],
                  "animations":[{"name":"bad","length":1,"animators":{"body":{"type":"bone","keyframes":[
                    {"channel":"rotation","time":0,"interpolation":"bezier","data_points":[{"x":"0","y":"0","z":"0"}]}
                  ]}}}]
                }
                """);
        assertThrows(IllegalArgumentException.class, () -> NativeBlueprintPackValidator.validate(stage, "faro12"));
    }

    @Test
    void supportsRelativePngTextureNextToModel(@TempDir Path stage) throws Exception {
        Path root = stage.resolve("blueprint/native/weapon");
        Files.createDirectories(root);
        byte[] png = java.util.Base64.getDecoder().decode(
                "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVQIHWP4z8DwHwAFgAI/ScL/nwAAAABJRU5ErkJggg==");
        Files.write(root.resolve("palette.png"), png);
        Files.writeString(root.resolve("model.bbmodel"), withTexture("palette.png"));
        assertEquals(1, NativeBlueprintPackValidator.validate(stage, "faro12"));
    }

    private static String withTexture(String source) {
        return """
                {"textures":[{"name":"palette.png","source":"%s"}],
                "elements":[],"outliner":[],"animations":[]}
                """.formatted(source);
    }
}
