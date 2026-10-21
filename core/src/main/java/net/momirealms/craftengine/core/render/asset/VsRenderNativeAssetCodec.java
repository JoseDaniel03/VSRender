package net.momirealms.craftengine.core.render.asset;

import net.momirealms.craftengine.core.render.animation.RenderAnimationClip;
import net.momirealms.craftengine.core.render.animation.RenderAnimationFrame;
import net.momirealms.craftengine.core.render.animation.RenderAnimationMode;
import net.momirealms.craftengine.core.render.animation.RenderAnimationTrack;
import net.momirealms.craftengine.core.render.animation.RenderBonePose;
import net.momirealms.craftengine.core.render.animation.RenderInterpolation;
import net.momirealms.craftengine.core.render.animation.RenderVec3;
import net.momirealms.craftengine.core.render.rig.RenderHitBox;
import net.momirealms.craftengine.core.render.rig.RenderMountPolicy;
import net.momirealms.craftengine.core.render.rig.RenderNameTagMode;
import net.momirealms.craftengine.core.render.rig.RenderPart;
import net.momirealms.craftengine.core.render.rig.RenderPartKind;
import net.momirealms.craftengine.core.render.rig.RenderPartRotationPolicy;
import net.momirealms.craftengine.core.render.rig.RenderRig;
import net.momirealms.craftengine.core.render.rig.RenderRigType;
import net.momirealms.craftengine.core.util.GsonHelper;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Versioned on-disk codec for canonical native VsRender assets. */
public final class VsRenderNativeAssetCodec {
    public static final String MANIFEST_FILE = "manifest.json";

    public void write(Path directory, NativeRenderAsset asset) throws IOException {
        Objects.requireNonNull(directory, "directory");
        Objects.requireNonNull(asset, "asset");
        recreateDirectory(directory);
        Path textureDirectory = directory.resolve("textures");
        LinkedHashMap<String, String> embeddedFiles = new LinkedHashMap<>();
        for (NativeRenderTexture texture : asset.textures().values()) {
            if (!texture.embedded()) continue;
            Files.createDirectories(textureDirectory);
            String file = safeFileName(texture.id()) + ".png";
            Path target = textureDirectory.resolve(file);
            Files.write(target, Objects.requireNonNull(texture.png(), "embedded texture bytes"));
            embeddedFiles.put(texture.id(), "textures/" + file);
        }
        ManifestDto manifest = toDto(asset, embeddedFiles);
        GsonHelper.writeJsonFile(GsonHelper.get().toJsonTree(manifest), directory.resolve(MANIFEST_FILE));
    }

