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

    def test_26_3_datapack_cooking_recipes_are_strict(self):
        reader = source("core/src/main/java/net/momirealms/craftengine/core/item/recipe/reader/VanillaRecipeReader26_3.java")
        serializer = source("core/src/main/java/net/momirealms/craftengine/core/item/recipe/AbstractRecipeSerializer.java")
        self.assertIn('recipe.has("cookingtime")', reader)
        self.assertIn('ticks <= 0', reader)
        self.assertIn("new VanillaRecipeReader26_3()", serializer)
        self.assertIn("VersionHelper.isOrAbove26_3", serializer)

    def test_26_3_blockbench_export_replaces_removed_shade_field(self):
        converter = source("core/src/main/java/net/momirealms/craftengine/core/pack/model/bbmodel/BBModelConverter.java")
        self.assertIn('out.addProperty("shade_direction_override", "up")', converter)
        self.assertIn("if (VersionHelper.isOrAbove26_3)", converter)
        self.assertIn('out.addProperty("shade", false)', converter)

    def test_26_3_native_hitbox_does_not_depend_on_removed_setter(self):
        entity = source("bukkit/native26/src/main/java/net/momirealms/craftengine/bukkit/render/runtime/v26_2/Native26RigCollisionEntity.java")
        self.assertNotIn("this.setInvulnerable(true);", entity)
        self.assertIn("boolean hurtServer(ServerLevel level, DamageSource source, float amount)", entity)
        self.assertIn("return false;", entity)

    def test_retired_components_are_handled_at_processing_boundary(self):
        content = source("core/src/main/java/net/momirealms/craftengine/core/item/processor/ComponentsProcessor.java")
        self.assertIn('if (DataComponentKeys.MAP_COLOR.equals(id)) return List.of();', content)
        self.assertIn('DataComponentKeys.SWING_ANIMATION.equals(id)', content)
        self.assertIn('DataComponentKeys.ATTACK_ANIMATION', content)
        self.assertIn('DataComponentKeys.INTERACT_ANIMATION', content)


if __name__ == "__main__":
    unittest.main()
