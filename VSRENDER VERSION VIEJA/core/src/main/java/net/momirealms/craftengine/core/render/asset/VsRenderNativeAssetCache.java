package net.momirealms.craftengine.core.render.asset;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Objects;

/**
 * Content-addressed native asset cache.
 *
 * <p>Layout: {@code <root>/v<schema>/<asset-key>/<source-sha256>/manifest.json}.</p>
 */
public final class VsRenderNativeAssetCache {
    private final Path root;
    private final BbModelImporter importer;
    private final VsRenderNativeAssetCodec codec;

    public VsRenderNativeAssetCache(Path root) {
        this(root, new BbModelImporter(), new VsRenderNativeAssetCodec());
    }

    public VsRenderNativeAssetCache(Path root, BbModelImporter importer, VsRenderNativeAssetCodec codec) {
        this.root = Objects.requireNonNull(root, "root");
        this.importer = Objects.requireNonNull(importer, "importer");
        this.codec = Objects.requireNonNull(codec, "codec");
    }

    public CacheResult loadOrImport(Path source,
                                    String assetId,
                                    NativeRenderRepresentation representation) throws IOException {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(assetId, "assetId");
        Objects.requireNonNull(representation, "representation");
        String sourceSha256 = NativeAssetHashes.sha256(source);
        Path directory = cacheDirectory(assetId, sourceSha256);
        Path manifest = directory.resolve(VsRenderNativeAssetCodec.MANIFEST_FILE);
        if (Files.isRegularFile(manifest)) {
            try {
                NativeRenderAsset cached = codec.read(directory);
                if (cached.assetId().equals(assetId)
                        && cached.sourceSha256().equals(sourceSha256)
                        && cached.representation().equals(representation)) {
                    return new CacheResult(cached, directory, true);
                }
            } catch (IOException | RuntimeException ignored) {
                // Corrupt/old cache is rebuilt from the canonical source below.
            }
        }

        NativeRenderAsset imported = importer.importFile(source, assetId, representation);
        codec.write(directory, imported);
        return new CacheResult(imported, directory, false);
    }

    public Path cacheDirectory(String assetId, String sourceSha256) {
        Objects.requireNonNull(assetId, "assetId");
        Objects.requireNonNull(sourceSha256, "sourceSha256");
        String readable = assetId.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_.-]+", "_");
        if (readable.isBlank()) readable = "asset";
        String idHash = NativeAssetHashes.sha256(assetId.getBytes(StandardCharsets.UTF_8)).substring(0, 10);
        return root
                .resolve("v" + NativeRenderAsset.SCHEMA_VERSION)
                .resolve(readable + "_" + idHash)
                .resolve(sourceSha256);
    }

    public record CacheResult(NativeRenderAsset asset, Path directory, boolean cacheHit) {
        public CacheResult {
            Objects.requireNonNull(asset, "asset");
            Objects.requireNonNull(directory, "directory");
        }
    }
}
