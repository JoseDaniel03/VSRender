"""Tests for the idempotent migration patch; no Java project is needed for these."""
from pathlib import Path
from tempfile import TemporaryDirectory
from unittest import TestCase
from unittest.mock import patch

import apply_26_3

ITEM_FIXTURE = """class ItemProcessors {
                        if (DataComponentKeys.contains(componentKey)) {
                            callback.accept(ComponentsProcessor.createSingle(componentKey, value));
                        }
}"""

COMP_FIXTURE = """import net.momirealms.craftengine.core.util.TagParser;
class ComponentsProcessor {
            } else {
                arguments.add(getProvider(id, value));
            }
    public record DynamicComponentProvider(Key type, Function<ItemBuildContext, Tag> function) {
    }

    public static ComponentsProcessor createSingle
                } else {
                    arguments.add(getProvider(id, componentValue));
                }
}"""


class ItemMigrationPatchTests(TestCase):
    def setUp(self):
        self.temp = TemporaryDirectory()
        self.root = Path(self.temp.name)
        self.item = self.root / "ItemProcessors.java"
        self.comp = self.root / "ComponentsProcessor.java"
        self.test = self.root / "tests" / "ItemComponent26_3Test.java"
        self.fixture = self.root / "porting" / "ItemComponent26_3Test.java"
        self.fixture.parent.mkdir()
        self.fixture.write_text("class PortingTest {}\n", encoding="utf-8")
        self.item.write_text(ITEM_FIXTURE, encoding="utf-8")
        self.comp.write_text(COMP_FIXTURE, encoding="utf-8")

    def tearDown(self):
        self.temp.cleanup()

    def apply(self):
        with patch.multiple(apply_26_3, ROOT=self.root, ITEM=self.item, COMP=self.comp, TEST=self.test):
            apply_26_3.run()

    def test_patches_both_yaml_component_paths_and_includes_junit(self):
        self.apply()
        item = self.item.read_text(encoding="utf-8")
        components = self.comp.read_text(encoding="utf-8")
        self.assertIn("ComponentsProcessor.compatibleKeys(", item)
        self.assertIn("VersionHelper.isOrAbove26_3", item)
        self.assertIn("addCompatibleProviders(arguments, id, value, keys)", components)
        self.assertIn("addCompatibleProviders(arguments, id, componentValue, componentsSection.keySet())", components)
        self.assertIn("import net.momirealms.craftengine.core.util.VersionHelper;", components)
        self.assertIn("if (DataComponentKeys.MAP_COLOR.equals(id)) return List.of();", components)
        self.assertEqual("class PortingTest {}\n", self.test.read_text())

    def test_running_twice_changes_nothing(self):
        self.apply()
        first = (self.item.read_bytes(), self.comp.read_bytes(), self.test.read_bytes())
        self.apply()
        second = (self.item.read_bytes(), self.comp.read_bytes(), self.test.read_bytes())
        self.assertEqual(first, second)

    def test_refuses_changed_upstream_without_partial_writes(self):
        self.comp.write_text(COMP_FIXTURE.replace(
            "arguments.add(getProvider(id, componentValue));",
            "differentUpstreamAPI(id, componentValue);"
        ), encoding="utf-8")
        item_before = self.item.read_bytes()
        comp_before = self.comp.read_bytes()
        with self.assertRaises(RuntimeError):
            self.apply()
        self.assertEqual(item_before, self.item.read_bytes())
        self.assertEqual(comp_before, self.comp.read_bytes())
        self.assertFalse(self.test.exists())

    def test_rejects_missing_original_sources(self):
        self.comp.unlink()
        with self.assertRaises(SystemExit):
            self.apply()
