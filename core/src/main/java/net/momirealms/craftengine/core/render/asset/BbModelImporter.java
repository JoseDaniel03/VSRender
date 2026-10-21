package net.momirealms.craftengine.core.render.asset;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import net.momirealms.craftengine.core.render.animation.RenderAnimationClip;
import net.momirealms.craftengine.core.render.animation.RenderAnimationFrame;
import net.momirealms.craftengine.core.render.animation.RenderAnimationMode;
import net.momirealms.craftengine.core.render.animation.RenderAnimationTrack;
import net.momirealms.craftengine.core.render.animation.RenderBonePose;
import net.momirealms.craftengine.core.render.animation.RenderInterpolation;
import net.momirealms.craftengine.core.render.animation.RenderVec3;
import net.momirealms.craftengine.core.render.rig.RenderPart;
import net.momirealms.craftengine.core.render.rig.RenderPartKind;
import net.momirealms.craftengine.core.render.rig.RenderRig;
import net.momirealms.craftengine.core.render.rig.RenderRigType;

import java.io.IOException;
import java.io.Reader;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Direct Blockbench .bbmodel importer for the native VsRender asset model.
 *
 * <p>v1 supports cube/group/locator hierarchy, embedded or explicit texture references and
 * position/rotation/scale animation channels using STEP or LINEAR interpolation. Expressions
 * and nonlinear interpolation fail explicitly instead of silently changing the authored model.</p>
 */
public final class BbModelImporter {
    private static final String BASE64_PNG_PREFIX = "data:image/png;base64,";
    private static final String ROOT_PART_ID = "root";

    public NativeRenderAsset importFile(Path source,
                                        String assetId,
                                        NativeRenderRepresentation representation) throws IOException {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(assetId, "assetId");
        Objects.requireNonNull(representation, "representation");
        JsonObject root;
        try (Reader reader = Files.newBufferedReader(source)) {
            JsonElement parsed = JsonParser.parseReader(reader);
            if (!parsed.isJsonObject()) throw new IllegalArgumentException("bbmodel root must be a JSON object");
            root = parsed.getAsJsonObject();
        } catch (JsonParseException | IllegalStateException e) {
            throw new IllegalArgumentException("invalid bbmodel: " + source, e);
        }

        int textureWidth = 16;
        int textureHeight = 16;
        JsonObject resolution = object(root, "resolution");
        if (resolution != null) {
            textureWidth = positiveInt(resolution, "width", 16);
            textureHeight = positiveInt(resolution, "height", 16);
        }

        TextureTable textureTable = parseTextures(root, source);
        Map<String, JsonObject> elements = parseElements(root);
        Hierarchy hierarchy = parseHierarchy(root, elements, textureTable, textureWidth, textureHeight);
        List<RenderAnimationClip> animations = parseAnimations(root, hierarchy.partIdByUuid());
        Set<String> animationIds = new LinkedHashSet<>();
        for (RenderAnimationClip clip : animations) {
            if (!animationIds.add(clip.animationId())) {
                throw new IllegalArgumentException("duplicate animation id: " + clip.animationId());
            }
        }
        RenderRig rig = new RenderRig(
                NativeRenderAsset.PROVIDER_ID,
                assetId,
                RenderRigType.MODEL,
                hierarchy.parts(),
                animationIds
        );
        return new NativeRenderAsset(
                assetId,
                NativeAssetHashes.sha256(source),
                textureWidth,
                textureHeight,
                string(root, "java_block_version", ""),
                rig,
                animations,
                hierarchy.geometry(),
                textureTable.byId(),
                representation
        );
    }

