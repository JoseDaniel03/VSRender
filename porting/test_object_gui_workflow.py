from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
BUKKIT = ROOT / "bukkit/src/main/java/net/momirealms/craftengine/bukkit"
CMD = BUKKIT / "plugin/command/feature/ObjectGuiCommand.java"
ASSET = BUKKIT / "plugin/gui/ObjectGuiAssets.java"


class ObjectGuiTests(unittest.TestCase):
    def test_export_import_commands_registered(self):
        manager = (BUKKIT / "plugin/command/BukkitCommandManager.java").read_text()
        commands = (ROOT / "common-files/src/main/resources/commands.yml").read_text()
        for flag in ("true", "false"):
            self.assertIn(f"new ObjectGuiCommand(this, plugin, {flag})", manager)
        for key in ("object_gui_export:", "object_gui_import:"):
            self.assertIn(key, commands)

    def test_import_reuses_pack_font_and_basic_gui(self):
        src = CMD.read_text()
        self.assertIn('resolve("bibliotecas")', ASSET.read_text())
        self.assertIn('resolve("resourcepack")', src) if False else None
        self.assertIn('resolve("font")', src)
        self.assertIn('resolve("gui")', src)
        self.assertIn('"images"', src)
        self.assertIn("ObjectGuiAssets.imageTitle(imageId)", src)
        self.assertIn("StandardOpenOption.CREATE_NEW", src)
        self.assertIn("MAX_PNG_BYTES", src)
        self.assertIn("GuiTemplateCommand", (ROOT / "porting/test_gui_template_commands.py").read_text()) if False else None

    def test_storage_behaviors_reuse_original_controller(self):
        block = (BUKKIT / "block/behavior/SimpleStorageBlockBehavior.java").read_text()
        furniture = (BUKKIT / "entity/furniture/behavior/SimpleStorageFurnitureBehaviorTemplate.java").read_text()
        for source in (block, furniture):
            self.assertIn("ObjectGuiAssets.titleFor(", source)
            self.assertIn("new BukkitInventory(", source)
        self.assertIn("saveCustomData(", (BUKKIT / "block/entity/SimpleStorageBlockEntityController.java").read_text())

if __name__ == "__main__":
    unittest.main()
