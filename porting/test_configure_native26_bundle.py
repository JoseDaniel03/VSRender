import tempfile
import unittest
from pathlib import Path

import configure_native26_bundle as patch


class Native26BundleTests(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.path = Path(self.tmp.name) / "bukkit/native26/build.gradle.kts"
        self.path.parent.mkdir(parents=True)
        self.path.write_text('dependencies {\n'+patch.ORIGINAL+'\n}\n',encoding="utf-8")

    def tearDown(self):
        self.tmp.cleanup()

    def test_adds_property_keeping_original_baseline(self):
        self.assertTrue(patch.patch_file(self.path))
        text = self.path.read_text()
        self.assertIn('vsrenderPaperDevBundle', text)
        self.assertIn('26.3.build.+', text)
        self.assertIn('.orElse("26.2.build.123-stable")', text)

    def test_idempotent(self):
        patch.patch_file(self.path)
        initial = self.path.read_bytes()
        self.assertFalse(patch.patch_file(self.path))
        self.assertEqual(initial, self.path.read_bytes())

    def test_fails_closed_when_upstream_changes(self):
        self.path.write_text('dependencies { paperweight.paperDevBundle("custom") }')
        original = self.path.read_bytes()
        with self.assertRaises(ValueError):
            patch.patch_file(self.path)
        self.assertEqual(original, self.path.read_bytes())

    def test_requires_native26_source(self):
        self.path.unlink()
        with self.assertRaises(FileNotFoundError):
            patch.patch_file(self.path)


if __name__ == "__main__":
    unittest.main()
