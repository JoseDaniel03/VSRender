#!/usr/bin/env python3
"""Verify exact Git blob identity for the VSRender standalone upstream import.

Only three Java/Gradle files are allowed to differ: they receive documented
Minecraft 26.3 patches. This verifies file presence and integrity, NOT that the
whole plugin compiles or is compatible with VSEngine 26.3.
"""
from __future__ import annotations
import argparse
import hashlib
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
MANIFEST = ROOT / "porting/upstream-manifest"
PATCHED = frozenset({
    "core/src/main/java/net/momirealms/craftengine/core/item/processor/ItemProcessors.java",
    "core/src/main/java/net/momirealms/craftengine/core/item/processor/ComponentsProcessor.java",
    "bukkit/native26/build.gradle.kts",
})


def blob_hash(path: Path) -> str:
    digest = hashlib.sha1()
    digest.update(f"blob {path.stat().st_size}\0".encode("ascii"))
    with path.open("rb") as stream:
        for chunk in iter(lambda: stream.read(128 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def read_manifest(folder: Path = MANIFEST) -> dict[str, str]:
    entries: dict[str, str] = {}
    source_tree = None
    shards = sorted(folder.glob("*.json"))
    if not shards:
        raise ValueError(f"Missing upstream manifests in {folder}")
    for shard in shards:
        data = json.loads(shard.read_text(encoding="utf-8"))
        tree = data["source_tree"]
        if source_tree is None:
            source_tree = tree
        if tree != source_tree:
            raise ValueError("Mixed upstream tree revisions in manifest shards")
        for name, sha in data["files"].items():
            if name in entries:
                raise ValueError(f"Duplicated upstream manifest path: {name}")
            if name.startswith("/") or ".." in Path(name).parts:
                raise ValueError(f"Unsafe upstream manifest path: {name}")
            entries[name] = sha
    return entries


def verify(root: Path, expected: dict[str, str]) -> dict[str, list[str]]:
    result = {"identical": [], "patched": [], "missing": [], "mismatch": []}
    for name, digest in sorted(expected.items()):
        path = root / name
        if not path.is_file() or path.is_symlink():
            result["missing"].append(name)
            continue
        actual = blob_hash(path)
        if actual == digest:
            result["identical"].append(name)
        elif name in PATCHED:
            result["patched"].append(name)
        else:
            result["mismatch"].append(name)
    return result


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--strict", action="store_true", help="Exit nonzero for any missing or unexpected changes")
    parser.add_argument("--root", type=Path, default=ROOT)
    args = parser.parse_args()
    expected = read_manifest()
    result = verify(args.root, expected)
    counts = ", ".join(f"{key}={len(paths)}" for key, paths in result.items())
    print(f"VSRender upstream integrity audit ({len(expected)} tracked files): {counts}")
    for category in ("missing", "mismatch"):
        for name in result[category][:8]:
            print(f"::warning::{category}: {name}")
        if len(result[category]) > 8:
            print(f"::notice::{len(result[category]) - 8} additional {category} paths not printed")
    if args.strict and (result["missing"] or result["mismatch"]):
        print("::error::VSRender import is incomplete or unexpectedly modified")
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
