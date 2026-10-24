package net.momirealms.craftengine.bukkit.plugin.web;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import net.momirealms.craftengine.bukkit.plugin.BukkitCraftEngine;
import org.bukkit.Material;
import org.snakeyaml.engine.v2.api.Dump;
import org.snakeyaml.engine.v2.api.DumpSettings;
import org.snakeyaml.engine.v2.api.Load;
import org.snakeyaml.engine.v2.api.LoadSettings;
import org.snakeyaml.engine.v2.common.FlowStyle;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * VSRender's local content editor. No separately installed Python service, no
 * public HTTP binding, no demo data, and no additional item/recipe engine.
 * Files remain the canonical resource-pack configuration and the existing
 * /vsr reload workflow applies them to Minecraft.
 */
public final class ContentWebServer implements AutoCloseable {
    private static final Gson JSON = new GsonBuilder().disableHtmlEscaping().create();
    private static final Pattern PART = Pattern.compile("[a-z0-9][a-z0-9_.-]{0,63}");
    private static final Pattern ITEM_ID = Pattern.compile("[a-z0-9_.-]+:[a-z0-9_./-]+");
    private static final int LIMIT = 1024 * 1024;
    private static final int PORT = 3050;
    private static final String ORIGIN = "http://127.0.0.1:" + PORT;
    private final BukkitCraftEngine plugin;
    private final Path resources;
    private final Path backups;
    private HttpServer server;
    private ExecutorService workers;
    private String token;

    public ContentWebServer(BukkitCraftEngine plugin) {
        this.plugin = plugin;
        this.resources = plugin.dataFolderPath().resolve("resources").toAbsolutePath().normalize();
        this.backups = plugin.dataFolderPath().resolve(".vsrender-web-backups").toAbsolutePath().normalize();
    }

    public synchronized String start() throws IOException {
        if (server != null) return link();
        Files.createDirectories(resources);
        byte[] random = new byte[32];
        new SecureRandom().nextBytes(random);
        token = Base64.getUrlEncoder().withoutPadding().encodeToString(random);
        HttpServer candidate = HttpServer.create(
                new InetSocketAddress(InetAddress.getByName("127.0.0.1"), PORT), 32);
        try {
            workers = Executors.newFixedThreadPool(3, r -> {
                Thread t = new Thread(r, "VSRender-Web");
                t.setDaemon(true);
                return t;
            });
            candidate.createContext("/", this::handle);
            candidate.setExecutor(workers);
            candidate.start();
            server = candidate;
            return link();
        } catch (RuntimeException exception) {
            candidate.stop(0);
            workers.shutdownNow();
            workers = null;
            token = null;
            throw exception;
        }
    }

    public synchronized void stop() {
        if (server != null) {
            server.stop(0);
            server = null;
        }
        if (workers != null) {
            workers.shutdownNow();
            workers = null;
        }
        token = null;
    }

    public synchronized boolean running() { return server != null; }

    public synchronized String link() {
        return server == null ? "Web apagada" : ORIGIN + "/?token=" + token;
    }

    @Override public void close() { stop(); }

    private void handle(HttpExchange ex) throws IOException {
        try {
            String method = ex.getRequestMethod();
            String path = ex.getRequestURI().getPath();
            String host = ex.getRequestHeaders().getFirst("Host");
            if (host == null || !(host.equals("127.0.0.1:" + PORT) || host.equals("localhost:" + PORT))) {
                fail(403, "Host no autorizado");
            }
            if (method.equals("GET") && path.equals("/") && Objects.equals(query(ex).get("token"), token)) {
                ex.getResponseHeaders().set("Set-Cookie",
                        "vsrender_auth=" + token + "; Path=/; HttpOnly; SameSite=Strict");
                ex.getResponseHeaders().set("Location", "/");
                send(ex, 303, "text/plain; charset=utf-8", new byte[0]);
                return;
            }
            String cookie = ex.getRequestHeaders().getFirst("Cookie");
            if (token == null || cookie == null || Arrays.stream(cookie.split(";"))
                    .map(String::trim).noneMatch(x -> x.equals("vsrender_auth=" + token))) {
                fail(403, "Acceso denegado. Abri la URL entregada por /vsr web start.");
            }
            if (!method.equals("GET")) {
                String origin = ex.getRequestHeaders().getFirst("Origin");
                if (!"1".equals(ex.getRequestHeaders().getFirst("X-VSRender-Editor"))
                        || (origin != null && !origin.equals(ORIGIN) && !origin.equals("http://localhost:" + PORT))) {
                    fail(403, "Origen o cabecera de edicion no autorizados");
                }
            }
            if (method.equals("GET") && (path.equals("/") || path.equals("/index.html")
                    || path.equals("/styles.css") || path.equals("/app.js"))) {
                String asset = path.equals("/") ? "index.html" : path.substring(1);
                byte[] content = resource("vsrender-web/" + asset);
                send(ex, 200, asset.endsWith(".css") ? "text/css; charset=utf-8"
                        : asset.endsWith(".js") ? "application/javascript; charset=utf-8"
                        : "text/html; charset=utf-8", content);
                return;
            }
            Map<String,String> args = query(ex);
            Object response;
            int code = 200;
            String mime = "application/json; charset=utf-8";
            byte[] bytes = null;
            switch (method + " " + path) {
                case "GET /api/catalog" -> response = catalog();
                case "GET /api/materials" -> response = materials();
                case "GET /api/item" -> response = item(required(args, "ref"));
                case "GET /api/docs" -> response = docs(args.get("slug"));
                case "GET /api/templates" -> response = templates();
                case "GET /api/recipes" -> response = recipeCatalog();
                case "GET /api/guis" -> response = guiCatalog();
                case "GET /api/gui" -> response = gui(required(args, "ref"));
                case "POST /api/gui" -> response = saveGui(jsonBody(ex), null);
                case "PUT /api/gui" -> response = saveGui(jsonBody(ex), required(args, "ref"));
                case "DELETE /api/gui" -> response = deleteGui(required(args, "ref"), jsonBody(ex));
                case "GET /api/recipe" -> response = recipe(required(args, "ref"));
                case "GET /api/recipe/export" -> {
                    Entry e = findRecipe(required(args, "ref"));
                    bytes = dump(Map.of("recipes", Map.of(e.key(), e.definition())))
                            .getBytes(StandardCharsets.UTF_8);
                    mime = "application/x-yaml; charset=utf-8";
                    ex.getResponseHeaders().set("Content-Disposition",
                            "attachment; filename=\"vsrender-recipe-" + localId(e.key()) + ".yml\"");
                    response = null;
                }
                case "POST /api/recipe" -> response = saveRecipe(jsonBody(ex), null);
                case "PUT /api/recipe" -> response = saveRecipe(jsonBody(ex), required(args, "ref"));
                case "DELETE /api/recipe" -> response = deleteRecipe(required(args, "ref"), jsonBody(ex));
                case "GET /api/export" -> {
                    String ref = required(args, "ref");
                    bytes = exportItem(ref);
                    mime = "application/zip";
                    ex.getResponseHeaders().set("Content-Disposition",
                            "attachment; filename=\"vsrender-" + localId(ref.split("\\|")[1]) + ".zip\"");
                    response = null;
                }
                case "GET /api/thumb" -> {
                    byte[] png = thumbnail(required(args, "ref"));
                    if (png == null) fail(404, "No hay textura");
                    bytes = png;
                    mime = "image/png";
                    response = null;
                }
                case "GET /api/textures" -> response = textures(required(args, "pack"));
                case "GET /api/texture" -> {
                    bytes = texture(required(args, "pack"), required(args, "id"));
                    mime = "image/png";
                    response = null;
                }
                case "POST /api/texture" -> response = uploadTexture(
                        required(args, "pack"), required(args, "name"), readBody(ex));
                case "POST /api/create" -> response = create(jsonBody(ex), null);
                case "POST /api/template" -> response = create(jsonBody(ex), "template");
                case "POST /api/packs" -> response = createPack(jsonBody(ex));
                case "POST /api/duplicate" -> response = duplicate(jsonBody(ex));
                case "POST /api/import" -> response = importItem(required(args, "pack"), readBody(ex),
                        ex.getRequestHeaders().getFirst("X-VSRender-File"));
                case "PUT /api/item" -> response = update(required(args, "ref"), jsonBody(ex));
                case "DELETE /api/item" -> response = delete(required(args, "ref"), jsonBody(ex));
                case "POST /api/reload" -> {
                    if (plugin.resourceOperations().isBusy()) fail(409, "Hay otra operacion de recursos en curso");
                    plugin.reloadPlugin(plugin.scheduler().async(),
                            r -> plugin.scheduler().platform().run(r), false, true);
                    response = Map.of("ok", true, "message", "Recarga solicitada. Revisa la consola.");
                    code = 202;
                }
                default -> throw new WebError(404, "Ruta no encontrada");
            }
            send(ex, code, mime, bytes != null ? bytes : JSON.toJson(response).getBytes(StandardCharsets.UTF_8));
        } catch (WebError e) {
            send(ex, e.status, "application/json; charset=utf-8",
                    JSON.toJson(Map.of("error", e.getMessage())).getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            plugin.logger().warn("VSRender web request failed", e);
            send(ex, 500, "application/json; charset=utf-8",
                    JSON.toJson(Map.of("error", "Error al procesar la solicitud; revisa la consola del servidor.")).getBytes(StandardCharsets.UTF_8));
        } finally {
            ex.close();
        }
    }

    private static void send(HttpExchange ex, int status, String type, byte[] content) throws IOException {
        var headers = ex.getResponseHeaders();
        headers.set("Content-Type", type);
        headers.set("Cache-Control", "no-store");
        headers.set("X-Content-Type-Options", "nosniff");
        headers.set("X-Frame-Options", "DENY");
        headers.set("Referrer-Policy", "no-referrer");
        headers.set("Content-Security-Policy",
                "default-src 'self'; img-src 'self' data:; style-src 'self' 'unsafe-inline'; " +
                "script-src 'self'; connect-src 'self'; frame-ancestors 'none'; object-src 'none'");
        ex.sendResponseHeaders(status, status == 303 ? -1 : content.length);
        if (status != 303) ex.getResponseBody().write(content);
    }

    private static byte[] resource(String name) {
        try (InputStream in = ContentWebServer.class.getClassLoader().getResourceAsStream(name)) {
            if (in == null) throw new WebError(404, "Recurso integrado no disponible: " + name);
            return in.readAllBytes();
        } catch (IOException e) {
            throw new WebError(500, "No se pudo leer el recurso web");
        }
    }

    private static Map<String,String> query(HttpExchange ex) {
        Map<String,String> out = new LinkedHashMap<>();
        String q = ex.getRequestURI().getRawQuery();
        if (q != null) for (String p : q.split("&", -1)) {
            int sep = p.indexOf('=');
            if (sep >= 0) out.put(URLDecoder.decode(p.substring(0, sep), StandardCharsets.UTF_8),
                    URLDecoder.decode(p.substring(sep + 1), StandardCharsets.UTF_8));
        }
        return out;
    }

    private static String required(Map<String,String> args, String key) {
        String value = args.get(key);
        if (value == null || value.isBlank()) throw new WebError(400, "Falta: " + key);
        return value;
    }

    private static String text(Map<?,?> map, String key) {
        Object value = map.get(key);
        return value == null ? "" : String.valueOf(value);
    }

    private static String id(String value) {
        if (value == null || !PART.matcher(value).matches()) fail(400, "Identificador invalido: " + value);
        return value;
    }

    private static String key(String value) {
        if (value == null || !ITEM_ID.matcher(value).matches()) fail(400, "ID completo invalido");
        return value;
    }

    private static String localId(String key) {
        String[] v = key.split(":",2);
        return id(v.length == 2 ? v[1] : v[0]);
    }

    private static String slug(String name) {
        String value = java.text.Normalizer.normalize(name, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "_").replaceAll("^_+|_+$", "");
        return id(value);
    }

    private static String name(Map<?,?> item, String fallback) {
        Object data = item.get("data");
        Object n = data instanceof Map<?,?> d ? d.get("item_name") : null;
        return n == null ? fallback : String.valueOf(n).replaceAll("<[^>]*>", "").trim();
    }

    private static List<String> behaviors(Map<?,?> item) {
        Object raw = item.get("behavior");
        List<?> entries = raw instanceof List<?> list ? list : raw == null ? List.of() : List.of(raw);
        List<String> out = new ArrayList<>();
        for (Object element : entries) if (element instanceof Map<?,?> m && m.get("type") != null) {
            out.add(String.valueOf(m.get("type")));
        }
        return out;
    }

    private static String kind(Map<?,?> entry) {
        List<String> kinds = behaviors(entry);
        return kinds.contains("furniture_item") ? "furniture" : kinds.contains("block_item") ? "blocks" : "items";
    }

    private static Map<String,Object> yaml(String data) {
        try {
            Object result = new Load(LoadSettings.builder().setAllowDuplicateKeys(false)
                    .setCodePointLimit(LIMIT).build()).loadFromString(data);
            if (!(result instanceof Map<?,?> m)) fail(400, "El YAML debe ser un mapa.");
            @SuppressWarnings("unchecked") Map<String,Object> parsed = (Map<String,Object>) result;
            return parsed;
        } catch (WebError e) { throw e; }
        catch (RuntimeException e) { throw new WebError(400, "YAML invalido: " + e.getMessage()); }
    }

    private static String dump(Object content) {
        return new Dump(DumpSettings.builder().setDefaultFlowStyle(FlowStyle.BLOCK)
                .setIndent(2).build()).dumpToString(content);
    }

    private static Map<?,?> entries(Map<String,Object> document, String section) {
        Object obj = document.get(section);
        return obj instanceof Map<?,?> m ? m : Map.of();
    }

    private static String digest(byte[] data) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(data);
            return java.util.HexFormat.of().formatHex(hash);
        } catch (java.security.NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }

