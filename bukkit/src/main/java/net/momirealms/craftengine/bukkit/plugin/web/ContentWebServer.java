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
                        || (origin != null && !origin.equals(ORIGIN))) {
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

    private static String itemSource(String key, String title, String material, Double nutrition, Double sat, int stack) {
        StringBuilder s = new StringBuilder("items:\n  " + key + ":\n    material: " + material
                + "\n    data:\n      item_name: " + quote("<!i><white>" + title) + "\n");
        if (stack != 64) s.append("      max_stack_size: ").append(stack).append("\n");
        if (nutrition != null) s.append("      food:\n        nutrition: ").append(nutrition.intValue())
                .append("\n        saturation: ").append(sat).append("\n        can_always_eat: false\n");
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
            if (entries(doc,"items").containsKey(full)) fail(409,"ID ya existente");
        }
        String template = type == null ? "ingrediente" : text(body,"template");
        if (!Set.of("ingrediente","alimento","bebida","plato").contains(template)) fail(400,"Plantilla no disponible para generacion automatica");
        String material = text(body,"material").replace("minecraft:","");
        if (material.isBlank()) material = switch (template) {
            case "alimento" -> "apple";
            case "bebida" -> "honey_bottle";
            case "plato" -> "beetroot_soup";
            default -> "paper";
        };
        Material vanilla = Material.matchMaterial(material.toUpperCase(Locale.ROOT));
        if (vanilla == null || !vanilla.isItem() || vanilla.isLegacy()) fail(400,"Material no disponible en este servidor: " + material);
        Double nutrition = null, saturation = null;
        if (!template.equals("ingrediente")) {
            nutrition = number(body.get("nutrition"), 4,0,20);
            saturation = number(body.get("saturation"), 1.2,0,20);
        }
        int maxStack = (int)Math.round(number(body.get("max_stack_size"), template.equals("plato")?1:64,1,99));
        if (maxStack > vanilla.getMaxStackSize()) maxStack = vanilla.getMaxStackSize();
        Path target = p.resolve("configuration/items/" + nameId + ".yml");
        if (Files.exists(target,LinkOption.NOFOLLOW_LINKS)) fail(409,"Archivo ya existente");
        write(target,itemSource(full,title,material,nutrition,saturation,maxStack).getBytes(StandardCharsets.UTF_8),false);
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

    private synchronized Object delete(String ref,Map<String,Object> body){
        Entry e=find(ref);
        if(!Objects.equals(text(body,"revision"),digest(e.bytes())))fail(409,"El YAML cambio; actualiza");
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
        }catch(RuntimeException ex){throw ex;}
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
        Map<?,?> contents=entries(doc,"items");
        if(contents.size()!=1)fail(400,"Debe contener exactamente un item en items:");
        Object k=contents.keySet().iterator().next();
        if(!(k instanceof String))fail(400,"Item o ID invalido");
        String full = (String) k;
        Object candidate = contents.get(k);
        if(!ITEM_ID.matcher(full).matches() || !(candidate instanceof Map<?,?>))
            fail(400,"Item o ID invalido");
        Map<?,?> def = (Map<?,?>) candidate;
        String local=localId(full);
        for(Path f:files(p))if(entries(yaml(new String(read(f),StandardCharsets.UTF_8)),"items").containsKey(full))
            fail(409,"Este item ya existe");
        Path out=p.resolve("configuration/"+kind(def)+"/"+local+".yml");
        if(Files.exists(out,LinkOption.NOFOLLOW_LINKS))fail(409,"Archivo ya existente");
        write(out,dump(Map.of("items",Map.of(full,def))).getBytes(StandardCharsets.UTF_8),false);
        return item(p.getFileName()+"|"+full);
    }

    private byte[] exportItem(String ref){
        Entry e=find(ref);
        try{
            ByteArrayOutputStream bytes=new ByteArrayOutputStream();
            try(ZipOutputStream zip=new ZipOutputStream(bytes)){
                zip.putNextEntry(new ZipEntry("configuration/items/"+localId(e.key())+".yml"));
                zip.write(dump(Map.of("items",Map.of(e.key(),e.definition()))).getBytes(StandardCharsets.UTF_8));
                zip.closeEntry();
                zip.putNextEntry(new ZipEntry("manifest.json"));
                zip.write(JSON.toJson(Map.of("format","vsrender-item-export-v1","id",e.key(),"pack",e.pack().getFileName().toString())).getBytes(StandardCharsets.UTF_8));
                zip.closeEntry();
            }
            return bytes.toByteArray();
        }catch(IOException x){throw new WebError(500,"No se pudo exportar");}
    }

    private byte[] thumbnail(String ref){
        Entry e=find(ref);
        Object model=e.definition().get("model");
        Object generation=model instanceof Map<?,?> m ? m.get("generation"):null;
        Object tex=generation instanceof Map<?,?> m?m.get("textures"):null;
        Object layer=tex instanceof Map<?,?> m?m.get("layer0"):null;
        if(!(layer instanceof String v)||!v.matches("[a-z0-9_.-]+:[a-z0-9_./-]+")||v.contains(".."))return null;
        String[] s=v.split(":",2);
        Path path=e.pack().resolve("resourcepack/assets/"+s[0]+"/textures/"+s[1]+".png").normalize();
        if(!path.startsWith(e.pack())||!Files.isRegularFile(path,LinkOption.NOFOLLOW_LINKS))return null;
        try{return Files.size(path)<500_000 ? Files.readAllBytes(path):null;}catch(IOException err){return null;}
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
                Map.of("id","ingrediente","name","Ingrediente","details","Material, nombre y apilamiento"),
                Map.of("id","alimento","name","Alimento","details","Nutricion y saturacion"),
                Map.of("id","bebida","name","Bebida","details","Consumible con valores base"),
                Map.of("id","plato","name","Plato generico","details","Un solo ID para guiso, sopa, tarta o ensalada"));
    }

    private static final class WebError extends RuntimeException {
        private final int status;
        WebError(int status,String message){super(message);this.status=status;}
    }
}
