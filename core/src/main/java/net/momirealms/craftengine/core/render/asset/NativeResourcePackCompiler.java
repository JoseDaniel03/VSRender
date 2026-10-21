package net.momirealms.craftengine.core.render.asset;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.render.animation.RenderVec3;
import net.momirealms.craftengine.core.util.GsonHelper;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Compiles canonical native VsRender geometry into a Minecraft resource-pack subtree.
 *
 * <p>Each visual rig part receives one model and one modern item-model definition. The same item
 * model keys are shared by ITEM/BLOCK/ENTITY/FURNITURE/PLAYER_ATTACHMENT/VEHICLE consumers; the
 * gameplay owner only chooses context and placement.</p>
 */
public final class NativeResourcePackCompiler {
    public Result compile(NativeRenderAsset asset, Path outputRoot) throws IOException {
        Objects.requireNonNull(asset, "asset");
        Objects.requireNonNull(outputRoot, "outputRoot");
        AssetKey key = AssetKey.parse(asset.assetId());
        String isolatedPath = key.isolatedPath();

        Path modelBase = outputRoot.resolve("assets").resolve(key.namespace()).resolve("models")
                .resolve("native").resolve(isolatedPath);
        Path itemBase = outputRoot.resolve("assets").resolve(key.namespace()).resolve("items")
                .resolve("native").resolve(isolatedPath);
        Path textureBase = outputRoot.resolve("assets").resolve(key.namespace()).resolve("textures")
                .resolve("native").resolve(isolatedPath).resolve("textures");
        Path metadataBase = outputRoot.resolve("assets").resolve(key.namespace()).resolve("vsrender")
                .resolve("native").resolve(isolatedPath);

        recreateDirectory(modelBase);
        recreateDirectory(itemBase);
        recreateDirectory(textureBase);
        recreateDirectory(metadataBase);

        ArrayList<Path> written = new ArrayList<>();
        LinkedHashMap<String, String> textureLocations = writeTextures(asset, key, textureBase, written);
        LinkedHashMap<String, String> itemModelsByPart = new LinkedHashMap<>();
        for (Map.Entry<String, List<NativeRenderCube>> entry : asset.geometry().entrySet()) {
            if (entry.getValue().isEmpty()) continue;
            String partId = entry.getKey();
            String leaf = partLeaf(partId);

            String relativeModel = "native/" + isolatedPath + "/" + leaf;
            String modelLocation = key.namespace() + ":" + relativeModel;
            String itemModelLocation = modelLocation;

            JsonObject modelJson = modelJson(asset, entry.getValue(), textureLocations);
            Path modelFile = modelBase.resolve(leaf + ".json");
            writeJson(modelFile, modelJson);
            written.add(modelFile);

            JsonObject itemDefinition = new JsonObject();
            JsonObject model = new JsonObject();
            model.addProperty("type", "minecraft:model");
            model.addProperty("model", modelLocation);
            itemDefinition.add("model", model);
            Path itemFile = itemBase.resolve(leaf + ".json");
            writeJson(itemFile, itemDefinition);
            written.add(itemFile);

            itemModelsByPart.put(partId, itemModelLocation);
        }

        JsonObject manifest = new JsonObject();
        manifest.addProperty("schema_version", NativeRenderAsset.SCHEMA_VERSION);
        manifest.addProperty("asset_id", asset.assetId());
        manifest.addProperty("source_sha256", asset.sourceSha256());
        if (!asset.javaBlockVersion().isBlank()) manifest.addProperty("java_block_version", asset.javaBlockVersion());
        manifest.addProperty("carrier_item", asset.representation().carrierItem());
        JsonArray contexts = new JsonArray();
        asset.representation().contexts().stream().map(Enum::name).sorted().forEach(contexts::add);
        manifest.add("contexts", contexts);
        JsonObject parts = new JsonObject();
        itemModelsByPart.forEach(parts::addProperty);
        manifest.add("item_models", parts);
        Path manifestFile = metadataBase.resolve("manifest.json");
        writeJson(manifestFile, manifest);
        written.add(manifestFile);

        return new Result(
                asset.assetId(),
                asset.representation(),
                Collections.unmodifiableMap(itemModelsByPart),
                List.copyOf(written),
                manifestFile
        );
    }

