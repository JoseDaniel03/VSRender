package com.virgolandstudio.items.core.studio;

import com.virgolandstudio.items.api.behavior.Trigger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** JDK-only behavior/value metadata rendered by Studio. Runtime authority remains BehaviorCompiler. */
public final class ItemBehaviorSchemaCatalog {
    private ItemBehaviorSchemaCatalog() {}

    public static Map<String, Object> describe() {
        var out = new LinkedHashMap<String, Object>();
        out.put("triggers", java.util.Arrays.stream(Trigger.values()).map(Enum::name).toList());
        out.put("conditions", conditions());
        out.put("actions", actions());
        out.put("values", values());
        return Collections.unmodifiableMap(out);
    }

    private static List<Map<String, Object>> conditions() {
        var out = new ArrayList<Map<String, Object>>();
        out.add(entry("has_component", "Tiene componente", "La definición contiene el componente indicado.",
                text("component", "Componente", "", true)));
        out.add(entry("variable_equals", "Variable igual", "Compara una variable temporal del contexto.",
                text("key", "Variable", "", true), json("value", "Valor", null, true)));
        out.add(entry("variable_compare", "Comparar variable", "Comparación numérica contra un Value provider.",
                text("key", "Variable", "", true), operator(), value("value", "Valor esperado", 0D, true)));
        out.add(entry("state_equals", "Estado igual", "Compara un campo persistente de la instancia.",
                text("key", "Clave state", "", true), json("value", "Valor", null, true)));
        out.add(entry("state_compare", "Comparar state", "Comparación numérica de un campo persistente.",
                text("key", "Clave state", "", true), operator(), value("value", "Valor esperado", 0D, true)));
        out.add(entry("state_available", "State disponible", "Comprueba que un recurso numérico alcance una cantidad.",
                text("key", "Clave state", "", true), number("amount", "Cantidad", 1D, true)));
        out.add(entry("state_between", "State entre valores", "Comprueba un rango numérico inclusivo.",
                text("key", "Clave state", "", true), number("min", "Mínimo", 0D, true), number("max", "Máximo", 1D, true)));
        out.add(entry("process_active", "Proceso activo", "Comprueba que Timed Process esté actualmente activo."));
        out.add(entry("process_ready", "Proceso listo", "Comprueba que el deadline del Timed Process ya haya vencido."));
        out.add(entry("reload_can_load_unit", "Puede cargar unidad", "El sistema de recarga puede introducir otra unidad."));
        out.add(entry("reload_can_replace_resource", "Puede reemplazar recurso", "Existe un recurso/batería válido para reemplazo."));
        out.add(entry("block_held", "Bloque sostenido", "El item actualmente sostiene un bloque mediante su bridge."));
        out.add(entry("block_can_grab", "Puede agarrar bloque", "Hay un bloque válido disponible para Block Grab."));
        out.add(entry("entity_can_pull", "Puede atraer entidad", "Hay un objetivo válido para Entity Pull."));
        out.add(entry("cooldown_ready", "Cooldown disponible", "El cooldown indicado ya venció.",
                text("key", "Clave cooldown", "default", false)));
        out.add(entry("energy_has", "Energía disponible", "Comprueba la energía necesaria antes de ejecutar.",
                value("amount", "Cantidad", 0D, false)));
        out.add(entry("consumable_available", "Consumible disponible",
                "Comprueba que el objeto tenga un perfil consumible válido y una dosis disponible."));

        out.add(entry("maintenance_operational", "Mantenimiento: operativo",
                "Comprueba si el estado de mantenimiento permite operar el objeto.",
                toggle("expected", "Debe estar operativo", true)));
        out.add(entry("maintenance_jammed", "Mantenimiento: atascado",
                "Comprueba el estado persistente de atasco mecánico.",
                toggle("expected", "Debe estar atascado", true)));
        out.add(entry("maintenance_metric_between", "Mantenimiento: métrica entre valores",
                "Comprueba una métrica de mantenimiento dentro de un rango inclusivo.",
                choice("metric", "Métrica", "CONDITION", List.of(
                        "DURABILITY", "DURABILITY_MAX",
                        "WEAR", "WEAR_MAX",
                        "DIRT", "DIRT_MAX",
                        "RUST", "RUST_MAX",
                        "LUBRICATION", "LUBRICATION_MAX",
                        "CONDITION", "FAILURE_CHANCE",
                        "FAILURE_COUNT", "MAINTENANCE_COUNT", "LAST_MAINTENANCE_MS"
                )),
                value("min", "Mínimo", 0D, false),
                value("max", "Máximo", 100D, false)));
        return List.copyOf(out);
    }