    private static byte[] read(Path p) {
        try {
            if (Files.isSymbolicLink(p) || Files.size(p) > LIMIT) fail(400, "Archivo simbolico o demasiado grande");
            return Files.readAllBytes(p);
        } catch (IOException e) { throw new WebError(400, "Error leyendo archivo"); }
    }

    private static Map<String,Object> jsonBody(HttpExchange ex) throws IOException {
        byte[] raw = readBody(ex);
        try {
            Object data = JSON.fromJson(new String(raw, StandardCharsets.UTF_8), Map.class);
            if (data instanceof Map<?,?> m) {
                @SuppressWarnings("unchecked") Map<String,Object> cast = (Map<String,Object>) m;
                return cast;
            }
        } catch (RuntimeException e) { throw new WebError(400, "JSON invalido"); }
        throw new WebError(400, "Se esperaba un objeto JSON");
    }

    private static byte[] readBody(HttpExchange ex) throws IOException {
        String raw = ex.getRequestHeaders().getFirst("Content-Length");
        if (raw != null) try { if (Long.parseLong(raw) > LIMIT) fail(413, "Solicitud mayor a 1 MB"); }
        catch (NumberFormatException e) { fail(400, "Content-Length incorrecto"); }
        byte[] bytes = ex.getRequestBody().readNBytes(LIMIT + 1);
        if (bytes.length > LIMIT) fail(413, "Solicitud mayor a 1 MB");
        return bytes;
    }

    private Path pack(String name) {
        id(name);
        Path p = resources.resolve(name).normalize();
        if (!p.startsWith(resources) || Files.isSymbolicLink(p) || !Files.isDirectory(p)
                || !Files.isRegularFile(p.resolve("pack.yml"), LinkOption.NOFOLLOW_LINKS)) fail(404, "Pack no encontrado");
        return p;
    }

    private List<Map<String,Object>> packs() {
        List<Map<String,Object>> result = new ArrayList<>();
        try (var folders = Files.list(resources)) {
            folders.filter(Files::isDirectory).filter(p -> !Files.isSymbolicLink(p))
                    .sorted().forEach(p -> {
                        try {
                            id(p.getFileName().toString());
                            if (!Files.isRegularFile(p.resolve("pack.yml"), LinkOption.NOFOLLOW_LINKS)) return;
                            Map<String,Object> meta = yaml(new String(read(p.resolve("pack.yml")), StandardCharsets.UTF_8));
                            Map<String,Object> info = new LinkedHashMap<>();
                            info.put("name", p.getFileName().toString());
                            info.put("namespace", text(meta,"namespace").isBlank() ? p.getFileName().toString():text(meta,"namespace"));
                            info.put("description", text(meta,"description"));
                            result.add(info);
                        } catch (RuntimeException ignored) { }
                    });
        } catch (IOException e) { throw new WebError(500, "No se pueden listar los packs"); }
        return result;
    }

    private List<Path> files(Path p) {
        List<Path> out = new ArrayList<>();
        Path dir = p.resolve("configuration");
        if (!Files.isDirectory(dir, LinkOption.NOFOLLOW_LINKS)) return out;
        try (var walk = Files.walk(dir, 10)) {
            walk.filter(f -> Files.isRegularFile(f, LinkOption.NOFOLLOW_LINKS))
                    .filter(f -> !Files.isSymbolicLink(f))
                    .filter(f -> f.getFileName().toString().matches("[a-zA-Z0-9_.-]+\\.ya?ml"))
                    .limit(3000).forEach(out::add);
        } catch (IOException e) { throw new WebError(500, "Error leyendo configuraciones"); }
        out.sort(Comparator.naturalOrder());
        return out;
    }

    private record Entry(Path pack, Path file, String key, Map<String,Object> document,
                         Map<String,Object> definition, byte[] bytes) {
        String ref() { return pack.getFileName() + "|" + key; }
    }

    private Entry find(String ref) {
        String[] s = ref.split("\\|",2);
        if (s.length != 2) throw new WebError(400, "Referencia invalida");
        Path p = pack(s[0]); key(s[1]);
        for (Path f : files(p)) {
            byte[] raw = read(f);
            Map<String,Object> doc;
            try { doc = yaml(new String(raw, StandardCharsets.UTF_8)); }
            catch (WebError e) { continue; }
            Object definition = entries(doc, "items").get(s[1]);
            if (definition instanceof Map<?,?> map) {
                @SuppressWarnings("unchecked") Map<String,Object> cast = (Map<String,Object>) map;
                return new Entry(p,f,s[1],doc,cast,raw);
            }
        }
        throw new WebError(404, "Creacion no encontrada");
    }

    private Map<String,Object> summary(Entry e) {
        Map<String,Object> o = new LinkedHashMap<>();
        o.put("ref", e.ref()); o.put("id",e.key());
        o.put("pack", e.pack().getFileName().toString());
        o.put("name",name(e.definition(),e.key().split(":")[1].replace('_',' ')));
        o.put("material",String.valueOf(e.definition().getOrDefault("material","paper")));
        o.put("type",kind(e.definition()));
        o.put("behavior",behaviors(e.definition()));
        o.put("revision",digest(e.bytes()));
        o.put("file",e.pack().relativize(e.file()).toString().replace('\\','/'));
        o.put("hasModel",e.definition().containsKey("model"));
        o.put("texture", String.valueOf(e.definition().getOrDefault("texture","")));
        return o;
    }

