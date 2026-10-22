import hashlib
import json
import tempfile
import unittest
from pathlib import Path

from unittest.mock import patch

from verify_original import PATCHED_SHA, blob_hash, read_manifest, verify


class OriginalImportVerifierTests(unittest.TestCase):
    def test_git_blob_sha_matches_git_protocol(self):
        with tempfile.TemporaryDirectory() as tmp:
            path = Path(tmp) / "text.txt"
            payload = b"VSRender\n"
            path.write_bytes(payload)
            expected = hashlib.sha1(b"blob " + str(len(payload)).encode() + b"\0" + payload).hexdigest()
            self.assertEqual(expected, blob_hash(path))

    def test_missing_and_corrupted_files(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            present = root / "sample.txt"
            present.write_text("before")
            result = verify(root, {"sample.txt": blob_hash(present), "missing.txt": "0" * 40})
            self.assertEqual(["sample.txt"], result["identical"])
            self.assertEqual(["missing.txt"], result["missing"])
            present.write_text("after")
            result = verify(root, {"sample.txt": hashlib.sha1(b"blob 6\0before").hexdigest()})
            self.assertEqual(["sample.txt"], result["mismatch"])

    def test_only_allowlisted_files_can_differ(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            path = root / "bukkit/native26/build.gradle.kts"
            path.parent.mkdir(parents=True)
            path.write_text("26.3 patched")
            with patch.dict(PATCHED_SHA, {"bukkit/native26/build.gradle.kts": blob_hash(path)}):
                result = verify(root, {"bukkit/native26/build.gradle.kts": "0" * 40})
            self.assertEqual(["bukkit/native26/build.gradle.kts"], result["patched"])

    def test_unrecognized_changes_to_allowlisted_file_fail(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            path = root / "bukkit/native26/build.gradle.kts"
            path.parent.mkdir(parents=True)
            path.write_text("modified without approval")
            result = verify(root, {"bukkit/native26/build.gradle.kts": "0" * 40})
            self.assertEqual(["bukkit/native26/build.gradle.kts"], result["mismatch"])

    def test_manifest_must_not_mix_revisions(self):
        with tempfile.TemporaryDirectory() as tmp:
            folder = Path(tmp)
            (folder / "00.json").write_text(json.dumps({"source_tree": "first", "files": {"a": "aa"}}))
            (folder / "01.json").write_text(json.dumps({"source_tree": "second", "files": {"b": "bb"}}))
            with self.assertRaises(ValueError):
                read_manifest(folder)

    def test_manifest_rejects_directory_escape(self):
        with tempfile.TemporaryDirectory() as tmp:
            folder = Path(tmp)
            (folder / "00.json").write_text(json.dumps({"source_tree": "first", "files": {"../other": "aa"}}))
            with self.assertRaises(ValueError):
                read_manifest(folder)


if __name__ == "__main__":
    unittest.main()
