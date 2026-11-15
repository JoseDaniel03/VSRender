package net.momirealms.craftengine.bukkit.plugin.web;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Strict loopback bridge from authenticated VSRender Web to VSMedia VSCore.
 * No public URL, user-supplied host, CORS changes, automatic imports or
 * Resource Pack publication. The browser only calls VSRender's same-origin
 * authenticated /api/vsassets/... endpoints.
 */
final class VsAssetsBridge {
    private static final Gson JSON = new Gson();
    private static final URI CORE = URI.create("http://127.0.0.1:3061");
    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();
    private static final Pattern ID = Pattern.compile("[a-z0-9_-]{1,48}");
    private static final int JSON_LIMIT = 1024 * 1024;
    private static final int MODEL_LIMIT = 8 * 1024 * 1024;
    private static final int TEXTURE_LIMIT = 500_000;

    private VsAssetsBridge() {}

    static boolean validId(String id) {
        return id != null && ID.matcher(id).matches();
    }

    static boolean validAssetPath(String path) {
        if (path == null || path.length() > 320 || !path.startsWith("assets/")
                || path.indexOf('\\') >= 0 || path.indexOf('\0') >= 0
                || path.indexOf(':') >= 0 || path.startsWith("/")
                || path.endsWith("/")) return false;
        String[] parts = path.split("/", -1);
        if (parts.length < 3 || parts.length > 16) return false;
        for (String part : parts) {
            if (part.isBlank() || part.equals(".") || part.equals("..")
                    || part.length() > 100 || part.contains("?") || part.contains("#")
                    || part.contains("%")) return false;
        }
        return true;
    }

    private static byte[] fetch(String path, int max) {
        if (!path.startsWith("/api/v1/workspace/projects") || path.contains("://")) {
            throw new IllegalArgumentException("VSAssets: ruta remota no permitida");
        }
        try {
            HttpRequest request = HttpRequest.newBuilder(CORE.resolve(path))
                    .GET().timeout(Duration.ofSeconds(12))
                    .header("Accept", "application/json, application/octet-stream")
                    .build();
            HttpResponse<InputStream> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofInputStream());
            try (InputStream in = response.body()) {
                if (response.statusCode() != 200) {
                    throw new IllegalStateException("VSCore devolvio HTTP " + response.statusCode()
                            + ". Abrí VSMedia para comprobar el proyecto.");
                }
                long length = response.headers().firstValueAsLong("Content-Length").orElse(-1L);
                if (length > max) throw new IllegalArgumentException("Archivo de VSAssets demasiado grande");
                byte[] bytes = in.readNBytes(max + 1);
                if (bytes.length > max) throw new IllegalArgumentException("Respuesta de VSAssets supera el limite");
                return bytes;
            }
        } catch (IOException ex) {
            throw new IllegalStateException("No se pudo conectar con VSMedia VSCore en 127.0.0.1:3061", ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("VSAssets: solicitud interrumpida", ex);
        }
    }

    private static String encoded(String value) {
        return java.net.URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    @SuppressWarnings("unchecked")
    static Map<String,Object> list() {
        Object decoded = JSON.fromJson(new String(fetch("/api/v1/workspace/projects",JSON_LIMIT),
                StandardCharsets.UTF_8), Map.class);
        if (!(decoded instanceof Map<?,?> map)) throw new IllegalStateException("VSAssets: catalogo no valido");
        return (Map<String,Object>)map;
    }

    @SuppressWarnings("unchecked")
    static Map<String,Object> project(String id) {
        if (!validId(id)) throw new IllegalArgumentException("ID de VSAssets invalido");
        Object decoded = JSON.fromJson(new String(fetch("/api/v1/workspace/projects/" + id,JSON_LIMIT),
                StandardCharsets.UTF_8), Map.class);
        if (!(decoded instanceof Map<?,?> map)) throw new IllegalStateException("VSAssets: proyecto no valido");
        return (Map<String,Object>)map;
    }

    /**
     * Import only an explicitly selected asset. Check the VSAssets recorded
     * SHA-256 against the returned bytes before writing into VSRender.
     * The metadata is consulted each time; no stale filename trust.
     */
    static byte[] verifiedFile(String projectId, String path, boolean texture) {
        if (!validId(projectId) || !validAssetPath(path)) {
            throw new IllegalArgumentException("Proyecto o ruta VSAssets invalida");
        }
        final int max = texture ? TEXTURE_LIMIT : MODEL_LIMIT;
        Map<String,Object> project = project(projectId);
        Object raw = project.get("files");
        if (!(raw instanceof List<?> files)) throw new IllegalStateException("VSAssets: faltan archivos");
        Map<?,?> matching = null;
        for (Object value : files) {
            if (value instanceof Map<?,?> entry && path.equals(entry.get("path"))) {
                matching = entry;
                break;
            }
        }
        if (matching == null) throw new IllegalArgumentException("El asset ya no figura en el proyecto");
        Object count = matching.get("bytes");
        String expected = Objects.toString(matching.get("sha256"), "");
        if (!(count instanceof Number number) || number.longValue() <= 0 || number.longValue() > max
                || !expected.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException("Asset incompatible o supera el limite de importacion");
        }
        byte[] bytes = fetch("/api/v1/workspace/projects/" + projectId
                + "/file?path=" + encoded(path), max);
        String actual;
        try {
            actual = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (java.security.NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
        if (bytes.length != number.longValue() || !actual.equals(expected)) {
            throw new IllegalStateException("El asset cambio: SHA-256 no coincide. Volve a sincronizar VSAssets.");
        }
        return bytes;
    }
}