    private static TextureTable parseTextures(JsonObject root, Path source) throws IOException {
        JsonArray array = array(root, "textures");
        if (array == null) return new TextureTable(List.of(), Map.of(), Map.of());
        ArrayList<String> ids = new ArrayList<>(array.size());
        LinkedHashMap<String, NativeRenderTexture> textures = new LinkedHashMap<>();
        HashMap<String, String> uuidToId = new HashMap<>();
        for (int index = 0; index < array.size(); index++) {
            JsonObject texture = array.get(index).getAsJsonObject();
            String id = "tex_" + index;
            ids.add(id);
            String uuid = string(texture, "uuid", "");
            if (!uuid.isBlank()) uuidToId.put(uuid, id);

            byte[] bytes = textureBytes(texture, source);
            if (bytes != null) {
                textures.put(id, NativeRenderTexture.embedded(id, bytes));
            } else {
                String link = javaTextureLink(texture);
                if (link.isBlank()) {
                    throw new IllegalArgumentException("texture " + index + " has neither embedded data nor resource location");
                }
                textures.put(id, NativeRenderTexture.external(id, link));
            }
        }
        return new TextureTable(List.copyOf(ids), Collections.unmodifiableMap(textures), Map.copyOf(uuidToId));
    }

    private static byte[] textureBytes(JsonObject texture, Path source) throws IOException {
        String embedded = string(texture, "source", "");
        if (embedded.startsWith(BASE64_PNG_PREFIX)) {
            try {
                return Base64.getDecoder().decode(embedded.substring(BASE64_PNG_PREFIX.length()));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("invalid embedded PNG in bbmodel", e);
            }
        }

        for (String candidate : List.of(embedded, string(texture, "path", ""))) {
            if (candidate.isBlank() || candidate.contains(":") && !candidate.startsWith("file:")) continue;
            Path resolved;
            if (candidate.startsWith("file:")) {
                try {
                    resolved = Path.of(URI.create(candidate));
                } catch (IllegalArgumentException e) {
                    continue;
                }
            } else {
                Path parent = source.toAbsolutePath().getParent();
                resolved = (parent == null ? Path.of(candidate) : parent.resolve(candidate)).normalize();
            }
            if (Files.isRegularFile(resolved)) return Files.readAllBytes(resolved);
        }
        return null;
    }

    private static String javaTextureLink(JsonObject texture) {
        String name = string(texture, "name", "").replaceAll("\\.\\w{2,8}$", "");
        String folder = string(texture, "folder", "");
        String namespace = string(texture, "namespace", "");
        String link = folder.isEmpty() ? name : folder + "/" + name;
        if (!namespace.isEmpty() && !"minecraft".equals(namespace)) link = namespace + ":" + link;
        return link;
    }

    private static Map<String, JsonObject> parseElements(JsonObject root) {
        LinkedHashMap<String, JsonObject> result = new LinkedHashMap<>();
        JsonArray array = array(root, "elements");
        if (array == null) return result;
        for (JsonElement value : array) {
            JsonObject element = value.getAsJsonObject();
            if (!"cube".equals(string(element, "type", "cube"))) continue;
            if (element.has("export") && !element.get("export").getAsBoolean()) continue;
            String uuid = string(element, "uuid", "");
            if (uuid.isBlank()) throw new IllegalArgumentException("bbmodel cube is missing uuid");
            if (result.putIfAbsent(uuid, element) != null) throw new IllegalArgumentException("duplicate element uuid: " + uuid);
        }
        return result;
    }

