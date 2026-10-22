package net.momirealms.craftengine.core.render.asset;

import net.momirealms.craftengine.core.pack.Pack;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Loads the reserved {@code blueprint/native} subtree of VsRender packs.
 *
 * <p>Compilation is staged outside the pack and installed only after every source in that
 * pack/subpack has succeeded. The existing live registry is replaced only after all packs
 * complete, preventing half-loaded native content on a failed reload.</p>
 */
public final class NativeRenderPackLoader {
    public static final String NATIVE_BLUEPRINT_FOLDER = "native";

    private final Path cacheRoot;
    private final NativeRenderAssetRegistry registry;
    private final VsRenderNativeAssetCache cache;
    private final NativeResourcePackCompiler compiler;

    public NativeRenderPackLoader(Path cacheRoot, NativeRenderAssetRegistry registry) {
        this(cacheRoot, registry, new VsRenderNativeAssetCache(cacheRoot.resolve("assets")), new NativeResourcePackCompiler());
    }

    NativeRenderPackLoader(Path cacheRoot,
                           NativeRenderAssetRegistry registry,
                           VsRenderNativeAssetCache cache,
                           NativeResourcePackCompiler compiler) {
        this.cacheRoot = Objects.requireNonNull(cacheRoot, "cacheRoot");
        this.registry = Objects.requireNonNull(registry, "registry");
        this.cache = Objects.requireNonNull(cache, "cache");
        this.compiler = Objects.requireNonNull(compiler, "compiler");
    }

    public LoadResult load(Collection<Pack> packs) throws IOException {
        Objects.requireNonNull(packs, "packs");
        LinkedHashMap<String, NativeRenderAsset> stagedRegistry = new LinkedHashMap<>();
        ArrayList<LoadedAsset> loadedAssets = new ArrayList<>();
        ArrayList<InstallPlan> installs = new ArrayList<>();
        int cacheHits = 0;

        try {
            for (Pack pack : packs) {
                Objects.requireNonNull(pack, "pack");
                if (!pack.enabled()) continue;
                Path[] blueprintRoots = pack.blueprintFolders();
                Path[] resourceRoots = pack.resourcePackFolders();
                if (blueprintRoots.length != resourceRoots.length) {
                    throw new IOException("pack blueprint/resource folder count mismatch: " + pack.name());
                }

                for (int index = 0; index < blueprintRoots.length; index++) {
                    Path sourceRoot = blueprintRoots[index].resolve(NATIVE_BLUEPRINT_FOLDER);
                    Path resourceRoot = resourceRoots[index];
                    Path stagingRoot = createStagingDirectory(pack.name(), index);
                    installs.add(new InstallPlan(stagingRoot, resourceRoot, pack.namespace()));
                    List<Path> sources = sourceFiles(sourceRoot);
                    for (Path source : sources) {
                        String assetId = assetId(pack.namespace(), sourceRoot, source);
                        var cached = cache.loadOrImport(
                                source,
                                assetId,
                                NativeRenderRepresentation.allContexts()
                        );
                        if (cached.cacheHit()) cacheHits++;
                        NativeRenderAsset asset = cached.asset();
                        if (stagedRegistry.put(asset.assetId(), asset) != null) {
                            throw new IOException("duplicate native asset id across packs/subpacks: " + asset.assetId());
                        }
                        NativeResourcePackCompiler.Result packResult = compiler.compile(asset, stagingRoot);
                        loadedAssets.add(new LoadedAsset(
                                asset.assetId(),
                                source,
                                cached.directory(),
                                cached.cacheHit(),
                                packResult.itemModelsByPart()
                        ));
                    }
                }
            }

            for (InstallPlan install : installs) {
                installReservedNativeTrees(install.stagingRoot(), install.resourceRoot(), install.namespace());
            }
            registry.clear();
            stagedRegistry.values().forEach(registry::register);
            return new LoadResult(List.copyOf(loadedAssets), cacheHits);
        } catch (IOException | RuntimeException error) {
            throw new IOException("failed to load native VsRender content", error);
        } finally {
            for (InstallPlan install : installs) {
                try {
                    deleteTree(install.stagingRoot());
                } catch (IOException ignored) {
                    // Staging cleanup must never replace the real import/compile result.
                }
            }
        }
    }

