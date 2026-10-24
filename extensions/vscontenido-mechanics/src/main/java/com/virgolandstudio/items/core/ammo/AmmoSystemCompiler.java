package com.virgolandstudio.items.core.ammo;

import com.virgolandstudio.items.api.NamespacedId;
import com.virgolandstudio.items.api.ammo.AmmoContainerKind;
import com.virgolandstudio.items.api.ammo.AmmoStorageMode;
import com.virgolandstudio.items.api.ammo.FeedMechanismKind;
import com.virgolandstudio.items.api.ammo.ReloadMechanismKind;
import com.virgolandstudio.items.api.component.ComponentConfig;
import com.virgolandstudio.items.api.definition.ItemDefinition;
import com.virgolandstudio.items.core.compile.ValidationException;
import com.virgolandstudio.items.core.registry.BuiltinSchemas;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Resolves author-facing ammo/container/feed/reload components into one immutable runtime spec. */
public final class AmmoSystemCompiler {
    public CompiledAmmoSystem compile(ItemDefinition definition) {
        ComponentConfig ammo = component(definition, BuiltinSchemas.AMMO);
        ComponentConfig container = component(definition, BuiltinSchemas.AMMO_CONTAINER);
        ComponentConfig feed = component(definition, BuiltinSchemas.FEED);
        ComponentConfig chamber = component(definition, BuiltinSchemas.CHAMBER);
        ComponentConfig reload = component(definition, BuiltinSchemas.RELOAD);

        if (ammo == null && container == null && feed == null && chamber == null && reload == null) return null;
        // A standalone loose-ammunition item may declare only Ammo. It is a reload resource, not an operational ammo system.
        if (ammo != null && container == null && feed == null && chamber == null && reload == null) return null;

        var errors = new ArrayList<String>();
        if (ammo == null) errors.add("Ammo subsystem requires component " + BuiltinSchemas.AMMO);
        if (container == null) errors.add("Ammo subsystem requires component " + BuiltinSchemas.AMMO_CONTAINER);
        if (!errors.isEmpty()) throw new ValidationException(errors);

        String ammoType = string(ammo, "ammoType", "generic");
        AmmoContainerKind kind = enumValue(container, "kind", AmmoContainerKind.class, AmmoContainerKind.DETACHABLE_MAGAZINE);
        AmmoStorageMode storage = enumValue(container, "storage", AmmoStorageMode.class, defaultStorage(kind));
        int capacity = positiveInt(container, "capacity", 1, errors);
        boolean detachable = bool(container, "detachable", defaultDetachable(kind));
        boolean operational = feed != null || chamber != null || reload != null;
        FeedMechanismKind feedKind = feed == null
                ? defaultFeed(kind)
                : enumValue(feed, "mode", FeedMechanismKind.class, defaultFeed(kind));
        int chamberCapacity = chamber == null ? 0 : nonNegativeInt(chamber, "capacity", 1, errors);
        ReloadMechanismKind reloadKind = reload == null
                ? (operational ? defaultReload(kind) : ReloadMechanismKind.CUSTOM)
                : enumValue(reload, "mechanism", ReloadMechanismKind.class, defaultReload(kind));

        validateCombination(kind, storage, detachable, feedKind, chamberCapacity, reloadKind, operational, errors);
        if (!errors.isEmpty()) throw new ValidationException(errors);

        return new CompiledAmmoSystem(
                ammoType, kind, storage, capacity, detachable, feedKind, chamberCapacity, reloadKind
        );
    }

    private static void validateCombination(
            AmmoContainerKind kind,
            AmmoStorageMode storage,
            boolean detachable,
            FeedMechanismKind feed,
            int chamberCapacity,
            ReloadMechanismKind reload,
            boolean operational,
            List<String> errors
    ) {
        if (kind == AmmoContainerKind.CYLINDER && storage != AmmoStorageMode.SLOTS) {
            errors.add("CYLINDER requires storage=SLOTS");
        }
        if ((kind == AmmoContainerKind.BREAK_ACTION || kind == AmmoContainerKind.SINGLE_CHAMBER)
                && storage != AmmoStorageMode.SLOTS) {
            errors.add(kind + " requires storage=SLOTS");
        }
        if (kind == AmmoContainerKind.BATTERY && storage != AmmoStorageMode.RESOURCE) {
            errors.add("BATTERY requires storage=RESOURCE");
        }
        if (!operational) return;
        if (feed == FeedMechanismKind.ROTATING && kind != AmmoContainerKind.CYLINDER) {
            errors.add("ROTATING feed requires CYLINDER container");
        }
        if (feed == FeedMechanismKind.ENERGY_DIRECT && kind != AmmoContainerKind.BATTERY) {
            errors.add("ENERGY_DIRECT feed requires BATTERY container");
        }
        if (feed == FeedMechanismKind.AUTO_CHAMBER || feed == FeedMechanismKind.MANUAL_CHAMBER) {
            if (chamberCapacity < 1) errors.add(feed + " requires a Chamber component with capacity >= 1");
        }
        if ((feed == FeedMechanismKind.DIRECT || feed == FeedMechanismKind.ROTATING || feed == FeedMechanismKind.ENERGY_DIRECT)
                && chamberCapacity > 0 && kind != AmmoContainerKind.BREAK_ACTION && kind != AmmoContainerKind.SINGLE_CHAMBER) {
            errors.add(feed + " normally does not use a separate Chamber component");
        }
        if (reload == ReloadMechanismKind.SWAP_CONTAINER && !detachable) {
            errors.add("SWAP_CONTAINER reload requires detachable=true");
        }
        if (reload == ReloadMechanismKind.TOP_OFF && detachable && kind == AmmoContainerKind.DETACHABLE_MAGAZINE) {
            errors.add("TOP_OFF is intended for an embedded/non-detachable container");
        }
        if (reload == ReloadMechanismKind.PER_SLOT && storage != AmmoStorageMode.SLOTS) {
            errors.add("PER_SLOT reload requires storage=SLOTS");
        }
        if (reload == ReloadMechanismKind.REPLACE_RESOURCE && storage != AmmoStorageMode.RESOURCE) {
            errors.add("REPLACE_RESOURCE reload requires storage=RESOURCE");
        }
    }