    private static Hierarchy parseHierarchy(JsonObject root,
                                            Map<String, JsonObject> elements,
                                            TextureTable textures,
                                            int textureWidth,
                                            int textureHeight) {
        JsonArray outliner = array(root, "outliner");
        if (outliner == null) outliner = new JsonArray();

        LinkedHashMap<String, JsonObject> partNodes = new LinkedHashMap<>();
        HashMap<String, Integer> nameCounts = new HashMap<>();
        collectPartNodes(outliner, partNodes, nameCounts);

        LinkedHashMap<String, String> partIdByUuid = new LinkedHashMap<>();
        HashSet<String> usedPartIds = new HashSet<>();
        usedPartIds.add(ROOT_PART_ID);
        for (Map.Entry<String, JsonObject> entry : partNodes.entrySet()) {
            String name = sanitizePartId(string(entry.getValue(), "name", "part"));
            String id = name;
            if (nameCounts.getOrDefault(name, 0) > 1 || ROOT_PART_ID.equals(id) || !usedPartIds.add(id)) {
                id = name + "_" + shortUuid(entry.getKey());
                if (!usedPartIds.add(id)) throw new IllegalArgumentException("unable to create unique part id for " + entry.getKey());
            }
            partIdByUuid.put(entry.getKey(), id);
        }

        LinkedHashMap<String, PartDraft> drafts = new LinkedHashMap<>();
        LinkedHashMap<String, List<NativeRenderCube>> geometry = new LinkedHashMap<>();
        HashSet<String> assignedElements = new HashSet<>();
        ArrayList<String> rootChildren = new ArrayList<>();

        for (JsonElement child : outliner) {
            if (child.isJsonObject() && isPartNode(child.getAsJsonObject())) {
                JsonObject node = child.getAsJsonObject();
                String childId = partIdByUuid.get(requireUuid(node));
                rootChildren.add(childId);
                appendPart(node, ROOT_PART_ID, RenderVec3.ZERO, partIdByUuid, drafts, geometry,
                        elements, assignedElements, textures, textureWidth, textureHeight);
            } else {
                appendCubeReference(child, ROOT_PART_ID, RenderVec3.ZERO, geometry, elements, assignedElements,
                        textures, textureWidth, textureHeight);
            }
        }
        for (Map.Entry<String, JsonObject> entry : elements.entrySet()) {
            if (!assignedElements.contains(entry.getKey())) {
                NativeRenderCube cube = convertCube(entry.getValue(), ROOT_PART_ID, RenderVec3.ZERO, textures, textureWidth, textureHeight);
                if (cube != null) geometry.computeIfAbsent(ROOT_PART_ID, ignored -> new ArrayList<>()).add(cube);
            }
        }

        ArrayList<RenderPart> parts = new ArrayList<>();
        parts.add(new RenderPart(ROOT_PART_ID, null, List.copyOf(rootChildren)));
        for (PartDraft draft : drafts.values()) {
            parts.add(new RenderPart(
                    draft.id(),
                    draft.parentId(),
                    draft.children(),
                    draft.kind(),
                    new RenderBonePose(draft.localPositionBlocks(), RenderVec3.ONE, draft.rotation())
            ));
        }
        LinkedHashMap<String, List<NativeRenderCube>> frozenGeometry = new LinkedHashMap<>();
        geometry.forEach((part, cubes) -> frozenGeometry.put(part, List.copyOf(cubes)));
        return new Hierarchy(List.copyOf(parts), Collections.unmodifiableMap(frozenGeometry), Map.copyOf(partIdByUuid));
    }

    private static void collectPartNodes(JsonArray nodes,
                                         Map<String, JsonObject> result,
                                         Map<String, Integer> nameCounts) {
        for (JsonElement element : nodes) {
            if (!element.isJsonObject()) continue;
            JsonObject node = element.getAsJsonObject();
            if (!isPartNode(node)) continue;
            String uuid = requireUuid(node);
            if (result.putIfAbsent(uuid, node) != null) throw new IllegalArgumentException("duplicate part uuid: " + uuid);
            String name = sanitizePartId(string(node, "name", "part"));
            nameCounts.merge(name, 1, Integer::sum);
            JsonArray children = array(node, "children");
            if (children != null) collectPartNodes(children, result, nameCounts);
        }
    }

