package net.momirealms.craftengine.core.pack.model.bbmodel;

import net.momirealms.craftengine.core.util.Key;
import org.junit.jupiter.api.Test;
import java.nio.file.Path;
import java.util.Base64;
import com.google.gson.JsonParser;
import java.nio.file.Files;
import static org.junit.jupiter.api.Assertions.*;

class VsRenderDemoTest {
    @Test void nativeBlockDemoPreservesGeometryAndEmbeddedTexture() throws Exception {
        Path input = Path.of("../examples/vsrender_demo/blueprint/demo.bbmodel");
        var source = JsonParser.parseString(Files.readString(input)).getAsJsonObject();
        var converted = BBModelConverter.convert(input, Key.of("vsrender", "block/demo_table"), "vsrender-demo");
        assertEquals(5, converted.json().getAsJsonArray("elements").size());
        assertEquals(1, converted.textures().size());
        var texture = Key.of("vsrender", "block/demo_table");
        String data = source.getAsJsonArray("textures").get(0).getAsJsonObject().get("source").getAsString();
        assertArrayEquals(Base64.getDecoder().decode(data.substring(data.indexOf(',') + 1)), converted.textures().get(texture));
        assertEquals("vsrender:block/demo_table", converted.json().getAsJsonObject("textures").get("0").getAsString());
        for (int i = 0; i < 5; i++) {
            var expected = source.getAsJsonArray("elements").get(i).getAsJsonObject();
            var actual = converted.json().getAsJsonArray("elements").get(i).getAsJsonObject();
            assertEquals(expected.get("from"), actual.get("from"));
            assertEquals(expected.get("to"), actual.get("to"));
        }
    }
}
