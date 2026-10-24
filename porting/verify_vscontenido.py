#!/usr/bin/env python3
"""Verify that the complete vendor API/Core snapshot matches its original Git blobs."""
from __future__ import annotations
import hashlib
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
MANIFEST = ROOT / "porting/vscontenido-source-manifest.json"
PREFIX = "extensions/vscontenido-mechanics/src/main/java/"


def blob_sha(data: bytes) -> str:
    return hashlib.sha1(b"blob " + str(len(data)).encode("ascii") + b"\0" + data).hexdigest()


def main() -> None:
    data = json.loads(MANIFEST.read_text(encoding="utf-8"))
    expected = data["java_blobs"]
    if len(expected) != 222:
        raise SystemExit("VSContenido manifest must contain 222 API/Core Java sources")
    counts = {"api": 0, "core": 0}
    for name, sha in expected.items():
        path = ROOT / name
        if not name.startswith(PREFIX) or not name.endswith(".java") or ".." in name:
            raise SystemExit("Unsafe source path in manifest: " + name)
        if not path.is_file():
            raise SystemExit("Missing original VSContenido source: " + name)
        actual = blob_sha(path.read_bytes())
        if actual != sha:
            raise SystemExit("VSContenido source diverged from the pinned upstream: " + name)
        if "/items/api/" in name:
            counts["api"] += 1
        elif "/items/core/" in name:
            counts["core"] += 1
        else:
            raise SystemExit("Unexpected VSContenido source package: " + name)
    if counts != {"api": 49, "core": 173}:
        raise SystemExit(f"Unexpected source category counts: {counts}")
    available = {p.relative_to(ROOT).as_posix() for p in (ROOT / PREFIX).rglob("*.java")}
    extra = available - set(expected)
    expected_own = {
        PREFIX + "com/virgolandstudio/items/core/capability/VsContenidoCapabilities.java",
        PREFIX + "com/virgolandstudio/items/core/capability/VsContenidoDefinitions.java"
    }
    if extra != expected_own:
        raise SystemExit(f"Unexpected added or missing extension-owned Java sources: {sorted(extra ^ expected_own)}")
    print("VSContenido source fidelity PASS: 49 API + 173 Core + 2 VSRender capability classes")


if __name__ == "__main__":
    main()