    private static LinkedHashMap<String, String> writeTextures(NativeRenderAsset asset,
                                                                AssetKey key,
                                                                Path textureBase,
                                                                List<Path> written) throws IOException {
        LinkedHashMap<String, String> locations = new LinkedHashMap<>();
        HashSet<String> usedLeaves = new HashSet<>();
        for (NativeRenderTexture texture : asset.textures().values()) {
            if (!texture.embedded()) {
                locations.put(texture.id(), Objects.requireNonNull(texture.resourceLocation(), "external texture location"));
                continue;
            }
            String leaf = safeLeaf(texture.id());
            if (!usedLeaves.add(leaf)) {
                leaf = leaf + "_" + NativeAssetHashes.sha256(texture.id().getBytes(java.nio.charset.StandardCharsets.UTF_8)).substring(0, 8);
                if (!usedLeaves.add(leaf)) throw new IllegalArgumentException("texture path collision: " + texture.id());
            }
            Path file = textureBase.resolve(leaf + ".png");
            Files.createDirectories(file.getParent());
            Files.write(file, Objects.requireNonNull(texture.png(), "embedded texture bytes"));
            written.add(file);
            locations.put(texture.id(), key.namespace() + ":native/" + key.isolatedPath() + "/textures/" + leaf);
        }
        return locations;
    }

    private static JsonObject modelJson(NativeRenderAsset asset,
                                        List<NativeRenderCube> cubes,
                                        Map<String, String> textureLocations) {
        JsonObject model = new JsonObject();
        if (asset.textureWidth() != 16 || asset.textureHeight() != 16) {
            JsonArray size = new JsonArray();
            size.add(asset.textureWidth());
            size.add(asset.textureHeight());
            model.add("texture_size", size);
        }

        LinkedHashSet<String> usedTextureIds = new LinkedHashSet<>();
        for (NativeRenderCube cube : cubes) {
            cube.faces().values().forEach(face -> usedTextureIds.add(face.textureId()));
        }
        JsonObject textures = new JsonObject();
        for (String textureId : usedTextureIds) {
            String location = textureLocations.get(textureId);
            if (location == null) throw new IllegalArgumentException("missing resource-pack texture: " + textureId);
            textures.addProperty(textureId, location);
        }
        if (!textures.isEmpty()) model.add("textures", textures);

        JsonArray elements = new JsonArray();
        for (NativeRenderCube cube : cubes) elements.add(cubeJson(asset, cube));
        model.add("elements", elements);
        return model;
    }

    private static JsonObject cubeJson(NativeRenderAsset asset, NativeRenderCube cube) {
        JsonObject out = new JsonObject();
        out.add("from", modelVector(cube.from()));
        out.add("to", modelVector(cube.to()));
        if (!cube.shade()) out.addProperty("shade", false);
        if (cube.lightEmission() != 0) out.addProperty("light_emission", cube.lightEmission());

        JsonObject rotation = rotationJson(asset, cube);
        if (rotation != null) out.add("rotation", rotation);

        JsonObject faces = new JsonObject();
        for (Map.Entry<NativeRenderFaceDirection, NativeRenderFace> entry : cube.faces().entrySet()) {
            NativeRenderFace face = entry.getValue();
            JsonObject faceJson = new JsonObject();
            JsonArray uv = new JsonArray();
            uv.add(face.uv().u1() * 16f / asset.textureWidth());
            uv.add(face.uv().v1() * 16f / asset.textureHeight());
            uv.add(face.uv().u2() * 16f / asset.textureWidth());
            uv.add(face.uv().v2() * 16f / asset.textureHeight());
            faceJson.add("uv", uv);
            if (face.rotation() != 0) faceJson.addProperty("rotation", face.rotation());
            faceJson.addProperty("texture", "#" + face.textureId());
            if (face.cullFace() != null) faceJson.addProperty("cullface", face.cullFace());
            if (face.tintIndex() >= 0) faceJson.addProperty("tintindex", face.tintIndex());
            faces.add(entry.getKey().key(), faceJson);
        }
        out.add("faces", faces);
        return out;
    }

    private static JsonObject rotationJson(NativeRenderAsset asset, NativeRenderCube cube) {
        RenderVec3 rotation = cube.rotation();
        int nonZero = nonZero(rotation.x()) + nonZero(rotation.y()) + nonZero(rotation.z());
        boolean modernRotation = compareVersion(asset.javaBlockVersion(), 1, 21, 11) >= 0;

        if (nonZero == 0 && !cube.rescale()) return null;

        JsonObject out = new JsonObject();
        if (modernRotation && (nonZero > 1 || maxAbs(rotation) > 45f)) {
            out.addProperty("x", rotation.x());
            out.addProperty("y", rotation.y());
            out.addProperty("z", rotation.z());
        } else {
            if (!modernRotation && nonZero > 1) {
                throw new IllegalArgumentException("cube " + cube.id()
                        + " uses multi-axis rotation unsupported by java_block_version "
                        + asset.javaBlockVersion());
            }
            String axis;
            float angle;
            if (rotation.x() != 0f) {
                axis = "x";
                angle = rotation.x();
            } else if (rotation.y() != 0f) {
                axis = "y";
                angle = rotation.y();
            } else if (rotation.z() != 0f) {
                axis = "z";
                angle = rotation.z();
            } else {
                axis = "y";
                angle = 0f;
            }
            if (!modernRotation && Math.abs(angle) > 45f) {
                throw new IllegalArgumentException("cube " + cube.id()
                        + " uses angle " + angle + " unsupported by java_block_version "
                        + asset.javaBlockVersion());
            }
            out.addProperty("angle", angle);
            out.addProperty("axis", axis);
        }
        out.add("origin", modelVector(cube.origin()));
        if (cube.rescale()) out.addProperty("rescale", true);
        return out;
    }

