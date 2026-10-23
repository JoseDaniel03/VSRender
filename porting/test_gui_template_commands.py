from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "bukkit/src/main/java/net/momirealms/craftengine/bukkit/plugin/command/feature/GuiTemplateCommand.java"

class GuiTemplateTests(unittest.TestCase):
    def test_commands_registered_with_permission(self):
        reg = (ROOT / "bukkit/src/main/java/net/momirealms/craftengine/bukkit/plugin/command/BukkitCommandManager.java").read_text()
        config = (ROOT / "common-files/src/main/resources/commands.yml").read_text()
        self.assertIn("new GuiTemplateCommand(this, plugin, true)", reg)
        self.assertIn("new GuiTemplateCommand(this, plugin, false)", reg)
        self.assertIn("create_gui:", config)
        self.assertIn("open_gui:", config)
        self.assertIn("ce.command.admin.gui", config)

    def test_command_keeps_current_basic_gui_and_safe_templates(self):
        src = SOURCE.read_text()
        self.assertIn("BasicGui.builder()", src)
        self.assertIn("new GuiLayout(grid)", src)
        self.assertIn("StandardOpenOption.CREATE_NEW", src)
        self.assertIn("rows < 1 || rows > 6", src)
        self.assertIn("MAX_TEMPLATE_BYTES", src)
        self.assertIn("name + \".yml\"", src)
        self.assertIn("action.equals(\"close\")", src)

if __name__ == "__main__":
    unittest.main()
