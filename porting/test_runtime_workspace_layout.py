"""Workspace folders appear on first VSRender launch without creating a second filesystem system."""

from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "bukkit/src/main/java/net/momirealms/craftengine/bukkit/plugin/BukkitCraftEngine.java"

class WorkspaceLayoutTests(unittest.TestCase):
    def test_layout_is_created_after_original_default_pack_extraction(self):
        source = SOURCE.read_text(encoding="utf-8")
        setup = source[source.index("public void setUpConfigAndLocale()"):source.index("// 这个方法应该尽早被执行")]
        self.assertLess(setup.index("super.packManager = new BukkitPackManager(this);"),
                        setup.index("this.ensureWorkspaceDirectories();"))

    def test_folders_are_idempotent_and_named_for_original_workspace(self):
        source = SOURCE.read_text(encoding="utf-8")
        for name in ("generated", "import", "library", "native-assets", "extensions",
                     "content-data", "cache", "export", "libs", "translations", "resources"):
            self.assertIn('"' + name + '"', source)
        self.assertIn("Files.createDirectories(this.dataFolderPath.resolve(name))", source)
        self.assertNotIn("deleteIfExists", source[source.index("private void ensureWorkspaceDirectories()"):
                                                    source.index("public void setUpConfigAndLocale()")])

if __name__ == "__main__":
    unittest.main()