    private Object catalog() {
        List<Object> items = new ArrayList<>(), errors = new ArrayList<>();
        List<Map<String,Object>> listed = packs();
        for (var p : listed) {
            Path folder = pack(String.valueOf(p.get("name")));
            for (Path f : files(folder)) {
                try {
                    byte[] bytes = read(f);
                    Map<String,Object> doc = yaml(new String(bytes, StandardCharsets.UTF_8));
                    for (var kv : entries(doc,"items").entrySet()) {
                        if (!(kv.getKey() instanceof String k) || !ITEM_ID.matcher(k).matches()
                                || !(kv.getValue() instanceof Map<?,?> m)) continue;
                        @SuppressWarnings("unchecked") Map<String,Object> definition = (Map<String,Object>) m;
                        items.add(summary(new Entry(folder,f,k,doc,definition,bytes)));
                    }
                } catch (WebError err) {
                    errors.add(Map.of("pack",p.get("name"), "file", folder.relativize(f).toString(),"message",err.getMessage()));
                }
            }
        }
        return Map.of("items",items,"packs",listed,"errors",errors,"demo",false,"resources",resources.toString());
    }

    private Object materials() {
        List<Object> items = new ArrayList<>();
        Arrays.stream(Material.values()).filter(Material::isItem).filter(m -> !m.isLegacy())
                .sorted(Comparator.comparing(Enum::name))
                .forEach(m -> items.add(Map.of("id",m.name().toLowerCase(Locale.ROOT),
                        "key","minecraft:"+m.name().toLowerCase(Locale.ROOT),
                        "block",m.isBlock(),"max_stack_size",m.getMaxStackSize())));
        return Map.of("ready",true,"demo",false,"minecraft_version",org.bukkit.Bukkit.getMinecraftVersion(),
                "materials",items,"count",items.size());
    }

    private Object item(String ref) {
        Entry e = find(ref);
        Map<String,Object> ret = summary(e);
        ret.put("yaml",dump(Map.of("items",Map.of(e.key(),e.definition()))));
        return ret;
    }

    private static String quote(String value) {
        return "'" + value.replace("'","''").replace("\r"," ").replace("\n"," ") + "'";
    }

    private static final Set<String> EFFECTS = Set.of(
            "speed", "slowness", "haste", "mining_fatigue", "strength", "instant_health",
            "instant_damage", "jump_boost", "nausea", "regeneration", "resistance",
            "fire_resistance", "water_breathing", "invisibility", "blindness", "night_vision",
            "hunger", "weakness", "poison", "wither", "health_boost", "absorption",
            "saturation", "glowing", "levitation", "luck", "unluck", "slow_falling",
            "conduit_power", "dolphins_grace", "darkness");
    private static final Set<String> ANIMATIONS = Set.of("eat", "drink");
    private static final Set<String> TEMPLATES = Set.of("ingrediente", "alimento", "bebida", "plato", "cultivo",
            "objeto", "herramienta", "equipable", "bloque", "mineral", "mueble");

    private static String itemSource(String key, String title, String material, String template,
                                     Map<String,Object> options, int stack) {
        StringBuilder s = new StringBuilder("items:\n  " + key + ":\n    material: " + material
                + "\n    data:\n      item_name: " + quote("<!i><white>" + title) + "\n");
        // This component overrides the vanilla base-material stack limit.
        s.append("      max_stack_size: ").append(stack).append("\n");
        String description = text(options,"description").trim();
        if (description.length() > 240) fail(400, "La descripcion tiene mas de 240 caracteres");
        String dish = text(options,"dish_type").trim().toLowerCase(Locale.ROOT);
        if (template.equals("plato")) {
            if (dish.isBlank()) dish = "sopa";
            if (!Set.of("sopa", "ensalada", "guiso", "estofado", "tarta").contains(dish))
                fail(400, "Tipo de plato no disponible");
        }
        if (!description.isBlank() || template.equals("ingrediente") || template.equals("plato")) {
            s.append("      lore:\n");
            if (template.equals("ingrediente"))
                s.append("        - '<!i><dark_gray>Ingrediente'\n");
            if (template.equals("plato"))
                s.append("        - ").append(quote("<!i><dark_gray>Plato: " + dish)).append("\n");
            if (!description.isBlank())
                s.append("        - ").append(quote("<!i><gray>" + description)).append("\n");
        }
        String texture = text(options,"texture").trim();
        if (!texture.isBlank()) {
            if (!texture.matches("[a-z0-9_.-]+:[a-z0-9_./-]+") || texture.contains(".."))
                fail(400, "La textura debe tener formato namespace:item/nombre");
        }
        if (Set.of("alimento","bebida","plato","cultivo").contains(template)) {
            int nutrition = (int) number(options.get("nutrition"),template.equals("bebida")?0:4,0,20);
            double saturation = number(options.get("saturation"),template.equals("bebida")?0:1.2,0,20);
            boolean always = Boolean.TRUE.equals(options.get("always_eat"));
            String animation = text(options,"animation").trim().toLowerCase(Locale.ROOT);
            if (animation.isBlank()) animation = template.equals("bebida")?"drink":"eat";
            if (!ANIMATIONS.contains(animation)) fail(400, "Animacion no permitida");
            double seconds = number(options.get("consume_seconds"), animation.equals("drink")?1.6:1.6,0.05,60);
            List<String> consumeEffects = new ArrayList<>();
            for (String prefix : List.of("effect", "effect2")) {
                String effect = text(options,prefix).trim().toLowerCase(Locale.ROOT).replace("minecraft:","");
                if (effect.isBlank()) continue;
                if (!EFFECTS.contains(effect)) fail(400, "Efecto no disponible: " + effect);
                int level = (int)number(options.get(prefix+"_level"),1,1,5);
                int durationTicks = (int)Math.round(number(options.get(prefix+"_seconds"),10,1,3600)*20);
                double probability = number(options.get(prefix+"_chance"),1,0,1);
                consumeEffects.add("{type:'minecraft:apply_effects',effects:[{id:'minecraft:"
                        + effect + "',duration:" + durationTicks + ",amplifier:" + (level-1)
                        + "}],probability:" + probability + "f}");
            }
            s.append("      food:\n        nutrition: ").append(nutrition)
                    .append("\n        saturation: ").append(saturation)
                    .append("\n        can_always_eat: ").append(always).append("\n");
            StringBuilder snbt = new StringBuilder("{consume_seconds:").append(seconds)
                    .append("f,animation:'").append(animation).append("'");
            if (!consumeEffects.isEmpty()) {
                snbt.append(",on_consume_effects:[")
                    .append(String.join(",",consumeEffects))
                    .append("]");
            }
            snbt.append("}");
            s.append("      components:\n        minecraft:consumable: ")
                    .append(quote("(snbt) " + snbt)).append("\n");
            String remain = text(options,"remainder").trim().toLowerCase(Locale.ROOT);
            if (!remain.isBlank() && !remain.equals("none")) {
                if (!remain.matches("(?:minecraft:)?[a-z0-9_]+"))
                    fail(400, "Recipiente invalido");
                String id = remain.startsWith("minecraft:")?remain:"minecraft:"+remain;
                Material remainderMaterial = Material.matchMaterial(id.substring("minecraft:".length()).toUpperCase(Locale.ROOT));
                if (remainderMaterial == null || !remainderMaterial.isItem() || remainderMaterial.isLegacy())
                    fail(400, "El recipiente no existe en este servidor");
                s.append("      use_remainder:\n        id: ").append(id).append("\n        count: 1\n");
            }
        }
        if (template.equals("herramienta")) {
            if (Material.matchMaterial(material.toUpperCase(Locale.ROOT)).getMaxDurability() <= 0)
                fail(400, "Para herramientas, elegi una base con durabilidad (pico, espada, hacha...)");
            int damage = (int) number(options.get("max_damage"), 256, 1, 100000);
            s.append("      max_damage: ").append(damage).append("\n");
        }
        if (template.equals("equipable")) {
            String slot = text(options, "equip_slot").toLowerCase(Locale.ROOT);
            if (!Set.of("head","chest","legs","feet").contains(slot))
                fail(400, "Selecciona una ranura de equipamiento valida");
            s.append("      equippable:\n        slot: ").append(slot).append("\n");
        }
        if (!texture.isBlank()) s.append("    texture: ").append(texture).append("\n");
        if (template.equals("bloque") || template.equals("mineral")) {
            String appearance = texture.isBlank() ? "minecraft:block/stone" : texture;
            if (!appearance.matches("[a-z0-9_.-]+:(item|block)/[a-z0-9_./-]+") || appearance.contains(".."))
                fail(400, "Textura de bloque invalida");
            String sound = text(options,"block_sound").trim().toLowerCase(Locale.ROOT);
            if (sound.isBlank()) sound = "stone";
            if (!Set.of("stone","wood","metal","glass","sand","grass").contains(sound))
                fail(400, "Sonido de bloque desconocido");
            double hardness = number(options.get("hardness"), template.equals("mineral") ? 3 : 1.5, 0, 50);
            double resistance = number(options.get("resistance"), 3, 0, 1200);
            s.append("    model:\n")
                    .append("      type: minecraft:model\n      path: ").append(key.split(":")[0]).append(":block/").append(key.split(":")[1]).append("\n")
                    .append("      generation:\n        parent: minecraft:block/cube_all\n        textures:\n")
                    .append("          all: ").append(appearance).append("\n")
                    .append("    behavior:\n      type: block_item\n      block: ").append(key).append("\n")
                    .append("blocks:\n  ").append(key).append(":\n")
                    .append("    settings:\n      template:\n        - default:sound/").append(sound)
                    .append("\n      overrides:\n        hardness: ").append(hardness)
                    .append("\n        resistance: ").append(resistance)
                    .append("\n        tags:\n          - minecraft:mineable/")
                    .append(template.equals("mineral") || sound.equals("stone") || sound.equals("metal") ? "pickaxe" : sound.equals("wood") ? "axe" : "shovel")
                    .append("\n    loot:\n      template: default:loot_table/self\n")
                    .append("    state:\n      auto_state: solid\n      texture: ").append(appearance).append("\n");
        }
        if (template.equals("mueble")) {
            if (texture.isBlank()) fail(400, "El mueble necesita una textura de item (PNG propio o existente)");
            s.append("    behavior:\n      type: furniture_item\n")
                    .append("      rules:\n        ground:\n          rotation: any\n          alignment: any\n")
                    .append("      furniture:\n        settings:\n          item: ").append(key)
                    .append("\n          hit_times: 3\n        variants:\n          ground:\n")
                    .append("            elements:\n              - item: ").append(key)
                    .append("\n                display_transform: none\n                billboard: fixed\n")
                    .append("                translation: 0,0.5,0\n                shadow_radius: 0.4\n                shadow_strength: 0.4\n")
                    .append("            hitboxes:\n              - type: interaction\n                position: 0,0,0\n")
                    .append("                width: 0.8\n                height: 1.0\n                interactive: true\n")
                    .append("                invisible: true\n                blocks_building: true\n")
                    .append("        loot:\n          template: default:loot_table/furniture\n")
                    .append("          arguments:\n            item: ").append(key).append("\n");
        }
        return s.toString();
    }

