#!/usr/bin/env python3
"""Read-only diagnostics for resource packs and datapacks targeting Minecraft Java 26.3.

The auditor never edits models, shaders, or recipes. It must not classify the
entire server as 26.3-compatible: NMS and Folia runtime testing is separate.
"""
import json
from pathlib import Path
import sys

COOKING = frozenset({"smelting", "smoking", "blasting", "campfire_cooking"})
SHADER_EXT = frozenset({".fsh", ".vsh", ".glsl"})
# 26.3 splits context-sensitive number providers into distinct integer and
# floating-point registries. Do not confuse these with context-free worldgen
# int_provider / float_provider.
CONTEXT_PROVIDER_REGISTRIES = frozenset({
    "context_int_provider", "context_float_provider"
})
RENAMED_NUMBER_PROVIDERS = {
    "sum": "add", "product": "mul", "minimum": "min",
    "maximum": "max", "average": "avg",
}
RENAMED_BLOCK_STATE_PROVIDERS = {
    "dual_noise_provider": "dual_noise",
    "noise_provider": "noise",
    "noise_threshold_provider": "noise_threshold",
    "randomized_int_state_provider": "randomized_int",
    "rule_based_state_provider": "rule_based",
    "simple_state_provider": "simple",
    "weighted_state_provider": "weighted",
    "random_block_provider": "random_block",
    "rotated_block_provider": "rotated",
}
# These fields explicitly consume 26.3 context-aware providers. Narrowing the
# check prevents false positives in unrelated JSON / worldgen number providers.
CONTEXT_FIELDS = {
    "rolls": "context_int_provider",
    "bonus_rolls": "context_float_provider",
    "burn_time": "context_int_provider",
    "speed_multiplier": "context_float_provider",
    "uses": "context_int_provider",
    "layers": "context_int_provider",
}
SCAN_DIRS = ("resources", "examples", "common-files/src/main/resources")


def _walk(data):
    """Visit nested JSON nodes without treating arbitrary text as a schema."""
    yield data
    if isinstance(data, dict):
        for value in data.values():
            yield from _walk(value)
    elif isinstance(data, list):
        for value in data:
            yield from _walk(value)


def _datapack_location(relative: Path) -> tuple[str, ...] | None:
    """Return path after data/<namespace> for assets nested in any pack folder."""
    parts = relative.parts
    for i in range(len(parts) - 2):
        if parts[i] == "data":
            return parts[i + 2:]
    return None


def _legacy_registry(relative: Path, location: tuple[str, ...] | None) -> list[str]:
    if not location:
        return []
    if location[0] == "number_provider":
        return [
            f"{relative}: minecraft:number_provider registry was split in 26.3; "
            "move this data file to context_int_provider/ or context_float_provider/ "
            "according to its use (not to worldgen int_provider)"
        ]
    if len(location) >= 2 and location[:2] == ("tags", "number_provider"):
        return [
            f"{relative}: tags/number_provider is obsolete in 26.3; "
            "use tags/context_int_provider or tags/context_float_provider"
        ]
    return []


def _provider_warning(relative: Path, value, registry: str) -> list[str]:
    if not isinstance(value, dict):
        return []
    kind = value.get("type")
    if not isinstance(kind, str):
        # Integer/float literals and resource IDs are legal and handled by
        # vanilla; this warning only concerns obvious old inline objects.
        if "min" in value and "max" in value:
            return [
                f"{relative}: 26.3 {registry} inline uniform provider requires "
                'an explicit type (minecraft:uniform)'
            ]
        return []
    old = kind.split(":")[-1]
    if old in RENAMED_NUMBER_PROVIDERS:
        new = RENAMED_NUMBER_PROVIDERS[old]
        return [
            f"{relative}: 26.3 {registry} renamed provider {kind}; "
            f"use minecraft:{new}"
        ]
    return []