    public NativeRenderAsset read(Path directory) throws IOException {
        Objects.requireNonNull(directory, "directory");
        Path manifestPath = directory.resolve(MANIFEST_FILE);
        ManifestDto manifest;
        try (Reader reader = Files.newBufferedReader(manifestPath)) {
            manifest = GsonHelper.get().fromJson(reader, ManifestDto.class);
        }
        if (manifest == null) throw new IOException("empty native asset manifest: " + manifestPath);
        if (manifest.schemaVersion() != NativeRenderAsset.SCHEMA_VERSION) {
            throw new IOException("unsupported native asset schema " + manifest.schemaVersion()
                    + ", expected " + NativeRenderAsset.SCHEMA_VERSION);
        }

        LinkedHashMap<String, NativeRenderTexture> textures = new LinkedHashMap<>();
        for (TextureDto texture : required(manifest.textures(), "textures")) {
            byte[] png = null;
            if (texture.embeddedFile() != null) {
                Path texturePath = directory.resolve(texture.embeddedFile()).normalize();
                if (!texturePath.startsWith(directory.normalize())) {
                    throw new IOException("texture escapes native asset directory: " + texture.embeddedFile());
                }
                png = Files.readAllBytes(texturePath);
            }
            NativeRenderTexture nativeTexture = new NativeRenderTexture(texture.id(), texture.resourceLocation(), png);
            textures.put(nativeTexture.id(), nativeTexture);
        }

        ArrayList<RenderPart> parts = new ArrayList<>();
        for (PartDto part : required(manifest.rig().parts(), "rig.parts")) {
            parts.add(new RenderPart(
                    part.id(),
                    part.parentId(),
                    required(part.children(), "part.children"),
                    RenderPartKind.valueOf(part.kind()),
                    pose(part.restPose()),
                    RenderPartRotationPolicy.valueOf(part.rotationPolicy()),
                    hitBox(part.hitBox()),
                    vec(part.anchorOffset()),
                    RenderNameTagMode.valueOf(part.nameTagMode()),
                    mount(part.mountPolicy())
            ));
        }

        ArrayList<RenderAnimationClip> clips = new ArrayList<>();
        for (AnimationDto animation : required(manifest.animations(), "animations")) {
            LinkedHashMap<String, RenderAnimationTrack> tracks = new LinkedHashMap<>();
            for (TrackDto track : required(animation.tracks(), "animation.tracks")) {
                ArrayList<RenderAnimationFrame> frames = new ArrayList<>();
                for (FrameDto frame : required(track.frames(), "track.frames")) {
                    frames.add(new RenderAnimationFrame(
                            frame.timeSeconds(),
                            pose(frame.pose()),
                            RenderInterpolation.valueOf(frame.interpolation()),
                            frame.globalRotation()
                    ));
                }
                tracks.put(track.partId(), new RenderAnimationTrack(track.partId(), frames));
            }
            clips.add(new RenderAnimationClip(
                    animation.animationId(),
                    animation.lengthSeconds(),
                    RenderAnimationMode.valueOf(animation.defaultMode()),
                    animation.override(),
                    tracks
            ));
        }

        LinkedHashMap<String, List<NativeRenderCube>> geometry = new LinkedHashMap<>();
        for (Map.Entry<String, List<CubeDto>> entry : required(manifest.geometry(), "geometry").entrySet()) {
            ArrayList<NativeRenderCube> cubes = new ArrayList<>();
            for (CubeDto cube : required(entry.getValue(), "geometry cubes")) {
                EnumMap<NativeRenderFaceDirection, NativeRenderFace> faces = new EnumMap<>(NativeRenderFaceDirection.class);
                for (FaceDto face : required(cube.faces(), "cube.faces")) {
                    faces.put(
                            NativeRenderFaceDirection.valueOf(face.direction()),
                            new NativeRenderFace(
                                    face.textureId(),
                                    new NativeRenderUv(face.u1(), face.v1(), face.u2(), face.v2()),
                                    face.rotation(),
                                    face.cullFace(),
                                    face.tintIndex()
                            )
                    );
                }
                cubes.add(new NativeRenderCube(
                        cube.id(),
                        entry.getKey(),
                        vec(cube.from()),
                        vec(cube.to()),
                        vec(cube.origin()),
                        vec(cube.rotation()),
                        cube.shade(),
                        cube.rescale(),
                        cube.lightEmission(),
                        faces
                ));
            }
            geometry.put(entry.getKey(), List.copyOf(cubes));
        }

        EnumSet<RenderUsageContext> contexts = EnumSet.noneOf(RenderUsageContext.class);
        for (String context : required(manifest.representation().contexts(), "representation.contexts")) {
            contexts.add(RenderUsageContext.valueOf(context));
        }
        NativeRenderRepresentation representation = new NativeRenderRepresentation(
                contexts,
                manifest.representation().carrierItem()
        );

        RenderRig rig = new RenderRig(
                NativeRenderAsset.PROVIDER_ID,
                manifest.rig().rigId(),
                RenderRigType.valueOf(manifest.rig().type()),
                parts,
                clips.stream().map(RenderAnimationClip::animationId).toList()
        );
        return new NativeRenderAsset(
                manifest.assetId(),
                manifest.sourceSha256(),
                manifest.textureWidth(),
                manifest.textureHeight(),
                manifest.javaBlockVersion() == null ? "" : manifest.javaBlockVersion(),
                rig,
                clips,
                geometry,
                textures,
                representation
        );
    }

    private static ManifestDto toDto(NativeRenderAsset asset, Map<String, String> embeddedFiles) {
        List<TextureDto> textures = asset.textures().values().stream()
                .map(texture -> new TextureDto(texture.id(), texture.resourceLocation(), embeddedFiles.get(texture.id())))
                .toList();
        List<PartDto> parts = asset.rig().parts().stream().map(part -> new PartDto(
                part.id(),
                part.parentId(),
                part.children(),
                part.kind().name(),
                pose(part.restPose()),
                part.rotationPolicy().name(),
                hitBox(part.hitBox()),
                vec(part.anchorOffset()),
                part.nameTagMode().name(),
                mount(part.mountPolicy())
        )).toList();

        List<AnimationDto> animations = asset.animations().stream().map(animation -> new AnimationDto(
                animation.animationId(),
                animation.lengthSeconds(),
                animation.defaultMode().name(),
                animation.override(),
                animation.tracks().values().stream().map(track -> new TrackDto(
                        track.partId(),
                        track.frames().stream().map(frame -> new FrameDto(
                                frame.timeSeconds(),
                                pose(frame.pose()),
                                frame.interpolation().name(),
                                frame.globalRotation()
                        )).toList()
                )).toList()
        )).toList();

        LinkedHashMap<String, List<CubeDto>> geometry = new LinkedHashMap<>();
        asset.geometry().forEach((partId, cubes) -> geometry.put(partId, cubes.stream().map(cube -> new CubeDto(
                cube.id(),
                vec(cube.from()),
                vec(cube.to()),
                vec(cube.origin()),
                vec(cube.rotation()),
                cube.shade(),
                cube.rescale(),
                cube.lightEmission(),
                cube.faces().entrySet().stream().map(face -> new FaceDto(
                        face.getKey().name(),
                        face.getValue().textureId(),
                        face.getValue().uv().u1(),
                        face.getValue().uv().v1(),
                        face.getValue().uv().u2(),
                        face.getValue().uv().v2(),
                        face.getValue().rotation(),
                        face.getValue().cullFace(),
                        face.getValue().tintIndex()
                )).toList()
        )).toList()));

        return new ManifestDto(
                NativeRenderAsset.SCHEMA_VERSION,
                asset.assetId(),
                asset.sourceSha256(),
                asset.textureWidth(),
                asset.textureHeight(),
                asset.javaBlockVersion(),
                new RepresentationDto(
                        asset.representation().contexts().stream().map(Enum::name).sorted().toList(),
                        asset.representation().carrierItem()
                ),
                new RigDto(asset.rig().rigId(), asset.rig().type().name(), parts),
                animations,
                geometry,
                textures
        );
    }