    private synchronized Object create(Map<String,Object> body, String type) {
        Path p = pack(text(body,"pack"));
        String namespace = text(body,"namespace");
        if (namespace.isBlank()) namespace = p.getFileName().toString();
        id(namespace);
        String title = text(body,"name").trim();
        if (title.isBlank() || title.length()>120) fail(400,"Nombre requerido (max. 120 caracteres)");
        String nameId = text(body,"id").isBlank() ? slug(title) : id(text(body,"id"));
        String full = namespace+":"+nameId;
        for (Path f: files(p)) {
            Map<String,Object> doc = yaml(new String(read(f), StandardCharsets.UTF_8));
            if (entries(doc,"items").containsKey(full) || entries(doc,"blocks").containsKey(full))
                fail(409,"ID ya existente");
        }
        String template = type == null ? "ingrediente" : text(body,"template");
        if (!TEMPLATES.contains(template)) fail(400,"Plantilla no disponible para generacion automatica");
        String material = text(body,"material").replace("minecraft:","");
        if (material.isBlank()) material = switch (template) {
            case "alimento" -> "apple";
            case "bebida" -> "honey_bottle";
            case "plato" -> "beetroot_soup";
            case "bloque","mineral" -> "stone";
            case "herramienta" -> "iron_pickaxe";
            case "equipable" -> "leather_helmet";
            default -> "paper";
        };
        Material vanilla = Material.matchMaterial(material.toUpperCase(Locale.ROOT));
        if (vanilla == null || !vanilla.isItem() || vanilla.isLegacy()) fail(400,"Material no disponible en este servidor: " + material);
        int maxStack = (int)Math.round(number(body.get("max_stack_size"),
                Set.of("plato","herramienta","equipable","mueble").contains(template)?1:64,1,99));
        if (Set.of("herramienta","equipable").contains(template) && maxStack != 1)
            fail(400, "Las herramientas y equipables deben tener stack de 1");
        String subdir = switch (template) {
            case "bloque", "mineral" -> "blocks";
            case "mueble" -> "furniture";
            default -> "items";
        };
        Path target = p.resolve("configuration/" + subdir + "/" + nameId + ".yml");
        if (Files.exists(target,LinkOption.NOFOLLOW_LINKS)) fail(409,"Archivo ya existente");
        String config = itemSource(full,title,material,template,body,maxStack);
        if ("cultivo".equals(template)) {
            for (Path file : files(p)) {
                Map<String,Object> parsed=yaml(new String(read(file),StandardCharsets.UTF_8));
                if(entries(parsed,"items").containsKey(full+"_semillas") ||
                        entries(parsed,"blocks").containsKey(full+"_cultivo"))
                    fail(409,"Ya existe la semilla o el cultivo de este ID");
            }
            int stages=(int)number(body.get("stages"),4,2,8);
            double speed=number(body.get("grow_speed"),0.25,0.01,1);
            String seedsTexture=text(body,"seed_texture").trim();
            if(!seedsTexture.isBlank() && (!seedsTexture.matches("[a-z0-9_.-]+:item/[a-z0-9_.-]+")
                    || seedsTexture.contains(".."))) fail(400,"La textura de semilla debe ser namespace:item/nombre");
            config = CropTemplateYaml.generate(full,title,config,stages,speed,seedsTexture);
        }
        yaml(config); // Validate all generated sections before writing.
        write(target,config.getBytes(StandardCharsets.UTF_8),false);
        return item(p.getFileName()+"|"+full);
    }

    private static double number(Object obj, double fallback, double low, double high) {
        if (obj == null || obj.toString().isBlank()) return fallback;
        try {
            double n=Double.parseDouble(obj.toString());
            if (!Double.isFinite(n) || n<low || n>high) fail(400,"Valor numerico fuera de rango");
            return n;
        } catch (NumberFormatException e) { throw new WebError(400,"Valor numerico invalido"); }
    }

    private static String patchOne(String source, String section, String key, String replacement) {
        // Replace only one YAML child node and preserve sibling definitions,
        // comments and unrelated top-level sections byte-for-byte.
        String[] lines=source.split("(?<=\n)",-1);
        int top=-1, at=-1, end=lines.length;
        for(int i=0;i<lines.length;i++){
            String line=lines[i].replace("\r","");
            if(line.matches("^"+Pattern.quote(section)+":\\s*(?:#.*)?\\n?$")){ top=i; break; }
        }
        if(top<0) fail(400,"Falta seccion "+section);
        for(int i=top+1;i<lines.length;i++){
            String line=lines[i].replace("\r","");
            if(line.matches("^[A-Za-z_][^\\s:]*:\\s*(?:#.*)?\\n?$")) break;
            if(line.startsWith("  "+key+":") && (line.length()==key.length()+3 || Character.isWhitespace(line.charAt(key.length()+3)))){at=i;break;}
        }
        if(at<0) fail(404,"Definicion no encontrada en el archivo");
        for(int i=at+1;i<lines.length;i++){
            String line=lines[i].replace("\r","");
            if(!line.isBlank() && !line.startsWith("#")
                    && !line.startsWith(" ") && !line.startsWith("\t")){end=i;break;}
            if(line.startsWith("  ") && !line.startsWith("    ") && !line.isBlank() && !line.startsWith("  #")){end=i;break;}
        }
        StringBuilder out=new StringBuilder();
        for(int i=0;i<at;i++) out.append(lines[i]);
        out.append(replacement);
        if(!replacement.isEmpty() && !replacement.endsWith("\n"))out.append("\n");
        for(int i=end;i<lines.length;i++)out.append(lines[i]);
        return out.toString();
    }

    private synchronized Object update(String ref, Map<String,Object> body) {
        Entry e=find(ref);
        if (!Objects.equals(text(body,"revision"),digest(e.bytes())))fail(409,"El YAML cambio; actualiza la ficha");
        String old=new String(e.bytes(),StandardCharsets.UTF_8);
        Map<String,Object> definition = new LinkedHashMap<>(e.definition());
        if(!text(body,"yaml").isBlank()){
            Map<String,Object> parsed=yaml(text(body,"yaml"));
            Map<?,?> entries=entries(parsed,"items");
            Object itemObject = entries.get(e.key());
            if(entries.size()!=1 || !(itemObject instanceof Map<?,?>)) fail(400,"Solo podes editar items: "+e.key());
            @SuppressWarnings("unchecked") Map<String,Object> cast=(Map<String,Object>)itemObject;
            definition=cast;
        }else {
            String title=text(body,"name").trim();
            if(title.isBlank()||title.length()>120)fail(400,"Nombre invalido");
            String material=text(body,"material").replace("minecraft:","");
            Material m=Material.matchMaterial(material.toUpperCase(Locale.ROOT));
            if(m==null||!m.isItem()||m.isLegacy())fail(400,"Material invalido");
            definition.put("material", material);
            Map<String,Object> data = definition.get("data") instanceof Map<?,?> d
                    ? new LinkedHashMap<>((Map<String,Object>)d) : new LinkedHashMap<>();
            data.put("item_name","<!i><white>"+title);
            definition.put("data",data);
            if (body.containsKey("texture")) {
                String texture=text(body,"texture").trim();
                if (texture.isBlank()) definition.remove("texture");
                else {
                    if (!texture.matches("[a-z0-9_.-]+:[a-z0-9_./-]+") || texture.contains(".."))
                        fail(400,"La textura debe ser un ID como cocina:item/cebolla");
                    definition.put("texture", texture);
                }
            }
        }
        String converted=dump(Map.of("items",Map.of(e.key(),definition)));
        int begin=converted.indexOf('\n');
        if(begin<0)fail(400,"Error serializando definicion");
        String replaced=patchOne(old,"items",e.key(),converted.substring(begin+1));
        yaml(replaced);
        backup(e);
        write(e.file(),replaced.getBytes(StandardCharsets.UTF_8),true);
        return item(ref);
    }

    /** A crop template is stored in a single dedicated file and must be
     * imported, exported or deleted together with its linked seed and block. */
    private static boolean cropBundle(Entry e) {
        Map<?,?> items=entries(e.document(),"items"),blocks=entries(e.document(),"blocks");
        return items.size()==2 && blocks.size()==1
                && items.containsKey(e.key()) && items.containsKey(e.key()+"_semillas")
                && blocks.containsKey(e.key()+"_cultivo")
                && e.document().keySet().stream().allMatch(k->k.equals("items")||k.equals("blocks"));
    }
    /** A generated block and its associated placing item must stay in one file. */
    private static boolean blockBundle(Entry e) {
        Map<?,?> items=entries(e.document(),"items"),blocks=entries(e.document(),"blocks");
        return items.size()==1 && blocks.size()==1
                && items.containsKey(e.key()) && blocks.containsKey(e.key())
                && e.document().keySet().stream().allMatch(k->k.equals("items")||k.equals("blocks"));
    }