    private static void appendPart(JsonObject node,
                                   String parentId,
                                   RenderVec3 parentAbsoluteOrigin,
                                   Map<String, String> partIdByUuid,
                                   Map<String, PartDraft> drafts,
                                   Map<String, List<NativeRenderCube>> geometry,
                                   Map<String, JsonObject> elements,
                                   Set<String> assignedElements,
                                   TextureTable textures,
                                   int textureWidth,
                                   int textureHeight) {
        String uuid = requireUuid(node);
        String id = Objects.requireNonNull(partIdByUuid.get(uuid), "missing part id");
        RenderPartKind kind = partKind(node);
        RenderVec3 absoluteOrigin = partOrigin(node);
        RenderVec3 localPosition = absoluteOrigin.subtract(parentAbsoluteOrigin);
        localPosition = new RenderVec3(localPosition.x() / 16f, localPosition.y() / 16f, localPosition.z() / 16f);
        RenderVec3 rotation = vector(node, "rotation", RenderVec3.ZERO);

        ArrayList<String> partChildren = new ArrayList<>();
        JsonArray children = array(node, "children");
        if (children != null) {
            for (JsonElement child : children) {
                if (child.isJsonObject() && isPartNode(child.getAsJsonObject())) {
                    String childId = partIdByUuid.get(requireUuid(child.getAsJsonObject()));
                    partChildren.add(childId);
                }
            }
        }
        drafts.put(id, new PartDraft(id, parentId, List.copyOf(partChildren), kind, localPosition, rotation));

        if (children != null) {
            for (JsonElement child : children) {
                if (child.isJsonObject() && isPartNode(child.getAsJsonObject())) {
                    appendPart(child.getAsJsonObject(), id, absoluteOrigin, partIdByUuid, drafts, geometry,
                            elements, assignedElements, textures, textureWidth, textureHeight);
                } else {
                    appendCubeReference(child, id, absoluteOrigin, geometry, elements, assignedElements,
                            textures, textureWidth, textureHeight);
                }
            }
        }
    }

    private static void appendCubeReference(JsonElement reference,
                                            String partId,
                                            RenderVec3 partOrigin,
                                            Map<String, List<NativeRenderCube>> geometry,
                                            Map<String, JsonObject> elements,
                                            Set<String> assignedElements,
                                            TextureTable textures,
                                            int textureWidth,
                                            int textureHeight) {
        JsonObject cube = null;
        String uuid = null;
        if (reference.isJsonPrimitive()) {
            uuid = reference.getAsString();
            cube = elements.get(uuid);
        } else if (reference.isJsonObject()) {
            JsonObject candidate = reference.getAsJsonObject();
            if ("cube".equals(string(candidate, "type", ""))) {
                uuid = string(candidate, "uuid", "");
                cube = candidate;
            }
        }
        if (cube == null) return;
        if (uuid != null && !uuid.isBlank()) assignedElements.add(uuid);
        NativeRenderCube converted = convertCube(cube, partId, partOrigin, textures, textureWidth, textureHeight);
        if (converted != null) geometry.computeIfAbsent(partId, ignored -> new ArrayList<>()).add(converted);
    }