def _check_json(relative: Path, data) -> list[str]:
    warnings = []
    path_parts = set(relative.parts)
    location = _datapack_location(relative)

    if relative.suffix == ".json":
        warnings.extend(_legacy_registry(relative, location))
        if location and location[0] in CONTEXT_PROVIDER_REGISTRIES:
            warnings.extend(_provider_warning(relative, data, location[0]))

    if relative.name == "pack.mcmeta" and isinstance(data, dict):
        pack = data.get("pack", {})
        if isinstance(pack, dict):
            fmt = pack.get("pack_format")
            if isinstance(fmt, int) and fmt < 97 and "supported_formats" not in pack:
                warnings.append(
                    f"{relative}: pack_format={fmt} is older than resource-pack 97.1; "
                    "check supported_formats/min_format/max_format for 26.3"
                )

    if not isinstance(data, (dict, list)):
        return warnings

    if ("recipe" in path_parts or "recipes" in path_parts) and isinstance(data, dict):
        kind = data.get("type")
        if isinstance(kind, str) and kind.split(":")[-1] in COOKING:
            if "cookingtime" not in data:
                warnings.append(f"{relative}: vanilla 26.3 cooking recipe requires cookingtime")
            elif not isinstance(data["cookingtime"], int) or isinstance(data["cookingtime"], bool) or data["cookingtime"] <= 0:
                warnings.append(f"{relative}: cookingtime must be a positive integer")

    if "trim_material" in path_parts and isinstance(data, dict):
        for removed in ("asset_name", "override_armor_assets"):
            if removed in data:
                warnings.append(f"{relative}: legacy trim_material field {removed}; update 26.3 palette/assets")

    # Minecraft 26.3 identifies brewing fuels by item components, not this legacy tag.
    if relative.name == "brewing_fuel.json" and (
        "tags" in path_parts and ("item" in path_parts or "items" in path_parts)
        and "minecraft" in path_parts
    ):
        warnings.append(
            f"{relative}: removed #minecraft:brewing_fuel item tag; "
            "use the minecraft:brewing_fuel item component"
        )

    is_asset = "resourcepack" in path_parts or "assets" in path_parts
    for node in _walk(data):
        if not isinstance(node, dict):
            continue
        if location and location[0] == "loot_table":
            for field in ("rolls", "bonus_rolls"):
                if field in node:
                    warnings.extend(_provider_warning(relative, node[field], CONTEXT_FIELDS[field]))
        # New item fuel and compostable components accept context providers.
        # Match the actual component name, not generic keys like "uses",
        # to avoid reporting unrelated data-driven configuration fields.
        for component, fields in (
            ("cooking_fuel", ("burn_time", "speed_multiplier")),
            ("brewing_fuel", ("uses", "speed_multiplier")),
            ("compostable", ("layers",)),
        ):
            for ns in ("minecraft:" + component, component):
                value = node.get(ns)
                if isinstance(value, dict):
                    for field in fields:
                        if field in value:
                            warnings.extend(
                                _provider_warning(relative, value[field], CONTEXT_FIELDS[field])
                            )
        if location and location[:2] == ("worldgen", "block_state_provider"):
            kind = node.get("type")
            if isinstance(kind, str):
                old = kind.split(":")[-1]
                if old in RENAMED_BLOCK_STATE_PROVIDERS:
                    warnings.append(
                        f"{relative}: 26.3 renamed block state provider {kind}; "
                        f"use minecraft:{RENAMED_BLOCK_STATE_PROVIDERS[old]}"
                    )
        if is_asset and node.get("type") in ("minecraft:map_color", "map_color"):
            warnings.append(f"{relative}: resource-pack map_color tint source was removed in 26.3")
        if is_asset and "models" in path_parts and node.get("shade") is False:
            warnings.append(
                f'{relative}: Minecraft 26.3 removed model element shade=false; '
                'use shade_direction_override="up" instead'
            )
        if "minecraft:block_transformer" in node or "block_transformer" in node:
            transformer = node.get("minecraft:block_transformer", node.get("block_transformer"))
            if isinstance(transformer, (dict, list)):
                warnings.append(
                    f"{relative}: 26.3 block_transformer item component must refer to a registry ID, "
                    "not an inline object/list"
                )
        for key in ("minecraft:pot_decorations", "pot_decorations"):
            if isinstance(node.get(key), list):
                warnings.append(
                    f"{relative}: Minecraft 26.3 pot_decorations requires an object "
                    "with optional back/left/right/front item stacks, not an ID list"
                )
        if "minecraft:swing_animation" in node:
            warnings.append(
                f"{relative}: raw JSON still uses removed swing_animation; "
                "replace with attack_animation and interact_animation"
            )

    return warnings


def check(root: Path) -> list[str]:
    warnings: list[str] = []
    seen: set[Path] = set()
    for folder in SCAN_DIRS:
        base = root / folder
        if not base.is_dir():
            continue
        for path in base.rglob("*"):
            if not path.is_file() or path in seen:
                continue
            seen.add(path)
            relative = path.relative_to(root)
            if path.suffix in SHADER_EXT:
                try:
                    shader = path.read_text(encoding="utf-8")
                except (OSError, UnicodeError):
                    continue
                if "#moj_import" in shader:
                    warnings.append(
                        f"{relative}: shader uses legacy #moj_import (26.3 uses #include)"
                    )
            elif path.suffix == ".json" or path.name == "pack.mcmeta":
                try:
                    data = json.loads(path.read_text(encoding="utf-8"))
                except (OSError, UnicodeError, json.JSONDecodeError):
                    continue
                warnings.extend(_check_json(relative, data))
    return sorted(set(warnings))


def main():
    root = Path(sys.argv[1] if len(sys.argv) > 1 else ".").resolve()
    diagnostics = check(root)
    for warning in diagnostics:
        print(f"::warning::{warning}")
    print(
        f"Minecraft 26.3 asset audit complete: {len(diagnostics)} advisories; "
        "files left unchanged."
    )


if __name__ == "__main__":
    main()