    private static boolean cropSeed(Entry e) {
        if (!e.key().endsWith("_semillas")) return false;
        String parent=e.key().substring(0,e.key().length()-"_semillas".length());
        return entries(e.document(),"items").containsKey(parent) &&
                entries(e.document(),"blocks").containsKey(parent+"_cultivo");
    }

    private synchronized Object delete(String ref,Map<String,Object> body){
        Entry e=find(ref);
        if(!Objects.equals(text(body,"revision"),digest(e.bytes())))fail(409,"El YAML cambio; actualiza");
        if (cropSeed(e)) fail(409,"La semilla pertenece a un cultivo. Eliminalo desde el item principal.");
        if (cropBundle(e) || blockBundle(e)) {
            backup(e);
            try { Files.delete(e.file()); } catch(IOException err) { throw new WebError(500,"No se pudo eliminar el cultivo"); }
            return Map.of("ok",true,"bundle",true);
        }
        String src=new String(e.bytes(),StandardCharsets.UTF_8);
        String result=patchOne(src,"items",e.key(),"");
        yaml(result);
        backup(e);
        if(entries(yaml(result),"items").isEmpty()
                && yaml(result).keySet().stream().allMatch(k->k.equals("items"))){
            try{Files.delete(e.file());}catch(IOException err){throw new WebError(500,"No se pudo eliminar");}
        }else write(e.file(),result.getBytes(StandardCharsets.UTF_8),true);
        return Map.of("ok",true);
    }

    private synchronized Object duplicate(Map<String,Object> body){
        Entry e=find(text(body,"ref"));
        if(cropSeed(e)||cropBundle(e)||blockBundle(e))
            fail(409,"Para duplicar un cultivo completo, usa Plantillas > Verdura cultivable.");
        Path p=pack(text(body,"pack").isBlank()?e.pack().getFileName().toString():text(body,"pack"));
        String full=id(text(body,"namespace").isBlank()?e.key().split(":")[0]:text(body,"namespace"))+":"+id(text(body,"id"));
        String ref=p.getFileName()+"|"+full;
        for(Path f:files(p)){
            Map<String,Object> doc=yaml(new String(read(f),StandardCharsets.UTF_8));
            if(entries(doc,"items").containsKey(full))fail(409,"El ID ya existe");
        }
        Map<String,Object> m=new LinkedHashMap<>(e.definition());
        if(m.get("data") instanceof Map<?,?> raw){
            Map<String,Object> data=new LinkedHashMap<>((Map<String,Object>)raw);
            data.put("item_name", name(e.definition(),full)+" (copia)");
            m.put("data",data);
        }
        Path target=p.resolve("configuration/"+kind(m)+"/"+localId(full)+".yml");
        if(Files.exists(target,LinkOption.NOFOLLOW_LINKS))fail(409,"Archivo existente");
        write(target,dump(Map.of("items",Map.of(full,m))).getBytes(StandardCharsets.UTF_8),false);
        return item(ref);
    }

    private synchronized Object createPack(Map<String,Object> body) {
        String n=id(text(body,"name")), ns=id(text(body,"namespace"));
        Path p=resources.resolve(n);
        if(Files.exists(p,LinkOption.NOFOLLOW_LINKS))fail(409,"Ya existe el pack");
        try{
            Files.createDirectories(p.resolve("configuration/items"));
            write(p.resolve("pack.yml"),("author: VSRender Web\nversion: 1.0\nnamespace: "+ns+
                    "\ndescription: Creado desde el administrador web\n").getBytes(StandardCharsets.UTF_8),false);
        }catch(IOException ex){throw new WebError(500,"No se pudo crear el pack");}
        return Map.of("name",n,"namespace",ns);
    }

    private Object importItem(String packName,byte[] body,String filename){
        Path p=pack(packName);
        String yamlContent=null;
        if(filename==null) fail(400,"Falta nombre de archivo");
        if(filename.matches("(?i).+\\.ya?ml")){
            yamlContent=new String(body,StandardCharsets.UTF_8);
        }else if(filename.matches("(?i).+\\.zip")){
            try(ZipInputStream zip=new ZipInputStream(new java.io.ByteArrayInputStream(body))){
                int files=0,bytes=0;
                ZipEntry entry;
                while((entry=zip.getNextEntry())!=null){
                    if(++files>48) fail(413,"Demasiados archivos en ZIP");
                    String name=entry.getName();
                    if(name.startsWith("/")||name.contains("..")||name.contains("\\")||name.contains(":"))
                        fail(400,"Ruta ZIP invalida");
                    byte[] inner=zip.readNBytes(LIMIT+1);
                    bytes+=inner.length;
                    if(bytes>LIMIT||inner.length>LIMIT)fail(413,"ZIP demasiado grande");
                    if(name.matches("configuration/(items|blocks|furniture)/[a-z0-9_.-]+\\.ya?ml")){
                        if(yamlContent!=null)fail(400,"ZIP con mas de un YAML");
                        yamlContent=new String(inner,StandardCharsets.UTF_8);
                    }
                }
            }catch(IOException e){throw new WebError(400,"ZIP dañado");}
        }else fail(400,"Solo YAML o ZIP exportado por esta web");
        if(yamlContent==null)fail(400,"Falta un YAML valido");
        Map<String,Object> doc=yaml(yamlContent);
        Map<?,?> contents=entries(doc,"items"),blocks=entries(doc,"blocks");
        if (contents.isEmpty() || contents.size()>2 || blocks.size()>1
                || doc.keySet().stream().anyMatch(k->!k.equals("items")&&!k.equals("blocks")))
            fail(400,"El YAML debe incluir un item o un conjunto de cultivo (producto, semilla y bloque)");
        String mainId=null;
        for (Object k: contents.keySet()) {
            if (!(k instanceof String)) fail(400,"Clave de item invalida");
            String itemKey=(String)k;
            if (!ITEM_ID.matcher(itemKey).matches() ||
                    !(contents.get(itemKey) instanceof Map<?,?>))
                fail(400,"Items no validos");
            if (contents.size()==1 || !itemKey.endsWith("_semillas")) mainId=itemKey;
        }
        if(mainId==null)fail(400,"No se encontro el producto principal");
        Object primary=contents.get(mainId);
        boolean bundle=contents.size()==2 && blocks.size()==1 &&
                contents.containsKey(mainId+"_semillas") &&
                blocks.containsKey(mainId+"_cultivo") &&
                blocks.get(mainId+"_cultivo") instanceof Map<?,?>;
        boolean placedBlock=contents.size()==1 && blocks.size()==1 && blocks.containsKey(mainId)
                && blocks.get(mainId) instanceof Map<?,?>;
        if (!bundle && !placedBlock && (contents.size()!=1 || !blocks.isEmpty()))
            fail(400,"Cultivo incompleto: faltan semillas o bloque");
        String local=localId(mainId);
        for(Path file:files(p)) {
            Map<String,Object> other=yaml(new String(read(file),StandardCharsets.UTF_8));
            for(Object itemId:contents.keySet())
                if(entries(other,"items").containsKey(itemId))fail(409,"Ya existe el item "+itemId);
            for(Object blockId:blocks.keySet())
                if(entries(other,"blocks").containsKey(blockId))fail(409,"Ya existe el bloque "+blockId);
        }
        Path target=p.resolve("configuration/"+(bundle?"items":placedBlock?"blocks":kind((Map<?,?>)primary))+"/"+local+".yml");
        if(Files.exists(target,LinkOption.NOFOLLOW_LINKS))fail(409,"El archivo ya existe");
        Map<String,Object> clean=new LinkedHashMap<>();
        clean.put("items",contents);
        if(bundle || placedBlock)clean.put("blocks",blocks);
        write(target,dump(clean).getBytes(StandardCharsets.UTF_8),false);
        return item(p.getFileName()+"|"+mainId);
    }

    private byte[] exportItem(String ref){
        Entry e=find(ref);
        if(cropSeed(e)) {
            String base=e.key().substring(0,e.key().length()-"_semillas".length());
            return exportItem(e.pack().getFileName()+"|"+base);
        }
        boolean bundle=cropBundle(e) || blockBundle(e);
        String yamlContent=bundle ? dump(e.document())
                : dump(Map.of("items",Map.of(e.key(),e.definition())));
        try{
            ByteArrayOutputStream bytes=new ByteArrayOutputStream();
            try(ZipOutputStream zip=new ZipOutputStream(bytes)){
                zip.putNextEntry(new ZipEntry("configuration/items/"+localId(e.key())+".yml"));
                zip.write(yamlContent.getBytes(StandardCharsets.UTF_8));
                zip.closeEntry();
                zip.putNextEntry(new ZipEntry("manifest.json"));
                zip.write(JSON.toJson(Map.of("format",bundle?"vsrender-crop-export-v1":"vsrender-item-export-v1","id",e.key(),"pack",e.pack().getFileName().toString())).getBytes(StandardCharsets.UTF_8));
                zip.closeEntry();
            }
            return bytes.toByteArray();
        }catch(IOException x){throw new WebError(500,"No se pudo exportar");}
    }