    private static AmmoStorageMode defaultStorage(AmmoContainerKind kind) {
        return switch (kind) {
            case CYLINDER, BREAK_ACTION, SINGLE_CHAMBER -> AmmoStorageMode.SLOTS;
            case BATTERY -> AmmoStorageMode.RESOURCE;
            case DETACHABLE_MAGAZINE, INTERNAL_MAGAZINE, BELT -> AmmoStorageMode.COUNT;
        };
    }

    private static boolean defaultDetachable(AmmoContainerKind kind) {
        return switch (kind) {
            case DETACHABLE_MAGAZINE, BATTERY, BELT -> true;
            case INTERNAL_MAGAZINE, CYLINDER, BREAK_ACTION, SINGLE_CHAMBER -> false;
        };
    }

    private static FeedMechanismKind defaultFeed(AmmoContainerKind kind) {
        return switch (kind) {
            case DETACHABLE_MAGAZINE, INTERNAL_MAGAZINE, BELT -> FeedMechanismKind.AUTO_CHAMBER;
            case CYLINDER -> FeedMechanismKind.ROTATING;
            case BREAK_ACTION, SINGLE_CHAMBER -> FeedMechanismKind.DIRECT;
            case BATTERY -> FeedMechanismKind.ENERGY_DIRECT;
        };
    }

    private static ReloadMechanismKind defaultReload(AmmoContainerKind kind) {
        return switch (kind) {
            case DETACHABLE_MAGAZINE, BELT -> ReloadMechanismKind.SWAP_CONTAINER;
            case INTERNAL_MAGAZINE -> ReloadMechanismKind.TOP_OFF;
            case CYLINDER, BREAK_ACTION, SINGLE_CHAMBER -> ReloadMechanismKind.PER_SLOT;
            case BATTERY -> ReloadMechanismKind.REPLACE_RESOURCE;
        };
    }

    private static ComponentConfig component(ItemDefinition definition, NamespacedId id) {
        for (ComponentConfig component : definition.components()) if (component.type().equals(id)) return component;
        return null;
    }

    private static String string(ComponentConfig config, String key, String fallback) {
        Object raw = config.values().get(key);
        return raw instanceof String string && !string.isBlank() ? string : fallback;
    }

    private static boolean bool(ComponentConfig config, String key, boolean fallback) {
        Object raw = config.values().get(key);
        return raw instanceof Boolean value ? value : fallback;
    }

    private static int positiveInt(ComponentConfig config, String key, int fallback, List<String> errors) {
        int value = intValue(config, key, fallback);
        if (value < 1) errors.add(config.type() + "." + key + " must be >= 1");
        return Math.max(1, value);
    }

    private static int nonNegativeInt(ComponentConfig config, String key, int fallback, List<String> errors) {
        int value = intValue(config, key, fallback);
        if (value < 0) errors.add(config.type() + "." + key + " must be >= 0");
        return Math.max(0, value);
    }

    private static int intValue(ComponentConfig config, String key, int fallback) {
        Object raw = config.values().get(key);
        return raw instanceof Number number ? (int) Math.round(number.doubleValue()) : fallback;
    }

    private static <E extends Enum<E>> E enumValue(ComponentConfig config, String key, Class<E> type, E fallback) {
        Object raw = config.values().get(key);
        if (!(raw instanceof String text) || text.isBlank()) return fallback;
        try {
            return Enum.valueOf(type, text.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new ValidationException(List.of(config.type() + "." + key + " has unsupported value: " + text));
        }
    }
}
