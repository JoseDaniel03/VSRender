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


def _check_json(relative: Path, data) -> list[str]:
    warnings = []
    path_parts = set(relative.parts)

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

    is_asset = "resourcepack" in path_parts or "assets" in path_parts
    for node in _walk(data):
        if not isinstance(node, dict):
            continue
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