    private byte[] thumbnail(String ref) {
        Entry e = find(ref);
        List<String> candidates = new ArrayList<>();
        // Honor the same top-level texture shortcuts as the underlying item system.
        collectTextureValues(e.definition().get("texture"), candidates, 0);
        collectTextureValues(e.definition().get("textures"), candidates, 0);
        collectTextures(e, e.definition().get("model"), candidates, 0);
        collectTextures(e, e.definition().get("models"), candidates, 0);
        collectTextures(e, e.definition().get("legacy_model"), candidates, 0);
        String vanilla = String.valueOf(e.definition().getOrDefault("material","paper"))
                .replace("minecraft:", "").toLowerCase(Locale.ROOT);
        // Existing resource packs may override vanilla item or block sprites.
        candidates.add("minecraft:item/" + vanilla);
        candidates.add("minecraft:block/" + vanilla);
        for (String candidate : candidates.stream().distinct().toList()) {
            Path file = findPackAsset(e.pack(), "textures", candidate, ".png");
            if (file == null) continue;
            try {
                if (Files.size(file) < 500_000) return Files.readAllBytes(file);
            } catch (IOException ignored) {}
        }
        // No default Minecraft textures ship inside the server JAR.
        // The web client intentionally displays its honest, labeled fallback.
        return null;
    }

    /** Real sprites from the owner's resource pack or another installed pack.
     * Does not access the Internet or pretend to render 3D models. */
    private Path findPackAsset(Path owner, String category, String key, String suffix) {
        if (!key.matches("[a-z0-9_.-]+:[a-z0-9_./-]+") || key.contains("..")) return null;
        String[] parts = key.split(":",2);
        Path relative = Path.of("resourcepack","assets",parts[0],category,parts[1]+suffix);
        Path direct = validPackAsset(owner,relative);
        if (direct != null) return direct;
        try (var folders = Files.list(resources)) {
            for (Path pack : folders.filter(Files::isDirectory).limit(150).toList()) {
                if (pack.equals(owner) || Files.isSymbolicLink(pack) ||
                        !Files.isRegularFile(pack.resolve("pack.yml"),LinkOption.NOFOLLOW_LINKS)) continue;
                Path hit = validPackAsset(pack,relative);
                if (hit != null) return hit;
            }
        } catch (IOException ignored) {}
        return null;
    }

    private static Path validPackAsset(Path pack, Path path) {
        Path candidate=pack.resolve(path).normalize();
        if (!candidate.startsWith(pack) || !Files.isRegularFile(candidate,LinkOption.NOFOLLOW_LINKS)
                || Files.isSymbolicLink(candidate)) return null;
        try { return candidate.toRealPath().startsWith(pack.toRealPath()) ? candidate : null; }
        catch (IOException ignored) { return null; }
    }

    /** Pack-local resource-pack texture upload; never serves arbitrary filesystem paths. */
    private static final byte[] PNG_MAGIC =
            new byte[]{(byte)137,80,78,71,13,10,26,10};

    private String packNamespace(Path folder) {
        Map<String,Object> metadata = yaml(new String(read(folder.resolve("pack.yml")), StandardCharsets.UTF_8));
        String namespace = text(metadata,"namespace");
        return id(namespace.isBlank() ? folder.getFileName().toString() : namespace);
    }

    private Path textureFile(Path folder, String assetId) {
        if (!assetId.matches("[a-z0-9_.-]+:item/[a-z0-9_.-]+") || assetId.contains(".."))
            throw new WebError(400, "Ruta de textura no valida");
        String ns = packNamespace(folder);
        if (!assetId.startsWith(ns + ":item/")) fail(403,"La textura debe pertenecer al namespace del pack");
        String name = id(assetId.substring((ns+":item/").length()));
        Path file = folder.resolve("resourcepack/assets/" + ns + "/textures/item/" + name + ".png").normalize();
        if (!file.startsWith(folder)) fail(403,"Ruta de textura fuera del pack");
        return file;
    }

    private byte[] texture(String packName,String assetId) {
        Path file = textureFile(pack(packName),assetId);
        try {
            if (!Files.isRegularFile(file,LinkOption.NOFOLLOW_LINKS) || Files.isSymbolicLink(file)
                    || Files.size(file)>500_000) fail(404,"Textura no encontrada");
            if (!file.toRealPath().startsWith(resources.toRealPath())) fail(403,"Ruta no permitida");
            return Files.readAllBytes(file);
        } catch(IOException error) { throw new WebError(404,"Textura no encontrada"); }
    }

    private Object textures(String packName) {
        Path p=pack(packName);
        String ns=packNamespace(p);
        Path dir=p.resolve("resourcepack/assets/"+ns+"/textures/item");
        if (!Files.isDirectory(dir,LinkOption.NOFOLLOW_LINKS) || Files.isSymbolicLink(dir))
            return List.of();
        List<String> result = new ArrayList<>();
        try (var stream=Files.list(dir)) {
            stream.filter(f->Files.isRegularFile(f,LinkOption.NOFOLLOW_LINKS) && !Files.isSymbolicLink(f))
                  .map(f->f.getFileName().toString())
                  .filter(f->f.matches("[a-z0-9_.-]+\\.png") && !f.contains(".."))
                  .sorted().limit(400)
                  .forEach(name->result.add(ns+":item/"+name.substring(0,name.length()-4)));
        }catch(IOException ex){throw new WebError(500,"Error buscando texturas");}
        return result;
    }

    private synchronized Object uploadTexture(String packName,String name,byte[] bytes){
        Path folder=pack(packName);
        id(name);
        if(bytes.length<33 || bytes.length>500_000) fail(413,"El PNG debe medir entre 33 B y 500 KB");
        for(int i=0;i<PNG_MAGIC.length;i++)
            if(bytes[i]!=PNG_MAGIC[i]) fail(400,"El archivo no es PNG");
        if(bytes[12]!='I'||bytes[13]!='H'||bytes[14]!='D'||bytes[15]!='R')
            fail(400,"Cabecera PNG invalida");
        int width=java.nio.ByteBuffer.wrap(bytes,16,4).getInt();
        int height=java.nio.ByteBuffer.wrap(bytes,20,4).getInt();
        if(width<1||height<1||width>1024||height>1024)fail(400,"La imagen debe ser de hasta 1024x1024");
        String assetId=packNamespace(folder)+":item/"+name;
        write(textureFile(folder,assetId),bytes,false);
        return Map.of("texture",assetId,"width",width,"height",height);
    }

    /** Collect texture references without creating or rendering any model.
     * Handles top-level 'texture'/'textures', generated models and nested JSON layers. */
    private static void collectTextureValues(Object value, List<String> output, int depth) {
        if (depth > 8 || value == null) return;
        if (value instanceof String candidate) {
            if (candidate.matches("[a-z0-9_.-]+:[a-z0-9_./-]+") && !candidate.contains(".."))
                output.add(candidate);
        } else if (value instanceof List<?> list) {
            for (Object child : list) collectTextureValues(child, output, depth + 1);
        } else if (value instanceof Map<?,?> map) {
            // Prefer the primary item sprite rather than secondary particle/overlay layers.
            for (String slot : List.of("layer0", "layer1", "all", "front", "top", "side", "particle")) {
                collectTextureValues(map.get(slot), output, depth + 1);
            }
            for (Object child : map.values()) collectTextureValues(child, output, depth + 1);
        }
    }

    /** Model traversal only follows paths to existing local JSON and PNG resources. */
    private void collectTextures(Entry e, Object node, List<String> textures, int depth) {
        if (depth >= 8 || node == null) return;
        if (node instanceof Map<?,?> map) {
            collectTextureValues(map.get("texture"), textures, depth + 1);
            collectTextureValues(map.get("textures"), textures, depth + 1);
            collectTextureValues(map.get("generation"), textures, depth + 1);
            for (String part : List.of("generation", "model", "models", "on_true", "on_false",
                    "fallback", "default", "cases", "entries", "children", "overrides")) {
                collectTextures(e, map.get(part), textures, depth + 1);
            }
            Object path = map.get("path");
            if (path instanceof String s) collectTextures(e, s, textures, depth + 1);
            Object parent = map.get("parent");
            if (parent instanceof String s) collectTextures(e, s, textures, depth + 1);
        } else if (node instanceof List<?> list) {
            for (Object child : list) collectTextures(e, child, textures, depth + 1);
        } else if (node instanceof String key) {
            if (!key.matches("[a-z0-9_.-]+:[a-z0-9_./-]+") || key.contains("..")) return;
            Path modelFile = findPackAsset(e.pack(), "models", key, ".json");
            if (modelFile == null) return;
            try {
                if (Files.size(modelFile) > 200_000) return;
                Object parsed = JSON.fromJson(Files.readString(modelFile), Map.class);
                collectTextures(e, parsed, textures, depth + 1);
            } catch (IOException | RuntimeException ignored) { }
        }
    }


    // Crafting Studio is a file editor for the existing CraftEngine recipe parser.
    // Recipes remain plain YAML in each pack; no extra runtime recipe engine.
    private static final Set<String> WEB_RECIPE_TYPES = Set.of(
            "shaped", "shapeless", "smelting", "smoking", "blasting",
            "campfire_cooking", "stonecutting");
    private static final Set<String> WEB_COOKING_TYPES = Set.of(
            "smelting", "smoking", "blasting", "campfire_cooking");

    private Entry findRecipe(String ref) {
        String[] parts = ref.split("\\|", 2);
        if (parts.length != 2) fail(400, "Referencia de receta invalida");
        Path owner = pack(parts[0]);
        String recipeId = key(parts[1]);
        if (recipeId.contains("..")) fail(400, "Identificador de receta invalido");
        for (Path file : files(owner)) {
            byte[] raw = read(file);
            Map<String,Object> document;
            try { document = yaml(new String(raw, StandardCharsets.UTF_8)); }
            catch (WebError e) { continue; }
            Object definition = entries(document, "recipes").get(recipeId);
            if (definition instanceof Map<?,?> m) {
                @SuppressWarnings("unchecked") Map<String,Object> cast = (Map<String,Object>)m;
                return new Entry(owner, file, recipeId, document, cast, raw);
            }
        }
        throw new WebError(404, "Receta no encontrada");
    }

