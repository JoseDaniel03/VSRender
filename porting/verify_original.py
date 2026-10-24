#!/usr/bin/env python3
"""Verify exact Git blob identity for the VSRender standalone upstream import.

Only reviewed 26.3 migration changes listed in PATCHED_SHA (plus the hardened
.gitignore) may differ from the pinned upstream. Original submodule metadata is verified separately, while README is
deliberately project-specific. Seven original third-party JARs are tracked with verified upstream hashes.
This checks source migration, NOT compilation or VSEngine 26.3 compatibility.
"""
from __future__ import annotations
import argparse
import hashlib
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
MANIFEST = ROOT / "porting/upstream-manifest"
PATCHED_SHA = {
    # Material snapshot exported for the VSRender web editor from running Minecraft.
    "bukkit/src/main/java/net/momirealms/craftengine/bukkit/plugin/BukkitCraftEngine.java": "ef86066d91852cc2e52e1d24d8625840378a94e7",
    # Workspace compatibility: restore visible root directories on initial startup.
    # Mochila 27 slots: nuevo comportamiento de ítem y registro en el motor existente.
    "bukkit/src/main/java/net/momirealms/craftengine/bukkit/item/behavior/BukkitItemBehaviors.java": "7530772626916d4414ad7c59ee8181ae26c8985f",
    # Tanda 306: object-owned GUI import/export and reviewed storage appearance linking.
    "bukkit/src/main/java/net/momirealms/craftengine/bukkit/block/behavior/SimpleStorageBlockBehavior.java": "41afd6cac38e20c025d43b098ed11c07c0122e63",
    "bukkit/src/main/java/net/momirealms/craftengine/bukkit/entity/furniture/behavior/SimpleStorageFurnitureBehaviorTemplate.java": "14cc57ed55c9b812fe6ad07f2077db653b36a098",
    "bukkit/src/main/java/net/momirealms/craftengine/bukkit/plugin/command/BukkitCommandManager.java": "3fccc33e97ccf73efbe8ca616f676a01f57c7296",
    "common-files/src/main/resources/commands.yml": "f876996c6973a9e9ba34aa49330198d9a00ea565",
    # 26.3 cooking fuel speeds mean smoker/blast defaults must double to preserve behavior.
    "core/src/main/java/net/momirealms/craftengine/core/item/recipe/CustomSmokingRecipe.java": "4b1825fa48037dcaae111bb3fccc2ce0bd2f8bac",
    "core/src/main/java/net/momirealms/craftengine/core/item/recipe/CustomBlastingRecipe.java": "15860dc8991f6a45bc9d25f9644f29a916eb10d8",
    # 26.3 removed model element shade=false; use shade direction override.
    "core/src/main/java/net/momirealms/craftengine/core/pack/model/bbmodel/BBModelConverter.java": "ea1ac705beb8869863cf677398a611f7c3c1aacb",
    # 26.3 Paper NMS: removed ArmorStand's unsupported invulnerability setter.
    "bukkit/native26/src/main/java/net/momirealms/craftengine/bukkit/render/runtime/v26_2/Native26RigCollisionEntity.java": "aac2f0e1f97e098bda32f9ceaf640cbaec31315f",
    # Minecraft 26.3: cooking recipes require explicit cookingtime.
    "core/src/main/java/net/momirealms/craftengine/core/item/recipe/AbstractRecipeSerializer.java": "07c1753165b0ce3b8f0b11afed2e03206bbfc896",
    # Folia async scheduled duration: saturate tick-to-ms conversion.
    "bukkit/src/main/java/net/momirealms/craftengine/bukkit/plugin/scheduler/impl/FoliaExecutor.java": "520c8d602ed4a0c9ef8646449a6defc3966bb692",
    # 26.3 Core needs the original LeafPile API on the compile-only classpath.
    "core/build.gradle.kts": "c95a7a89030ec09bd6b44b1ce8b2fc8c8ec74396",
    # Standalone web import tests require Gson on Bukkit test runtime classpath.
    "bukkit/build.gradle.kts": "2ede0140c655b58b029dd434f59d8cd181181988",
    # Public repository safeguard: do not allow private build JARs into Git.
    ".gitignore": "43f6e28aeaf314a890cfb0fdc496ca1b68da7321",
    # Folia/Canvas 26.3: delayed tasks must preserve their target region.
    "bukkit/src/main/java/net/momirealms/craftengine/bukkit/plugin/scheduler/impl/AbstractBukkitExecutor.java": "07b64a628b04a5172a71f1e68e3a6f8281c6bfa5",
    "core/src/main/java/net/momirealms/craftengine/core/item/processor/ItemProcessors.java": "5b9223fd05ab282d66f907787914e0887ccb9811",
    "core/src/main/java/net/momirealms/craftengine/core/item/processor/ComponentsProcessor.java": "7c942f64776407bfc2e47b7ea84c8f42dc010b4b",
    "bukkit/native26/build.gradle.kts": "f952327cd462709f0645091b5f184aa84c3ba6f6",
}

# The two legacy binary paths are included in the original manifest and now
# tracked with exact upstream Git blob hashes. Five additional JARs are checked
# explicitly in GitHub Actions (outside this original-source manifest).
# This allowlist documents historical exemptions; missing files still fail
# --strict, and --strict-source is used to audit the source import.
OMITTED_SOURCE_PATHS = frozenset({
    "bukkit/compatibility/legacy/libs/awsm-api.jar",
    "bukkit/compatibility/legacy/libs/flow-nbt-2.0.2.jar",
})
UPSTREAM_METADATA_SHA = {
    ".gitmodules": "4facef73050191ebedcf6139fda688694f42b9c0",
}
PROJECT_DOCUMENTS = ("README.md",)


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
    metadata = verify(args.root, UPSTREAM_METADATA_SHA)
    project_docs_missing = [name for name in PROJECT_DOCUMENTS if not (args.root / name).is_file()]
    counts = ", ".join(f"{key}={len(paths)}" for key, paths in result.items())
    print(f"VSRender upstream integrity audit ({len(expected)} tracked files): {counts}")
    for category in ("missing", "mismatch"):
        for name in result[category][:8]:
            print(f"::warning::{category}: {name}")
        if len(result[category]) > 8:
            print(f"::notice::{len(result[category]) - 8} additional {category} paths not printed")
    required_missing = unexpected_missing(result["missing"])
    metadata_issues = metadata["missing"] + metadata["mismatch"]
    if args.strict_source:
        print(
            f"VSRender strict-source: required_missing={len(required_missing)}, "
            f"intentional_binary_exclusions={len(result['missing']) - len(required_missing)}, "
            f"mismatch={len(result['mismatch'])}, original_metadata_issues={len(metadata_issues)}, "
            f"project_docs_missing={len(project_docs_missing)}"
        )
        for name in metadata_issues + project_docs_missing:
            print(f"::error::Missing or modified source metadata: {name}")
    if (args.strict and (result["missing"] or result["mismatch"] or metadata_issues or project_docs_missing)) or (args.strict_source and (required_missing or result["mismatch"] or metadata_issues or project_docs_missing)):
        print("::error::VSRender import is incomplete or unexpectedly modified")
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