    private static List<Map<String, Object>> actions() {
        var out = new ArrayList<Map<String, Object>>();
        out.add(entry("set_variable", "Asignar variable", "Asigna una variable temporal del contexto.",
                text("key", "Variable", "", true), value("value", "Valor", "", true)));
        out.add(entry("add_variable", "Sumar variable", "Suma numéricamente sobre una variable temporal.",
                text("key", "Variable", "", true), value("value", "Cantidad", 1D, true)));
        out.add(entry("set_state", "Asignar state", "Asigna un campo persistente de la instancia.",
                text("key", "Clave state", "", true), value("value", "Valor", "", true)));
        out.add(entry("add_state", "Sumar state", "Suma sobre un campo numérico persistente.",
                text("key", "Clave state", "", true), value("value", "Cantidad", 1D, true)));
        out.add(entry("consume_state", "Consumir state", "Consume una cantidad sólo si está disponible.",
                text("key", "Clave state", "", true), number("amount", "Cantidad", 1D, true)));
        out.add(entry("consume_consumable", "Consumir objeto",
                "Ejecuta la transacción consumible atómica; el efecto del actor debe aceptar antes de gastar la dosis.",
                toggle("cancelOnFailure", "Cancelar trigger si falla", true)));
        out.add(entry("toggle_state", "Alternar state", "Alterna un campo booleano persistente.",
                text("key", "Clave state", "", true)));
        out.add(entry("clamp_state", "Limitar state", "Fuerza un state numérico dentro de min/max.",
                text("key", "Clave state", "", true), number("min", "Mínimo", 0D, true), number("max", "Máximo", 1D, true)));
        out.add(entry("process_start", "Iniciar proceso", "Inicia Timed Process usando timestamps; puede sobrescribir id/duración.",
                text("processId", "Process id override", "", false), number("durationSeconds", "Duración override (s)", null, false)));
        out.add(entry("process_advance", "Reconciliar proceso", "Compara el deadline con el reloj actual y completa si corresponde."));
        out.add(entry("process_cancel", "Cancelar proceso", "Detiene Timed Process y limpia sus timestamps pendientes."));

        out.add(entry("reload_detach_container", "Extraer contenedor", "Extrae el cargador/contenedor actual."));
        out.add(entry("reload_insert_reserved_container", "Insertar contenedor", "Inserta el contenedor reservado por la recarga."));
        out.add(entry("feed_chamber", "Alimentar recámara", "Mueve una unidad del contenedor a la recámara."));
        out.add(entry("reload_load_one", "Cargar una unidad", "Carga una unidad en un sistema top-off/per-slot."));
        out.add(entry("reload_replace_resource", "Reemplazar recurso", "Reemplaza batería/recurso durante recarga."));
        out.add(entry("ammo_fire", "Consumir disparo", "Intenta consumir munición y ciclar el mecanismo.",
                toggle("cycle", "Ciclar mecanismo", true)));
        out.add(entry("ammo_feed_one", "Alimentar una unidad", "Intenta alimentar una unidad fuera de la secuencia de recarga."));

        out.add(entry("spawn_projectile", "Crear proyectil", "Crea un proyectil virtual usando el componente Projectile."));
        out.add(entry("spawn_projectiles", "Crear varios proyectiles", "Crea varios proyectiles virtuales con presupuesto acotado.",
                integer("count", "Cantidad", 1L, 1L, 32L, true)));
        out.add(entry("projectile_damage", "Daño de proyectil", "Aplica daño al impacto actual.",
                value("amount", "Daño", 1D, false)));
        out.add(entry("apply_recoil", "Aplicar recoil", "Aplica el perfil Recoil compilado."));
        out.add(entry("block_grab", "Agarrar bloque", "Ejecuta Block Grab mediante el bridge neutral."));
        out.add(entry("block_throw", "Lanzar bloque", "Lanza el bloque sostenido."));
        out.add(entry("block_release", "Soltar bloque", "Suelta el bloque sin lanzarlo."));
        out.add(entry("entity_pull", "Atraer entidad", "Ejecuta Entity Pull mediante el bridge neutral."));
        out.add(entry("impact_damage", "Daño de impacto", "Aplica daño a la entidad impactada.",
                value("amount", "Daño", 1D, false)));
        out.add(entry("impact_knockback", "Knockback de impacto", "Aplica impulso horizontal/vertical.",
                value("horizontal", "Horizontal", 0.8D, false), value("vertical", "Vertical", 0.2D, false)));
        out.add(entry("start_cooldown", "Iniciar cooldown", "Inicia un cooldown expresado en ticks.",
                text("key", "Clave", "default", false), integer("durationTicks", "Ticks", 10L, 0L, 1_000_000L, false)));
        out.add(entry("consume_energy", "Consumir energía", "Consume energía de forma atómica.",
                value("amount", "Cantidad", 0D, false)));

        out.add(entry("maintenance_clean", "Limpiar mantenimiento",
                "Reduce suciedad y óxido de forma independiente.",
                value("dirt", "Suciedad a quitar", 25D, false),
                value("rust", "Óxido a quitar", 0D, false)));
        out.add(entry("maintenance_clean_profile", "Limpiar según perfil",
                "Aplica las cantidades de limpieza definidas por los componentes del objeto."));
        out.add(entry("maintenance_lubricate", "Lubricar",
                "Aumenta la lubricación sin reparar ni limpiar otras dimensiones.",
                value("amount", "Cantidad", 25D, false)));
        out.add(entry("maintenance_repair", "Reparar",
                "Recupera durabilidad según la cantidad resuelta.",
                value("amount", "Cantidad", 25D, false)));
        out.add(entry("maintenance_damage", "Dañar",
                "Aplica daño de durabilidad mediante el servicio genérico de mantenimiento.",
                value("amount", "Cantidad", 1D, false)));
        out.add(entry("maintenance_corrode", "Corroer",
                "Aumenta el óxido/corrosión mediante el servicio genérico de mantenimiento.",
                value("amount", "Cantidad", 1D, false)));
        out.add(entry("maintenance_set_jammed", "Cambiar atasco",
                "Asigna el estado mecánico persistente de atasco.",
                toggle("jammed", "Atascado", true)));
        out.add(entry("maintenance_record", "Registrar mantenimiento",
                "Registra una operación de mantenimiento y actualiza su timestamp."));
        out.add(entry("maintenance_service", "Servicio de mantenimiento",
                "Ejecuta limpieza, desoxidado, lubricación y reparación independientes en una sola operación.",
                value("dirt", "Suciedad a quitar", 0D, false),
                value("rust", "Óxido a quitar", 0D, false),
                value("lubrication", "Lubricación a agregar", 0D, false),
                value("repair", "Durabilidad a reparar", 0D, false),
                toggle("clearJam", "Despejar atasco", false)));

        out.add(entry("spawn_particle", "Partícula", "Emite partículas desde un origen semántico.",
                text("particle", "Partícula", "minecraft:crit", true),
                choice("origin", "Origen", "HIT", List.of("PLAYER", "MUZZLE", "HIT", "BLOCK", "ENTITY")),
                integer("count", "Cantidad", 1L, 1L, 256L, false),
                number("offsetX", "Offset X", 0D, false), number("offsetY", "Offset Y", 0D, false),
                number("offsetZ", "Offset Z", 0D, false), number("extra", "Extra", 0D, false)));
        out.add(entry("play_sound", "Sonido", "Reproduce sonido a través del EffectBridge.",
                text("sound", "Sound id", "", true), text("category", "Categoría", "PLAYERS", false),
                choice("origin", "Origen", "HIT", List.of("PLAYER", "MUZZLE", "HIT", "BLOCK", "ENTITY")),
                number("volume", "Volumen", 1D, false), number("pitch", "Pitch", 1D, false)));

        out.add(entry("set_visual_variant", "Visual: usar variante",
                "Fuerza una variante visual provider-neutral para esta instancia. durationTicks=0 la mantiene hasta limpiarla.",
                text("variant", "Variante", "", true),
                integer("durationTicks", "Duración (ticks)", 0L, 0L, 1_000_000L, false)));
        out.add(entry("set_visual_animation", "Visual: usar animación",
                "Fuerza un payload de animación visual provider-neutral para esta instancia. Puede resolver a sprite, item_model o modelo 3D.",
                text("animation", "Animación", "", true),
                integer("durationTicks", "Duración (ticks)", 0L, 0L, 1_000_000L, false)));
        out.add(entry("clear_visual_override", "Visual: limpiar override",
                "Quita variantes/animaciones temporales y vuelve al visual derivado del estado del item."));

        out.add(entry("stop", "Detener rama", "No ejecuta más acciones de esta rama."));
        out.add(entry("cancel_trigger", "Cancelar trigger", "Cancela la propagación semántica del trigger."));
        return List.copyOf(out);
    }