    private static NativeRenderCube convertCube(JsonObject cube,
                                                String partId,
                                                RenderVec3 partOrigin,
                                                TextureTable textures,
                                                int textureWidth,
                                                int textureHeight) {
        RenderVec3 from = vector(cube, "from", null);
        RenderVec3 to = vector(cube, "to", null);
        if (from == null || to == null) throw new IllegalArgumentException("cube requires from/to");
        float inflate = number(cube, "inflate", 0f);
        if (inflate != 0f) {
            from = new RenderVec3(from.x() - inflate, from.y() - inflate, from.z() - inflate);
            to = new RenderVec3(to.x() + inflate, to.y() + inflate, to.z() + inflate);
        }
        from = from.subtract(partOrigin);
        to = to.subtract(partOrigin);
        RenderVec3 origin = vector(cube, "origin", RenderVec3.ZERO).subtract(partOrigin);
        RenderVec3 rotation = vector(cube, "rotation", RenderVec3.ZERO);

        EnumMap<NativeRenderFaceDirection, NativeRenderFace> faces = new EnumMap<>(NativeRenderFaceDirection.class);
        JsonObject faceObject = object(cube, "faces");
        if (faceObject != null) {
            for (Map.Entry<String, JsonElement> entry : faceObject.entrySet()) {
                NativeRenderFaceDirection direction = NativeRenderFaceDirection.parse(entry.getKey());
                JsonObject face = entry.getValue().getAsJsonObject();
                if (face.has("enabled") && !face.get("enabled").getAsBoolean()) continue;
                if (!face.has("texture") || face.get("texture").isJsonNull()) continue;
                String textureId = textures.resolve(face.get("texture"));
                if (textureId == null) throw new IllegalArgumentException("cube face references unknown texture: " + face.get("texture"));

                JsonArray uv = array(face, "uv");
                NativeRenderUv nativeUv = uv == null
                        ? new NativeRenderUv(0, 0, textureWidth, textureHeight)
                        : new NativeRenderUv(uv.get(0).getAsFloat(), uv.get(1).getAsFloat(), uv.get(2).getAsFloat(), uv.get(3).getAsFloat());
                int faceRotation = integer(face, "rotation", 0);
                String cullFace = string(face, "cullface", "");
                int tintIndex = integer(face, "tint", integer(face, "tintindex", -1));
                faces.put(direction, new NativeRenderFace(textureId, nativeUv, faceRotation, cullFace.isBlank() ? null : cullFace, tintIndex));
            }
        }
        if (faces.isEmpty()) return null;
        String id = string(cube, "uuid", string(cube, "name", "cube"));
        return new NativeRenderCube(
                id,
                partId,
                from,
                to,
                origin,
                rotation,
                !cube.has("shade") || cube.get("shade").getAsBoolean(),
                cube.has("rescale") && cube.get("rescale").getAsBoolean(),
                integer(cube, "light_emission", 0),
                faces
        );
    }

    private static List<RenderAnimationClip> parseAnimations(JsonObject root, Map<String, String> partIdByUuid) {
        JsonArray animations = array(root, "animations");
        if (animations == null) return List.of();
        ArrayList<RenderAnimationClip> result = new ArrayList<>();
        for (int animationIndex = 0; animationIndex < animations.size(); animationIndex++) {
            JsonObject animation = animations.get(animationIndex).getAsJsonObject();
            String name = string(animation, "name", "animation_" + animationIndex);
            RenderAnimationMode mode = animationMode(animation.get("loop"));
            boolean override = animation.has("override") && animation.get("override").getAsBoolean();
            LinkedHashMap<String, RenderAnimationTrack> tracks = new LinkedHashMap<>();
            float maxTime = 0f;

            JsonObject animators = object(animation, "animators");
            if (animators != null) {
                for (Map.Entry<String, JsonElement> animatorEntry : animators.entrySet()) {
                    JsonObject animator = animatorEntry.getValue().getAsJsonObject();
                    String partId = partIdByUuid.get(animatorEntry.getKey());
                    JsonArray keyframes = array(animator, "keyframes");
                    if (keyframes == null || keyframes.isEmpty()) continue;
                    EnumMap<Channel, ChannelCurve> curves = parseCurves(keyframes);
                    if (curves.isEmpty()) continue;
                    if (partId == null) {
                        String type = string(animator, "type", "");
                        if ("bone".equalsIgnoreCase(type) || containsTransformChannels(keyframes)) {
                            throw new IllegalArgumentException("animation references unknown part uuid: " + animatorEntry.getKey());
                        }
                        continue;
                    }

                    TreeSet<Float> times = new TreeSet<>();
                    curves.values().forEach(curve -> times.addAll(curve.times()));
                    ArrayList<RenderAnimationFrame> frames = new ArrayList<>(times.size());
                    for (float time : times) {
                        RenderVec3 position = sample(curves.get(Channel.POSITION), time, RenderVec3.ZERO);
                        RenderVec3 rotation = sample(curves.get(Channel.ROTATION), time, RenderVec3.ZERO);
                        RenderVec3 scale = sample(curves.get(Channel.SCALE), time, RenderVec3.ONE);
                        RenderInterpolation interpolation = segmentInterpolation(curves.values(), time);
                        boolean globalRotation = curves.containsKey(Channel.ROTATION) && curves.get(Channel.ROTATION).globalAt(time);
                        frames.add(new RenderAnimationFrame(time, new RenderBonePose(position, scale, rotation), interpolation, globalRotation));
                        maxTime = Math.max(maxTime, time);
                    }
                    if (!frames.isEmpty()) tracks.put(partId, new RenderAnimationTrack(partId, frames));
                }
            }
            float declaredLength = number(animation, "length", 0f);
            float length = Math.max(declaredLength, maxTime);
            result.add(new RenderAnimationClip(name, length, mode, override, tracks));
        }
        return List.copyOf(result);
    }

