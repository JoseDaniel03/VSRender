package net.momirealms.craftengine.bukkit.plugin.web;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.momirealms.craftengine.core.render.asset.BbModelImporter;
import net.momirealms.craftengine.core.render.asset.NativeRenderRepresentation;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;

/**
 * Preflight for untrusted Blockbench projects uploaded in a complete web pack ZIP.
 * Only blueprint/native files can reach the native importer and no local/absolute
 * texture reference may escape the temporary pack staging directory.
 */
final class NativeBlueprintPackValidator {
    private static final String PREFIX = "blueprint/native/";
    private static final String EMBEDDED_PNG = "data:image/png;base64:";
    private static final byte[] PNG_MAGIC = {(byte) 137, 80, 78, 71, 13, 10, 26, 10};

    private NativeBlueprintPackValidator() {
    }

    static boolean allowedFile(String path) {
        if (!path.startsWith(PREFIX)) return false;
        String relative = path.substring(PREFIX.length());
        if (!relative.matches("(?:[a-z0-9_.-]+/)*[a-z0-9_.-]+\\.(?:bbmodel|png)")) return false;
        for (String part : relative.split("/")) {
            if (part.equals(".") || part.equals("..")) return false;
        }
        return true;
    }

    static int validate(Path stagedPack, String namespace) throws IOException {
        Path nativeRoot = stagedPack.resolve(PREFIX).normalize();
        if (!Files.isDirectory(nativeRoot)) return 0;
        int count = 0;
        try (var stream = Files.walk(nativeRoot)) {
            List<Path> models = stream.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".bbmodel"))
                    .sorted(Comparator.naturalOrder()).toList();
            BbModelImporter importer = new BbModelImporter();
            for (Path model : models) {
                String relative = nativeRoot.relativize(model).toString().replace('\\', '/');
                try {
                    validateTextureSources(model, nativeRoot);
                    String assetId = namespace + ":" + relative.substring(0, relative.length() - ".bbmodel".length());
                    importer.importFile(model, assetId, NativeRenderRepresentation.allContexts()).compile();
                } catch (RuntimeException | IOException invalid) {
                    throw new IllegalArgumentException("BBModel no compatible (" + relative
                            + "): " + invalid.getMessage(), invalid);
                }
                count++;
            }
        }
        return count;
    }

    private static void validateTextureSources(Path model, Path nativeRoot) throws IOException {
        JsonElement root = JsonParser.parseString(Files.readString(model, StandardCharsets.UTF_8));
        if (!root.isJsonObject()) throw new IllegalArgumentException("JSON principal invalido");
        JsonElement textures = root.getAsJsonObject().get("textures");
        if (textures == null || textures.isJsonNull()) return;
        if (!textures.isJsonArray()) throw new IllegalArgumentException("textures debe ser un array");
        JsonArray entries = textures.getAsJsonArray();
        for (JsonElement entry : entries) {
            if (!entry.isJsonObject()) throw new IllegalArgumentException("Entrada de textura invalida");
            JsonObject texture = entry.getAsJsonObject();
            String source = value(texture, "source");
            if (source.startsWith(EMBEDDED_PNG)) {
                byte[] image = Base64.getDecoder().decode(source.substring(EMBEDDED_PNG.length()));
                if (!hasPngHeader(image)) throw new IllegalArgumentException("Textura PNG embebida invalida");
                // Blockbench may retain an absolute 'path' from the author's
                // workstation; the importer never reads it when source is embedded.
                continue;
            }
            validateSource(source, model, nativeRoot);
            validateSource(value(texture, "path"), model, nativeRoot);
        }
    }

    private static void validateSource(String ref, Path model, Path nativeRoot) {
        if (ref.isBlank()) return;
        if (ref.startsWith("data:") || ref.startsWith("file:"))
            throw new IllegalArgumentException("Solo se admiten PNG embebidos o rutas relativas seguras");
        // Minecraft resource locations do not access server-side files.
        if (ref.matches("[a-z0-9_.-]+:[a-z0-9_./-]+")) return;
        if (ref.contains(":") || ref.contains("\\") || ref.startsWith("/") || ref.indexOf('\0') >= 0)
            throw new IllegalArgumentException("Ruta de textura insegura");
        Path relative = Path.of(ref);
        if (relative.isAbsolute()) throw new IllegalArgumentException("Ruta de textura absoluta");
        for (Path part : relative) {
            if (part.toString().equals("..")) throw new IllegalArgumentException("Ruta de textura fuera del pack");
        }
        Path resolved = model.getParent().resolve(relative).normalize();
        if (!resolved.startsWith(nativeRoot) || !Files.isRegularFile(resolved))
            throw new IllegalArgumentException("Textura relativa ausente o fuera de blueprint/native: " + ref);
    }

    private static String value(JsonObject object, String field) {
        JsonElement value = object.get(field);
        if (value == null || value.isJsonNull()) return "";
        if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString())
            throw new IllegalArgumentException("Referencia de textura invalida: " + field);
        return value.getAsString();
    }

    private static boolean hasPngHeader(byte[] data) {
        if (data.length < PNG_MAGIC.length) return false;
        for (int i = 0; i < PNG_MAGIC.length; i++) {
            if (data[i] != PNG_MAGIC[i]) return false;
        }
        return true;
    }
}