    private static List<Map<String, Object>> values() {
        return List.of(
                entry("constant", "Constante", "Valor literal.", json("value", "Valor", null, false)),
                entry("stat", "Stat", "Lee un stat compilado de la definición.",
                        text("key", "Stat", "", true), number("fallback", "Fallback", 0D, false)),
                entry("variable", "Variable", "Lee una variable temporal.", text("key", "Variable", "", true)),
                entry("state", "State", "Lee un campo de estado de instancia.", text("key", "Clave state", "", true)),
                entry("process_progress", "Progreso de proceso", "Lee progreso 0..1 del Timed Process actual."),
                entry("curve", "Curva", "Evalúa una curva usando otro Value provider.",
                        json("curve", "Curva", Map.of(), true), value("input", "Entrada", 0D, true))
        );
    }

    @SafeVarargs
    private static Map<String, Object> entry(String id, String name, String description, Map<String, Object>... fields) {
        var out = new LinkedHashMap<String, Object>();
        out.put("id", id);
        out.put("name", name);
        out.put("description", description);
        out.put("fields", List.of(fields));
        return Collections.unmodifiableMap(out);
    }

    private static Map<String, Object> operator() {
        return choice("operator", "Operador", ">=", List.of(">", ">=", "<", "<=", "==", "!="));
    }