    private static EnumMap<Channel, ChannelCurve> parseCurves(JsonArray keyframes) {
        EnumMap<Channel, TreeMap<Float, KeyPoint>> points = new EnumMap<>(Channel.class);
        for (JsonElement element : keyframes) {
            JsonObject keyframe = element.getAsJsonObject();
            Channel channel = Channel.parse(string(keyframe, "channel", ""));
            if (channel == null) continue;
            float time = number(keyframe, "time", 0f);
            if (time < 0f) throw new IllegalArgumentException("animation keyframe time must be >= 0");
            RenderInterpolation interpolation = interpolation(string(keyframe, "interpolation", "linear"));
            RenderVec3 value = keyframeVector(keyframe, channel);
            boolean global = (keyframe.has("global") && keyframe.get("global").getAsBoolean())
                    || (keyframe.has("global_rotation") && keyframe.get("global_rotation").getAsBoolean());
            points.computeIfAbsent(channel, ignored -> new TreeMap<>())
                    .put(time, new KeyPoint(time, value, interpolation, global));
        }
        EnumMap<Channel, ChannelCurve> result = new EnumMap<>(Channel.class);
        points.forEach((channel, values) -> result.put(channel, new ChannelCurve(channel, List.copyOf(values.values()))));
        return result;
    }

    private static RenderVec3 keyframeVector(JsonObject keyframe, Channel channel) {
        JsonArray dataPoints = array(keyframe, "data_points");
        JsonObject value = dataPoints != null && !dataPoints.isEmpty()
                ? dataPoints.get(0).getAsJsonObject()
                : keyframe;
        float x = numericExpression(value.get("x"), channel.defaultValue().x());
        float y = numericExpression(value.get("y"), channel.defaultValue().y());
        float z = numericExpression(value.get("z"), channel.defaultValue().z());
        RenderVec3 vector = new RenderVec3(x, y, z);
        return channel == Channel.POSITION
                ? new RenderVec3(vector.x() / 16f, vector.y() / 16f, vector.z() / 16f)
                : vector;
    }

    private static float numericExpression(JsonElement element, float defaultValue) {
        if (element == null || element.isJsonNull()) return defaultValue;
        try {
            return Float.parseFloat(element.getAsString());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("bbmodel expressions are not supported in native importer v1: " + element, e);
        }
    }

    private static RenderVec3 sample(ChannelCurve curve, float time, RenderVec3 defaultValue) {
        return curve == null ? defaultValue : curve.sample(time);
    }

    private static RenderInterpolation segmentInterpolation(Collection<ChannelCurve> curves, float time) {
        EnumSet<RenderInterpolation> modes = EnumSet.noneOf(RenderInterpolation.class);
        for (ChannelCurve curve : curves) {
            RenderInterpolation mode = curve.segmentInterpolation(time);
            if (mode != null) modes.add(mode);
        }
        if (modes.size() > 1) {
            throw new IllegalArgumentException("mixed STEP/LINEAR interpolation cannot be represented by native track at " + time + "s");
        }
        return modes.isEmpty() ? RenderInterpolation.LINEAR : modes.iterator().next();
    }

    private static boolean containsTransformChannels(JsonArray keyframes) {
        for (JsonElement element : keyframes) {
            if (Channel.parse(string(element.getAsJsonObject(), "channel", "")) != null) return true;
        }
        return false;
    }