    private static int compareVersion(String version, int major, int minor, int patch) {
        if (version == null || version.isBlank()) return 1;
        String[] parts = version.split("\\.");
        int[] target = {major, minor, patch};
        for (int index = 0; index < target.length; index++) {
            int value;
            try {
                value = index < parts.length ? Integer.parseInt(parts[index]) : 0;
            } catch (NumberFormatException ignored) {
                return 1;
            }
            if (value != target[index]) return value - target[index];
        }
        return 0;
    }

    private static float maxAbs(RenderVec3 value) {
        return Math.max(Math.abs(value.x()), Math.max(Math.abs(value.y()), Math.abs(value.z())));
    }

    private static int nonZero(float value) {
        return value == 0f ? 0 : 1;
    }

    private static JsonArray modelVector(RenderVec3 local) {
        JsonArray array = new JsonArray();
        array.add(local.x() + 8f);
        array.add(local.y() + 8f);
        array.add(local.z() + 8f);
        return array;
    }

    private static void writeJson(Path file, JsonObject json) throws IOException {
        Files.createDirectories(file.getParent());
        GsonHelper.writeJsonFile(json, file);
    }

    public static String itemModelLocation(String assetId, String partId) {
        AssetKey key = AssetKey.parse(assetId);
        return key.namespace() + ":native/" + key.isolatedPath() + "/" + partLeaf(partId);
    }

    private static String partLeaf(String partId) {
        Objects.requireNonNull(partId, "partId");
        if (partId.isBlank()) throw new IllegalArgumentException("partId must not be blank");
        return safeLeaf(partId) + "_" + NativeAssetHashes.sha256(
                partId.getBytes(java.nio.charset.StandardCharsets.UTF_8)).substring(0, 8);
    }

    private static String safeLeaf(String value) {
        String safe = value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_.-]+", "_");
        safe = safe.replaceAll("^_+|_+$", "");
        return safe.isBlank() ? "part" : safe;
    }

    private static void recreateDirectory(Path directory) throws IOException {
        if (Files.exists(directory)) {
            Files.walkFileTree(directory, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    if (exc != null) throw exc;
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
        }
        Files.createDirectories(directory);
    }

    public record Result(String assetId,
                         NativeRenderRepresentation representation,
                         Map<String, String> itemModelsByPart,
                         List<Path> writtenFiles,
                         Path manifestFile) {
        public Result {
            Objects.requireNonNull(assetId, "assetId");
            Objects.requireNonNull(representation, "representation");
            itemModelsByPart = Map.copyOf(itemModelsByPart);
            writtenFiles = List.copyOf(writtenFiles);
            Objects.requireNonNull(manifestFile, "manifestFile");
        }

        public Optional<String> itemModel(RenderUsageContext context, String partId) {
            Objects.requireNonNull(context, "context");
            Objects.requireNonNull(partId, "partId");
            if (!representation.supports(context)) return Optional.empty();
            return Optional.ofNullable(itemModelsByPart.get(partId));
        }
    }

    private record AssetKey(String namespace, String pathString) {
        String isolatedPath() {
            int slash = pathString.lastIndexOf('/');
            String parent = slash < 0 ? "" : pathString.substring(0, slash + 1);
            String leaf = slash < 0 ? pathString : pathString.substring(slash + 1);
            String suffix = NativeAssetHashes.sha256((namespace + ":" + pathString)
                    .getBytes(java.nio.charset.StandardCharsets.UTF_8)).substring(0, 10);
            return parent + leaf + "_" + suffix;
        }

        static AssetKey parse(String assetId) {
            Objects.requireNonNull(assetId, "assetId");
            String namespace = "vsrender";
            String path = assetId;
            int separator = assetId.indexOf(':');
            if (separator >= 0) {
                namespace = assetId.substring(0, separator);
                path = assetId.substring(separator + 1);
            }
            if (!namespace.matches("[a-z0-9_.-]+")) {
                throw new IllegalArgumentException("invalid asset namespace: " + namespace);
            }
            if (!path.matches("[a-z0-9._/-]+") || path.startsWith("/") || path.endsWith("/") || path.contains("//")) {
                throw new IllegalArgumentException("invalid asset path: " + path);
            }
            for (String segment : path.split("/")) {
                if (".".equals(segment) || "..".equals(segment)) throw new IllegalArgumentException("invalid asset path segment: " + segment);
            }
            return new AssetKey(namespace, path);
        }
    }
}
