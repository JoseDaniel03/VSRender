"""26.3 source contracts that can be checked before private Java dependencies exist.

These checks prevent accidental regression to 26.2 metadata, item component
mappings and native bundle selection. They are not a native runtime smoke test.
"""
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def source(path):
    return (ROOT / path).read_text(encoding="utf-8")


class Minecraft263SourceContracts(unittest.TestCase):
    def test_resource_pack_minor_version_and_registered_server_release(self):
        content = source("core/src/main/java/net/momirealms/craftengine/core/util/MinecraftVersion.java")
        self.assertIn('PACK_FORMATS.put(26_03_00, new PackVersion(97, 1))', content)
        self.assertIn('new MinecraftVersion("26.3")', content)

    def test_java25_userdev_bundle_targets_26_3(self):
        content = source("bukkit/native26/build.gradle.kts")
        self.assertIn('.orElse("26.3.build.+")', content)

    def test_leafpile_is_compile_only_for_core(self):
        content = source("core/build.gradle.kts")
        self.assertIn('compileOnly(files("${rootProject.rootDir}/libs/leafpile-${versionOf("leafpile")}.jar"))', content)
        self.assertNotIn('implementation(files("${rootProject.rootDir}/libs/leafpile-', content)

    def test_new_data_component_ids_are_present(self):
        content = source("core/src/main/java/net/momirealms/craftengine/core/item/component/DataComponentKeys.java")
        for key in (
            '"attack_animation"',
            '"interact_animation"',
            '"compostable"',
            '"cooking_fuel"',
            '"brewing_fuel"',
            '"block_transformer"',
        ):
            with self.subTest(component=key):
                self.assertIn(key, content)

    def test_retired_components_are_handled_at_processing_boundary(self):
        content = source("core/src/main/java/net/momirealms/craftengine/core/item/processor/ComponentsProcessor.java")
        self.assertIn('if (DataComponentKeys.MAP_COLOR.equals(id)) return List.of();', content)
        self.assertIn('DataComponentKeys.SWING_ANIMATION.equals(id)', content)
        self.assertIn('DataComponentKeys.ATTACK_ANIMATION', content)
        self.assertIn('DataComponentKeys.INTERACT_ANIMATION', content)


if __name__ == "__main__":
    unittest.main()