    private Object recipeCatalog() {
        List<Object> recipes = new ArrayList<>();
        for (Map<String,Object> info : packs()) {
            Path owner = pack(String.valueOf(info.get("name")));
            for (Path file : files(owner)) {
                try {
                    byte[] raw = read(file);
                    Map<String,Object> doc = yaml(new String(raw, StandardCharsets.UTF_8));
                    for (var definition : entries(doc, "recipes").entrySet()) {
                        if (!(definition.getKey() instanceof String recipeId)
                                || !ITEM_ID.matcher(recipeId).matches()
                                || !(definition.getValue() instanceof Map<?,?> m)) continue;
                        String type = text(m, "type").replace("minecraft:", "");
                        if (!WEB_RECIPE_TYPES.contains(type)) continue;
                        Map<?,?> result = m.get("result") instanceof Map<?,?> r ? r : Map.of();
                        recipes.add(Map.of(
                                "ref", owner.getFileName() + "|" + recipeId,
                                "id", recipeId,
                                "pack", owner.getFileName().toString(),
                                "type", type,
                                "result", text(result, "id"),
                                "revision", digest(raw)));
                    }
                } catch (WebError ignored) {
                    // Malformed unrelated files must not break the recipe list.
                }
            }
        }
        return recipes;
    }

    private Object recipe(String ref) {
        Entry e = findRecipe(ref);
        Map<String,Object> info = new LinkedHashMap<>();
        info.put("ref", e.ref());
        info.put("id", e.key());
        info.put("pack", e.pack().getFileName().toString());
        info.put("file", e.pack().relativize(e.file()).toString().replace('\\','/'));
        info.put("revision", digest(e.bytes()));
        info.put("definition", e.definition());
        return info;
    }

    private static String recipeItemId(String input) {
        String id = key(input == null ? "" : input.trim().toLowerCase(Locale.ROOT));
        if (id.contains("..")) fail(400, "ID de ingrediente o resultado invalido");
        return id;
    }

    private boolean hasRecipeItem(String key) {
        if (key.startsWith("minecraft:")) {
            Material material = Material.matchMaterial(key.substring("minecraft:".length()).toUpperCase(Locale.ROOT));
            return material != null && material.isItem() && !material.isLegacy();
        }
        for (Map<String,Object> packInfo : packs()) {
            Path owner = pack(String.valueOf(packInfo.get("name")));
            for (Path file : files(owner)) {
                try {
                    Map<String,Object> doc = yaml(new String(read(file), StandardCharsets.UTF_8));
                    if (entries(doc, "items").containsKey(key)) return true;
                } catch (WebError ignored) {}
            }
        }
        return false;
    }

    private void checkRecipeItem(String key) {
        if (!hasRecipeItem(key)) fail(400, "Item no encontrado en los packs cargados: " + key);
    }

    private Map<String,Object> recipeDefinition(Map<String,Object> body) {
        String type = text(body, "type").trim().toLowerCase(Locale.ROOT);
        if (!WEB_RECIPE_TYPES.contains(type)) fail(400, "Tipo de receta no admitido por Crafting Studio");
        String resultId = recipeItemId(text(body, "result"));
        checkRecipeItem(resultId);
        int resultCount = (int) number(body.get("count"), 1, 1, 64);
        Object gridObject = body.get("grid");
        if (!(gridObject instanceof List<?>)) fail(400, "Se esperaba una cuadricula de ingredientes");
        List<?> grid = (List<?>) gridObject;
        if (grid.size() != 9) fail(400, "La cuadricula debe tener exactamente 9 posiciones");
        List<String> slots = new ArrayList<>();
        for (Object raw : grid) {
            String cell = raw == null ? "" : raw.toString().trim();
            if (!cell.isBlank()) {
                cell = recipeItemId(cell);
                checkRecipeItem(cell);
            }
            slots.add(cell);
        }
        LinkedHashMap<String,Object> definition = new LinkedHashMap<>();
        definition.put("type", type);
        if (type.equals("shaped")) {
            Map<String,String> symbols = new LinkedHashMap<>();
            List<String> pattern = new ArrayList<>();
            for (int row = 0; row < 3; row++) {
                StringBuilder line = new StringBuilder(3);
                for (int col = 0; col < 3; col++) {
                    String item = slots.get(row * 3 + col);
                    if (item.isBlank()) line.append(' ');
                    else {
                        String symbol = symbols.get(item);
                        if (symbol == null) {
                            symbol = String.valueOf((char)('A' + symbols.size()));
                            symbols.put(item, symbol);
                        }
                        line.append(symbol);
                    }
                }
                pattern.add(line.toString());
            }
            if (symbols.isEmpty()) fail(400, "Agrega al menos un ingrediente");
            Map<String,String> ingredients = new LinkedHashMap<>();
            for (var symbol : symbols.entrySet()) ingredients.put(symbol.getValue(), symbol.getKey());
            definition.put("pattern", pattern);
            definition.put("ingredients", ingredients);
        } else if (type.equals("shapeless")) {
            List<String> ingredients = slots.stream().filter(v -> !v.isBlank()).toList();
            if (ingredients.isEmpty()) fail(400, "Agrega al menos un ingrediente");
            definition.put("ingredients", ingredients);
        } else {
            if (slots.getFirst().isBlank()) fail(400, "Falta el ingrediente de entrada");
            if (slots.subList(1, 9).stream().anyMatch(v -> !v.isBlank()))
                fail(400, "Este tipo de receta admite un solo ingrediente");
            definition.put("ingredients", slots.getFirst());
            if (WEB_COOKING_TYPES.contains(type)) {
                int time = (int)number(body.get("time"), 200, 1, 72000);
                double exp = number(body.get("experience"), 0, 0, 100);
                definition.put("time", time);
                definition.put("exp", exp);
            }
        }
        definition.put("result", Map.of("id", resultId, "count", resultCount));
        return definition;
    }

    private synchronized Object saveRecipe(Map<String,Object> body, String ref) {
        Entry old = ref == null ? null : findRecipe(ref);
        Path owner = old == null ? pack(text(body, "pack")) : old.pack();
        String recipeId;
        if (old == null) {
            recipeId = packNamespace(owner) + ":" + id(text(body, "id").trim().toLowerCase(Locale.ROOT));
            for (Path file : files(owner)) {
                Map<String,Object> doc = yaml(new String(read(file), StandardCharsets.UTF_8));
                if (entries(doc, "recipes").containsKey(recipeId)) fail(409, "Ya existe una receta con este ID");
            }
        } else {
            recipeId = old.key();
            if (!Objects.equals(text(body, "revision"), digest(old.bytes())))
                fail(409, "La receta cambio. Actualiza antes de guardar");
            String type = text(old.definition(), "type").replace("minecraft:", "");
            if (!WEB_RECIPE_TYPES.contains(type)) fail(400, "Formato existente no compatible con el editor");
        }
        Map<String,Object> definition = recipeDefinition(body);
        String serialized = dump(Map.of("recipes", Map.of(recipeId, definition)));
        yaml(serialized);
        if (old == null) {
            Path target = owner.resolve("configuration/recipes/" + localId(recipeId) + ".yml");
            if (Files.exists(target, LinkOption.NOFOLLOW_LINKS)) fail(409, "Ya existe ese archivo");
            write(target, serialized.getBytes(StandardCharsets.UTF_8), false);
        } else {
            String original = new String(old.bytes(), StandardCharsets.UTF_8);
            String updated = patchOne(original, "recipes", recipeId, serialized.substring(serialized.indexOf('\n')+1));
            yaml(updated);
            backup(old);
            write(old.file(), updated.getBytes(StandardCharsets.UTF_8), true);
        }
        return recipe(owner.getFileName() + "|" + recipeId);
    }

    private synchronized Object deleteRecipe(String ref, Map<String,Object> body) {
        Entry e = findRecipe(ref);
        if (!Objects.equals(text(body, "revision"), digest(e.bytes())))
            fail(409, "La receta cambio. Actualiza antes de eliminar");
        String result = patchOne(new String(e.bytes(), StandardCharsets.UTF_8), "recipes", e.key(), "");
        Map<String,Object> parsed = yaml(result);
        backup(e);
        if (entries(parsed, "recipes").isEmpty() && parsed.keySet().stream().allMatch(k -> k.equals("recipes"))) {
            try { Files.delete(e.file()); } catch(IOException err) { throw new WebError(500, "No se pudo eliminar la receta"); }
        } else {
            write(e.file(), result.getBytes(StandardCharsets.UTF_8), true);
        }
        return Map.of("ok", true);
    }