    private static RenderAnimationMode animationMode(JsonElement loop) {
        if (loop == null || loop.isJsonNull()) return RenderAnimationMode.ONCE;
        if (loop.isJsonPrimitive() && loop.getAsJsonPrimitive().isBoolean()) {
            return loop.getAsBoolean() ? RenderAnimationMode.LOOP : RenderAnimationMode.ONCE;
        }
        String value = loop.getAsString().toLowerCase(Locale.ROOT);
        return switch (value) {
            case "loop", "true" -> RenderAnimationMode.LOOP;
            case "hold", "hold_on_last", "hold_on_last_frame" -> RenderAnimationMode.HOLD;
            default -> RenderAnimationMode.ONCE;
        };
    }

    private static RenderInterpolation interpolation(String value) {
        return switch (value.toLowerCase(Locale.ROOT)) {
            case "", "linear" -> RenderInterpolation.LINEAR;
            case "step", "constant" -> RenderInterpolation.STEP;
            default -> throw new IllegalArgumentException("unsupported bbmodel interpolation in native importer v1: " + value);
        };
    }

    private static RenderPartKind partKind(JsonObject node) {
        return switch (string(node, "type", "group").toLowerCase(Locale.ROOT)) {
            case "locator" -> RenderPartKind.LOCATOR;
            case "null_object", "null" -> RenderPartKind.NULL_OBJECT;
            case "group" -> RenderPartKind.GROUP;
            default -> RenderPartKind.OTHER;
        };
    }

    private static boolean isPartNode(JsonObject node) {
        String type = string(node, "type", "group").toLowerCase(Locale.ROOT);
        return !"cube".equals(type) && ("group".equals(type) || "locator".equals(type) || "null_object".equals(type) || "null".equals(type));
    }

    private static RenderVec3 partOrigin(JsonObject node) {
        if ("locator".equalsIgnoreCase(string(node, "type", "")) && node.has("position")) {
            return vector(node, "position", RenderVec3.ZERO);
        }
        return vector(node, "origin", RenderVec3.ZERO);
    }

    private static String requireUuid(JsonObject node) {
        String uuid = string(node, "uuid", "");
        if (uuid.isBlank()) throw new IllegalArgumentException("bbmodel part is missing uuid");
        return uuid;
    }

