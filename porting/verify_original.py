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
PATCHED_SHA = {
    "core/src/main/java/net/momirealms/craftengine/core/item/processor/ItemProcessors.java": "5b9223fd05ab282d66f907787914e0887ccb9811",
    "core/src/main/java/net/momirealms/craftengine/core/item/processor/ComponentsProcessor.java": "7c942f64776407bfc2e47b7ea84c8f42dc010b4b",
    "bukkit/native26/build.gradle.kts": "89c142234447f20c928720814b98b1cc71a255e5",
}

# Upstream files intentionally absent from the standalone source checkout:
# obsolete developer notes and third-party compile-only binary dependencies.
# This allowlist applies only to --strict-source; --strict stays exhaustive.
OMITTED_SOURCE_PATHS = frozenset({
    "tasks/todo.md",
    "bukkit/compatibility/legacy/libs/awsm-api.jar",
    "bukkit/compatibility/legacy/libs/flow-nbt-2.0.2.jar",
})


def unexpected_missing(paths: list[str]) -> list[str]:
    return [name for name in paths if name not in OMITTED_SOURCE_PATHS]


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
        elif PATCHED_SHA.get(name) == actual:
            result["patched"].append(name)
        else:
            result["mismatch"].append(name)
    return result


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--strict", action="store_true", help="Exit nonzero for any missing or unexpected changes")
    parser.add_argument("--strict-source", action="store_true", help="Require exact source integrity while permitting only known omitted third-party JARs and obsolete developer notes")
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
    required_missing = unexpected_missing(result["missing"])
    if args.strict_source:
        print(f"VSRender strict-source: required_missing={len(required_missing)}, intentionally_omitted={len(result['missing']) - len(required_missing)}, mismatch={len(result['mismatch'])}")
    if (args.strict and (result["missing"] or result["mismatch"])) or (args.strict_source and (required_missing or result["mismatch"])):
        print("::error::VSRender import is incomplete or unexpectedly modified")
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
