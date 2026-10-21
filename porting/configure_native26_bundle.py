#!/usr/bin/env python3
"""Make VSRender's native26 Paper dev bundle selectable without changing its 26.2 baseline.

Runs after importing the complete original module into this standalone repository.
The original pinned dev bundle is retained as the default; CI can opt into Paper
26.3 to reveal NMS API changes before claiming runtime compatibility.
"""
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
NATIVE26_GRADLE = ROOT / "bukkit/native26/build.gradle.kts"
ORIGINAL = '    paperweight.paperDevBundle("26.2.build.123-stable")'
UPDATED = '''    // Keep the original tested 26.2 bundle as the baseline; allow an explicit
    // 26.3 userdev build through -PvsrenderPaperDevBundle=26.3.build.+.
    paperweight.paperDevBundle(
        providers.gradleProperty("vsrenderPaperDevBundle")
            .orElse("26.2.build.123-stable")
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
    if original.count(ORIGINAL) != 1:
        raise ValueError(
            "Unknown native26 Gradle layout: refusing to modify a different upstream version"
        )
    path.write_text(original.replace(ORIGINAL, UPDATED), encoding="utf-8")
    return True


if __name__ == "__main__":
    print("native26 dev bundle configurable:", "patched" if patch_file() else "already configured")