    private Path createStagingDirectory(String packName, int index) throws IOException {
        Path staging = cacheRoot.resolve("staging");
        Files.createDirectories(staging);
        String prefix = sanitize(packName) + "-" + index + "-";
        return Files.createTempDirectory(staging, prefix);
    }

    private static List<Path> sourceFiles(Path sourceRoot) throws IOException {
        if (!Files.isDirectory(sourceRoot)) return List.of();
        try (var stream = Files.walk(sourceRoot)) {
            return stream.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".bbmodel"))
                    .sorted(Comparator.comparing(path -> sourceRoot.relativize(path).toString()))
                    .toList();
        }
    }

    private static String assetId(String namespace, Path sourceRoot, Path source) throws IOException {
        String relative = sourceRoot.relativize(source).toString().replace('\\', '/');
        if (!relative.endsWith(".bbmodel")) throw new IOException("native blueprint must end in .bbmodel: " + source);
        relative = relative.substring(0, relative.length() - ".bbmodel".length());
        String id = namespace + ":" + relative;
        if (!namespace.matches("[a-z0-9_.-]+") || !relative.matches("[a-z0-9._/-]+")
                || relative.startsWith("/") || relative.endsWith("/") || relative.contains("//")) {
            throw new IOException("native asset id must be a lowercase Minecraft resource location: " + id);
        }
        for (String segment : relative.split("/")) {
            if (".".equals(segment) || "..".equals(segment)) {
                throw new IOException("invalid native asset path segment in " + id);
            }
        }
        return id;
    }

    private static void installReservedNativeTrees(Path stagingRoot,
                                                   Path resourceRoot,
                                                   String namespace) throws IOException {
        for (String category : List.of("models", "items", "textures", "vsrender")) {
            Path target = resourceRoot.resolve("assets").resolve(namespace).resolve(category).resolve("native");
            deleteTree(target);
            Path source = stagingRoot.resolve("assets").resolve(namespace).resolve(category).resolve("native");
            if (Files.isDirectory(source)) copyTree(source, target);
        }
    }

    private static void copyTree(Path source, Path target) throws IOException {
        Files.walkFileTree(source, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                Files.createDirectories(target.resolve(source.relativize(dir).toString()));
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Path destination = target.resolve(source.relativize(file).toString());
                Files.createDirectories(destination.getParent());
                Files.copy(file, destination, StandardCopyOption.REPLACE_EXISTING);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private static void deleteTree(Path root) throws IOException {
        if (root == null || !Files.exists(root)) return;
        Files.walkFileTree(root, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.deleteIfExists(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                if (exc != null) throw exc;
                Files.deleteIfExists(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private static String sanitize(String value) {
        String safe = value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_.-]+", "_");
        return safe.isBlank() ? "pack" : safe;
    }

    private record InstallPlan(Path stagingRoot, Path resourceRoot, String namespace) {
    }

    public record LoadedAsset(String assetId,
                              Path source,
                              Path cacheDirectory,
                              boolean cacheHit,
                              Map<String, String> itemModelsByPart) {
        public LoadedAsset {
            Objects.requireNonNull(assetId, "assetId");
            Objects.requireNonNull(source, "source");
            Objects.requireNonNull(cacheDirectory, "cacheDirectory");
            itemModelsByPart = Map.copyOf(itemModelsByPart);
        }
    }

    public record LoadResult(List<LoadedAsset> assets, int cacheHits) {
        public LoadResult {
            assets = List.copyOf(assets);
            if (cacheHits < 0 || cacheHits > assets.size()) {
                throw new IllegalArgumentException("cacheHits out of range");
            }
        }

        public int loadedCount() {
            return assets.size();
        }
    }
}