    private static Map<String, Object> text(String key, String label, String defaultValue, boolean required) {
        return field(key, label, "STRING", "INPUT", defaultValue, required, null, null, List.of());
    }

    private static Map<String, Object> number(String key, String label, Double defaultValue, boolean required) {
        return field(key, label, "NUMBER", "NUMBER", defaultValue, required, null, null, List.of());
    }

    private static Map<String, Object> integer(String key, String label, Long defaultValue, Long min, Long max, boolean required) {
        return field(key, label, "INTEGER", "NUMBER", defaultValue, required, min, max, List.of());
    }

    private static Map<String, Object> toggle(String key, String label, boolean defaultValue) {
        return field(key, label, "BOOLEAN", "SWITCH", defaultValue, false, null, null, List.of());
    }

    private static Map<String, Object> choice(String key, String label, String defaultValue, List<String> options) {
        return field(key, label, "ENUM", "SELECT", defaultValue, false, null, null, options);
    }

    private static Map<String, Object> json(String key, String label, Object defaultValue, boolean required) {
        return field(key, label, "JSON", "JSON_EDITOR", defaultValue, required, null, null, List.of());
    }

    /** ValueConfig editor. Studio may keep the default as a constant provider and expand it later. */
    private static Map<String, Object> value(String key, String label, Object constantDefault, boolean required) {
        return field(key, label, "VALUE", "VALUE_EDITOR",
                Map.of("type", "constant", "values", Map.of("value", constantDefault)), required, null, null, List.of());
    }

    private static Map<String, Object> field(
            String key,
            String label,
            String type,
            String control,
            Object defaultValue,
            boolean required,
            Number min,
            Number max,
            List<String> options
    ) {
        var out = new LinkedHashMap<String, Object>();
        out.put("key", key);
        out.put("label", label);
        out.put("type", type);
        out.put("control", control);
        out.put("required", required);
        if (defaultValue != null) out.put("default", defaultValue);
        if (min != null) out.put("min", min);
        if (max != null) out.put("max", max);
        out.put("options", options == null ? List.of() : List.copyOf(options));
        return Collections.unmodifiableMap(out);
    }
}
