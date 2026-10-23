#!/usr/bin/env python3
"""Upgrade VSRender's native26 Paper dev bundle to the current 26.3 baseline.

Handles both the original pinned 26.2 Gradle file and the older configurable
26.2 migration format. Leaves the current canonical 26.3 configuration intact,
with no changes to the native rendering code or generated plugin artifacts.
"""
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
NATIVE26_GRADLE = ROOT / "bukkit/native26/build.gradle.kts"
ORIGINAL = '    paperweight.paperDevBundle("26.2.build.123-stable")'
LEGACY_UPDATED = '''    // Keep the original tested 26.2 bundle as the baseline; allow an explicit
    // 26.3 userdev build through -PvsrenderPaperDevBundle=26.3.build.+.
    paperweight.paperDevBundle(
        providers.gradleProperty("vsrenderPaperDevBundle")
            .orElse("26.2.build.123-stable")
            .get()
    )'''
UPDATED = '''    // Compile against Minecraft 26.3 by default; override via
    // -PvsrenderPaperDevBundle when testing an explicit Paper build.
    paperweight.paperDevBundle(
        providers.gradleProperty("vsrenderPaperDevBundle")
            .orElse("26.3.build.+")
            .get()
    )'''


def patch_file(path: Path = NATIVE26_GRADLE) -> bool:
    if not path.is_file():
        raise FileNotFoundError(
            f"Native26 Gradle project is missing: {path}. Import source before patching."
        )
    original = path.read_text(encoding="utf-8")
    if UPDATED in original:
        return False

    for old in (LEGACY_UPDATED, ORIGINAL):
        if original.count(old) == 1:
            path.write_text(original.replace(old, UPDATED), encoding="utf-8")
            return True

    raise ValueError(
        "Unknown native26 Gradle layout: refusing to modify a different upstream version"
    )


if __name__ == "__main__":
    print("native26 dev bundle:", "upgraded to 26.3" if patch_file() else "already set to 26.3")
