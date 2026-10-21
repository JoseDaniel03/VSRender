#!/usr/bin/env python3
"""Read-only compatibility audit for Minecraft Java 26.3 assets.

Diagnostics only: never rewrite custom shaders or datapacks automatically.
"""
from pathlib import Path
import json
import sys

COOKING={"minecraft:smelting","minecraft:smoking","minecraft:blasting","minecraft:campfire_cooking"}
SHADER_EXT={".fsh",".vsh",".glsl"}
SCAN_DIRS=("resources","examples","common-files/src/main/resources")
def check(root: Path) -> list[str]:
    warnings=[]
    for directory in SCAN_DIRS:
        base=root/directory
        if not base.is_dir():
            continue
        for p in base.rglob("*"):
            if not p.is_file():
                continue
            relative=p.relative_to(root)
            if p.suffix in SHADER_EXT:
                try:
                    shader=p.read_text(encoding="utf-8")
                except (OSError, UnicodeError):
                    continue
                if "#moj_import" in shader:
                    warnings.append(f"{relative}: shader uses legacy #moj_import (26.3 uses #include)")
            if p.suffix==".json" and ("recipe" in p.parts):
                try:
                    recipe=json.loads(p.read_text(encoding="utf-8"))
                except (OSError, UnicodeError, json.JSONDecodeError):
                    continue
                if isinstance(recipe,dict) and recipe.get("type") in COOKING and "cookingtime" not in recipe:
                    warnings.append(f"{relative}: vanilla 26.3 cooking recipe requires cookingtime")
    return warnings

def main():
    root=Path(sys.argv[1] if len(sys.argv)>1 else ".").resolve()
    for warning in check(root):
        print(f"::warning::{warning}")
    print("Minecraft 26.3 asset audit finished. Reports are advisory; no files changed.")

if __name__=="__main__": main()