    private static String sanitizePartId(String input) {
        String value = input.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_.-]+", "_");
        value = value.replaceAll("^_+|_+$", "");
        return value.isBlank() ? "part" : value;
    }

    private static String shortUuid(String uuid) {
        String compact = uuid.replaceAll("[^A-Za-z0-9]", "").toLowerCase(Locale.ROOT);
        return compact.length() <= 8 ? compact : compact.substring(0, 8);
    }

    private static RenderVec3 vector(JsonObject object, String key, RenderVec3 fallback) {
        JsonArray values = array(object, key);
        if (values == null) return fallback;
        if (values.size() < 3) throw new IllegalArgumentException(key + " must contain 3 numbers");
        return new RenderVec3(values.get(0).getAsFloat(), values.get(1).getAsFloat(), values.get(2).getAsFloat());
    }

    private static JsonArray array(JsonObject object, String key) {
        JsonElement value = object.get(key);
        return value != null && value.isJsonArray() ? value.getAsJsonArray() : null;
    }

    private static JsonObject object(JsonObject object, String key) {
        JsonElement value = object.get(key);
        return value != null && value.isJsonObject() ? value.getAsJsonObject() : null;
    }

    private static String string(JsonObject object, String key, String fallback) {
        JsonElement value = object.get(key);
        return value != null && value.isJsonPrimitive() ? value.getAsString() : fallback;
    }

    private static float number(JsonObject object, String key, float fallback) {
        JsonElement value = object.get(key);
        return value != null && value.isJsonPrimitive() ? value.getAsFloat() : fallback;
    }

    private static int integer(JsonObject object, String key, int fallback) {
        JsonElement value = object.get(key);
        return value != null && value.isJsonPrimitive() ? value.getAsInt() : fallback;
    }

    private static int positiveInt(JsonObject object, String key, int fallback) {
        int value = integer(object, key, fallback);
        if (value <= 0) throw new IllegalArgumentException(key + " must be positive");
        return value;
    }

    private record TextureTable(List<String> byIndex,
                                Map<String, NativeRenderTexture> byId,
                                Map<String, String> idByUuid) {
        String resolve(JsonElement reference) {
            if (reference == null || reference.isJsonNull()) return null;
            if (reference.isJsonPrimitive() && reference.getAsJsonPrimitive().isNumber()) {
                int index = reference.getAsInt();
                return index >= 0 && index < byIndex.size() ? byIndex.get(index) : null;
            }
            String value = reference.getAsString();
            if (value.startsWith("#")) value = value.substring(1);
            try {
                int index = Integer.parseInt(value);
                return index >= 0 && index < byIndex.size() ? byIndex.get(index) : null;
            } catch (NumberFormatException ignored) {
                String byUuid = idByUuid.get(value);
                if (byUuid != null) return byUuid;
                return byId.containsKey(value) ? value : null;
            }
        }
    }

    private record PartDraft(String id,
                             String parentId,
                             List<String> children,
                             RenderPartKind kind,
                             RenderVec3 localPositionBlocks,
                             RenderVec3 rotation) {
    }

    private record Hierarchy(List<RenderPart> parts,
                             Map<String, List<NativeRenderCube>> geometry,
                             Map<String, String> partIdByUuid) {
    }

    private enum Channel {
        POSITION(RenderVec3.ZERO),
        ROTATION(RenderVec3.ZERO),
        SCALE(RenderVec3.ONE);

        private final RenderVec3 defaultValue;

        Channel(RenderVec3 defaultValue) {
            this.defaultValue = defaultValue;
        }

        RenderVec3 defaultValue() {
            return defaultValue;
        }

        static Channel parse(String value) {
            return switch (value.toLowerCase(Locale.ROOT)) {
                case "position", "translation" -> POSITION;
                case "rotation" -> ROTATION;
                case "scale" -> SCALE;
                default -> null;
            };
        }
    }

    private record KeyPoint(float time,
                            RenderVec3 value,
                            RenderInterpolation interpolation,
                            boolean globalRotation) {
    }

    private static final class ChannelCurve {
        private final Channel channel;
        private final List<KeyPoint> points;

        private ChannelCurve(Channel channel, List<KeyPoint> points) {
            this.channel = channel;
            this.points = points.stream().sorted(Comparator.comparingDouble(KeyPoint::time)).toList();
        }

        List<Float> times() {
            return points.stream().map(KeyPoint::time).toList();
        }

        RenderVec3 sample(float time) {
            if (points.isEmpty()) return channel.defaultValue();
            KeyPoint previous = null;
            KeyPoint next = null;
            for (KeyPoint point : points) {
                if (point.time() <= time) previous = point;
                if (point.time() >= time) {
                    next = point;
                    break;
                }
            }
            if (previous == null) return channel.defaultValue();
            if (next == null || next.time() == previous.time()) return previous.value();
            if (previous.interpolation() == RenderInterpolation.STEP) return previous.value();
            float alpha = (time - previous.time()) / (next.time() - previous.time());
            return previous.value().lerp(next.value(), alpha);
        }

        RenderInterpolation segmentInterpolation(float time) {
            KeyPoint previous = null;
            for (KeyPoint point : points) {
                if (point.time() <= time) previous = point;
                if (point.time() > time) return previous == null ? RenderInterpolation.LINEAR : previous.interpolation();
            }
            return null;
        }

        boolean globalAt(float time) {
            KeyPoint previous = null;
            for (KeyPoint point : points) {
                if (point.time() <= time) previous = point;
                else break;
            }
            return previous != null && previous.globalRotation();
        }
    }
}