    // GUI Studio writes the simple inventory YAML consumed by GuiTemplateCommand.
    // It does not replace the built-in GUI/item browser or introduce a new renderer.
    private static String guiId(String raw) {
        String name = id(raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT));
        if (name.length() > 48 || name.contains(".."))
            fail(400, "El ID de GUI debe tener hasta 48 caracteres y no contener ..");
        return name;
    }

    private Entry findGui(String ref) {
        String[] parts = ref.split("\\|", 2);
        if (parts.length != 2) fail(400, "Referencia de GUI invalida");
        Path owner = pack(parts[0]);
        String name = guiId(parts[1]);
        Path file = owner.resolve("configuration/gui/" + name + ".yml");
        if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS) || Files.isSymbolicLink(file)
                || Files.isSymbolicLink(file.getParent())) fail(404, "GUI no encontrada");
        try {
            if (!file.toRealPath().startsWith(resources.toRealPath()))
                fail(403, "Ruta de GUI fuera de los packs");
        } catch (IOException error) { throw new WebError(404, "GUI no encontrada"); }
        byte[] raw = read(file);
        Map<String,Object> doc = yaml(new String(raw, StandardCharsets.UTF_8));
        return new Entry(owner, file, name, doc, doc, raw);
    }

    private Object guiCatalog() {
        List<Object> result = new ArrayList<>();
        for (Map<String,Object> info : packs()) {
            Path owner = pack(String.valueOf(info.get("name")));
            Path folder = owner.resolve("configuration/gui");
            if (!Files.isDirectory(folder, LinkOption.NOFOLLOW_LINKS) || Files.isSymbolicLink(folder)) continue;
            try (var stream = Files.list(folder)) {
                for (Path file : stream.filter(f -> Files.isRegularFile(f, LinkOption.NOFOLLOW_LINKS)
                        && !Files.isSymbolicLink(f) && f.getFileName().toString().matches("[a-z0-9_.-]+\\.yml"))
                        .limit(400).toList()) {
                    try {
                        byte[] raw = read(file);
                        Map<String,Object> doc = yaml(new String(raw, StandardCharsets.UTF_8));
                        String name = guiId(file.getFileName().toString().replaceFirst("\\.yml$", ""));
                        Object buttons = doc.get("buttons");
                        int count = buttons instanceof Map<?,?> m ? m.size() : 0;
                        Object rows = doc.get("rows");
                        if (!(rows instanceof Number n) || n.intValue() < 1 || n.intValue() > 6) continue;
                        result.add(Map.of("ref", owner.getFileName() + "|" + name,
                                "pack", owner.getFileName().toString(),
                                "id", name, "title", text(doc, "title"),
                                "rows", n.intValue(), "buttons", count,
                                "revision", digest(raw)));
                    } catch (WebError ignored) {}
                }
            } catch (IOException e) { throw new WebError(500, "No se pudieron listar las GUIs"); }
        }
        return result;
    }

    private Object gui(String ref) {
        Entry e = findGui(ref);
        Map<String,Object> result = new LinkedHashMap<>(e.document());
        result.put("ref", e.ref());
        result.put("id", e.key());
        result.put("pack", e.pack().getFileName().toString());
        result.put("revision", digest(e.bytes()));
        return result;
    }

    private synchronized Object saveGui(Map<String,Object> body, String ref) {
        Entry old = ref == null ? null : findGui(ref);
        Path owner = old == null ? pack(text(body, "pack")) : old.pack();
        String name = old == null ? guiId(text(body, "id")) : old.key();
        if (old != null && !Objects.equals(digest(old.bytes()), text(body, "revision")))
            fail(409, "La GUI cambio. Actualiza antes de guardar");
        String title = text(body, "title").trim();
        if (title.isBlank() || title.length() > 120) fail(400, "Titulo de GUI invalido (1-120 caracteres)");
        int rows = (int)number(body.get("rows"), 3, 1, 6);
        Object raw = body.get("buttons");
        if (!(raw instanceof Map<?,?>)) fail(400, "Se esperaba el mapa de botones");
        Map<?,?> supplied = (Map<?,?>) raw;
        Map<Integer,Object> buttons = new LinkedHashMap<>();
        for (var entry : supplied.entrySet()) {
            int slot;
            try { slot = Integer.parseInt(String.valueOf(entry.getKey())); }
            catch (NumberFormatException error) { throw new WebError(400, "Casilla de GUI no numerica"); }
            if (slot < 0 || slot >= rows * 9 || !(entry.getValue() instanceof Map<?,?>))
                fail(400, "Casilla fuera del inventario");
            Map<?,?> button = (Map<?,?>) entry.getValue();
            String item = recipeItemId(text(button, "item"));
            checkRecipeItem(item);
            String action = text(button, "action").trim().toLowerCase(Locale.ROOT);
            if (!Set.of("none", "close").contains(action)) fail(400, "Accion de boton no admitida");
            buttons.put(slot, Map.of("item", item, "action", action));
        }
        Map<String,Object> doc = old == null ? new LinkedHashMap<>() : new LinkedHashMap<>(old.document());
        doc.put("title", title);
        doc.put("rows", rows);
        doc.put("buttons", buttons);
        String yamlText = dump(doc);
        yaml(yamlText);
        Path target = owner.resolve("configuration/gui/" + name + ".yml");
        if (old == null) {
            if (Files.exists(target, LinkOption.NOFOLLOW_LINKS)) fail(409, "Ya existe esa GUI");
            write(target, yamlText.getBytes(StandardCharsets.UTF_8), false);
        } else {
            backup(old);
            write(old.file(), yamlText.getBytes(StandardCharsets.UTF_8), true);
        }
        return gui(owner.getFileName() + "|" + name);
    }

    private synchronized Object deleteGui(String ref, Map<String,Object> body) {
        Entry e = findGui(ref);
        if (!Objects.equals(digest(e.bytes()), text(body, "revision")))
            fail(409, "La GUI cambio. Actualiza antes de eliminar");
        backup(e);
        try { Files.delete(e.file()); }
        catch (IOException error) { throw new WebError(500, "No se pudo eliminar la GUI"); }
        return Map.of("ok", true);
    }

    private static void fail(int status,String message){throw new WebError(status,message);}

    private void backup(Entry e){
        try{
            Path dir=backups.resolve(e.pack().getFileName().toString());
            Files.createDirectories(dir);
            String relative=e.pack().relativize(e.file()).toString().replaceAll("[^a-zA-Z0-9_.-]","_");
            Files.copy(e.file(),dir.resolve(System.currentTimeMillis()+"-"+relative+".bak"),
                    StandardCopyOption.REPLACE_EXISTING);
        }catch(IOException err){throw new WebError(500,"Error creando respaldo; cambios cancelados");}
    }

    private void write(Path target,byte[] data,boolean replacing){
        try {
            Path base=resources.toRealPath();
            Path normalized=target.toAbsolutePath().normalize();
            if(!normalized.startsWith(base)||Files.isSymbolicLink(target))fail(403,"Ruta no autorizada");
            Path parent=target.getParent();
            Files.createDirectories(parent);
            for(Path piece=parent;piece!=null&&piece.startsWith(base);piece=piece.getParent())
                if(Files.isSymbolicLink(piece))fail(403,"Ruta simbolica no admitida");
            if(replacing&&!Files.isRegularFile(target,LinkOption.NOFOLLOW_LINKS))fail(404,"Archivo perdido");
            if(!replacing&&Files.exists(target,LinkOption.NOFOLLOW_LINKS))fail(409,"Ya existe archivo");
            Path tmp=Files.createTempFile(parent,".vsrender-web-", ".tmp");
            try{
                Files.write(tmp,data);
                try{Files.move(tmp,target,StandardCopyOption.ATOMIC_MOVE,
                        replacing?StandardCopyOption.REPLACE_EXISTING:StandardCopyOption.ATOMIC_MOVE);}
                catch(AtomicMoveNotSupportedException exc){
                    Files.move(tmp,target, replacing?new java.nio.file.CopyOption[]{StandardCopyOption.REPLACE_EXISTING}:new java.nio.file.CopyOption[]{});
                }
            }finally{Files.deleteIfExists(tmp);}
        }catch(IOException exc){throw new WebError(500,"No se pudo escribir el YAML");}
    }

    private Object docs(String slug){
        Map<String,String> titles=Map.of(
                "introduccion","Introduccion",
                "crear-item","Crear un item",
                "plantillas","Plantillas de cocina",
                "cultivos","Cultivos y semillas",
                "recetas","Recetas y estaciones",
                "platos","Platos con ingredientes variables",
                "gui","Interfaces GUI",
                "seguridad","Seguridad y recarga");
        if(slug==null||slug.isBlank()){
            return titles.entrySet().stream().sorted(Map.Entry.comparingByKey())
                    .map(e->Map.of("slug",e.getKey(),"title",e.getValue())).toList();
        }
        if(!titles.containsKey(slug))fail(404,"Documento no encontrado");
        String body=new String(resource("vsrender-web/docs/"+slug+".md"),StandardCharsets.UTF_8);
        return Map.of("slug",slug,"title",titles.get(slug),"markdown",body);
    }

    private Object templates(){
        return List.of(
                Map.of("id","ingrediente","name","Ingrediente","details","Material, textura, descripcion y apilamiento"),
                Map.of("id","alimento","name","Alimento","details","Nutricion, saturacion, tiempo, animacion y efectos de estado"),
                Map.of("id","bebida","name","Bebida","details","Animacion de beber, efectos, duracion y recipiente de retorno"),
                Map.of("id","plato","name","Plato generico","details","Sopa, guiso, ensalada o tarta con opciones de consumo"),
                Map.of("id","cultivo","name","Verdura cultivable","details","Producto, semillas, cultivo, crecimiento y cosecha"),
                Map.of("id","objeto","name","Objeto simple","details","Material vanilla, textura, lore y apilamiento sin consumo"),
                Map.of("id","herramienta","name","Herramienta o arma","details","Pico, espada o hacha con durabilidad y textura"),
                Map.of("id","equipable","name","Accesorio equipable","details","Ranura de armadura, textura y material"),
                Map.of("id","bloque","name","Bloque sólido","details","Colocación, textura, resistencia, sonido y botín propio"),
                Map.of("id","mineral","name","Mineral de construcción","details","Bloque minable con pico y auto-botín (sin mena variable)"),
                Map.of("id","mueble","name","Mueble sencillo","details","Objeto colocable como display, hitbox e interacción básica"));
    }

    private static final class WebError extends RuntimeException {
        private final int status;
        WebError(int status,String message){super(message);this.status=status;}
    }
}