    private static PoseDto pose(RenderBonePose pose) {
        return new PoseDto(vec(pose.translation()), vec(pose.scale()), vec(pose.rotation()));
    }

    private static RenderBonePose pose(PoseDto pose) {
        return new RenderBonePose(vec(pose.translation()), vec(pose.scale()), vec(pose.rotation()));
    }

    private static VecDto vec(RenderVec3 vector) {
        return new VecDto(vector.x(), vector.y(), vector.z());
    }

    private static RenderVec3 vec(VecDto vector) {
        Objects.requireNonNull(vector, "vector");
        return new RenderVec3(vector.x(), vector.y(), vector.z());
    }

    private static HitBoxDto hitBox(RenderHitBox hitBox) {
        if (hitBox == null) return null;
        return new HitBoxDto(
                vec(hitBox.centerOffset()),
                hitBox.minX(), hitBox.minY(), hitBox.minZ(),
                hitBox.maxX(), hitBox.maxY(), hitBox.maxZ()
        );
    }

    private static RenderHitBox hitBox(HitBoxDto hitBox) {
        if (hitBox == null) return null;
        return new RenderHitBox(
                vec(hitBox.centerOffset()),
                hitBox.minX(), hitBox.minY(), hitBox.minZ(),
                hitBox.maxX(), hitBox.maxY(), hitBox.maxZ()
        );
    }

    private static MountDto mount(RenderMountPolicy policy) {
        return new MountDto(
                policy.mode().name(),
                policy.canDismountBySelf(),
                policy.canJump(),
                policy.canBeDamagedByRider()
        );
    }

    private static RenderMountPolicy mount(MountDto policy) {
        Objects.requireNonNull(policy, "mountPolicy");
        return new RenderMountPolicy(
                RenderMountPolicy.Mode.valueOf(policy.mode()),
                policy.canDismountBySelf(),
                policy.canJump(),
                policy.canBeDamagedByRider()
        );
    }

    private static String safeFileName(String value) {
        String safe = value.toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9_.-]+", "_");
        if (safe.isBlank()) safe = "texture";
        return safe + "_" + NativeAssetHashes.sha256(value.getBytes(StandardCharsets.UTF_8)).substring(0, 8);
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

    private static <T> T required(T value, String name) throws IOException {
        if (value == null) throw new IOException("native asset manifest missing " + name);
        return value;
    }

    private record ManifestDto(int schemaVersion,
                               String assetId,
                               String sourceSha256,
                               int textureWidth,
                               int textureHeight,
                               String javaBlockVersion,
                               RepresentationDto representation,
                               RigDto rig,
                               List<AnimationDto> animations,
                               Map<String, List<CubeDto>> geometry,
                               List<TextureDto> textures) {
    }

    private record RepresentationDto(List<String> contexts, String carrierItem) {
    }

    private record RigDto(String rigId, String type, List<PartDto> parts) {
    }

    private record PartDto(String id,
                           String parentId,
                           List<String> children,
                           String kind,
                           PoseDto restPose,
                           String rotationPolicy,
                           HitBoxDto hitBox,
                           VecDto anchorOffset,
                           String nameTagMode,
                           MountDto mountPolicy) {
    }

    private record PoseDto(VecDto translation, VecDto scale, VecDto rotation) {
    }

    private record VecDto(float x, float y, float z) {
    }

    private record HitBoxDto(VecDto centerOffset,
                             float minX, float minY, float minZ,
                             float maxX, float maxY, float maxZ) {
    }

    private record MountDto(String mode,
                            boolean canDismountBySelf,
                            boolean canJump,
                            boolean canBeDamagedByRider) {
    }

    private record AnimationDto(String animationId,
                                float lengthSeconds,
                                String defaultMode,
                                boolean override,
                                List<TrackDto> tracks) {
    }

    private record TrackDto(String partId, List<FrameDto> frames) {
    }

    private record FrameDto(float timeSeconds,
                            PoseDto pose,
                            String interpolation,
                            boolean globalRotation) {
    }

    private record CubeDto(String id,
                           VecDto from,
                           VecDto to,
                           VecDto origin,
                           VecDto rotation,
                           boolean shade,
                           boolean rescale,
                           int lightEmission,
                           List<FaceDto> faces) {
    }

    private record FaceDto(String direction,
                           String textureId,
                           float u1, float v1, float u2, float v2,
                           int rotation,
                           String cullFace,
                           int tintIndex) {
    }

    private record TextureDto(String id, String resourceLocation, String embeddedFile) {
    }
}
