import json
import tempfile
import unittest
from pathlib import Path

from audit_26_3 import check


class Audit26_3Tests(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.root = Path(self.tmp.name)

    def tearDown(self):
        self.tmp.cleanup()

    def _file(self, filename, data):
        path = self.root / filename
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(
            data if isinstance(data, str) else json.dumps(data), encoding="utf-8"
        )
        return path

    def test_finds_legacy_shader_import(self):
        self._file(
            "resources/demo/resourcepack/assets/demo/shaders/fog.fsh",
            "#version 330\n#moj_import <fog.glsl>\n",
        )
        self.assertTrue(any("moj_import" in w for w in check(self.root)))

    def test_cooking_recipe_in_singular_directory(self):
        self._file(
            "examples/demo/data/demo/recipe/furnace.json",
            {"type": "minecraft:smelting", "result": {"id": "minecraft:stone"}},
        )
        self.assertTrue(any("requires cookingtime" in w for w in check(self.root)))

    def test_cooking_recipe_in_plural_directory_and_short_type(self):
        self._file(
            "resources/demo/data/demo/recipes/food.json",
            {"type": "smoking", "result": {"id": "minecraft:cooked_beef"}},
        )
        self.assertTrue(any("requires cookingtime" in w for w in check(self.root)))

    def test_cooking_time_must_be_positive(self):
        self._file(
            "resources/demo/data/demo/recipe/a.json",
            {"type": "minecraft:blasting", "cookingtime": 0},
        )
        self.assertTrue(any("positive integer" in w for w in check(self.root)))

    def test_removed_map_color_item_tint(self):
        self._file(
            "resources/demo/resourcepack/assets/demo/items/map.json",
            {"model": {"type": "minecraft:model", "tints": [{"type": "minecraft:map_color"}]}},
        )
        self.assertTrue(any("tint source" in w for w in check(self.root)))

    def test_removed_shade_flag_in_resource_pack_model(self):
        self._file(
            "resources/demo/resourcepack/assets/demo/models/block/unshaded.json",
            {"elements": [{"from": [0, 0, 0], "to": [16, 16, 16], "shade": False}]},
        )
        warnings = check(self.root)
        self.assertTrue(any("shade_direction_override" in w for w in warnings))

    def test_shader_and_unrelated_json_shade_fields_are_not_reported_as_models(self):
        self._file(
            "resources/demo/resourcepack/assets/demo/other/config.json",
            {"shade": False},
        )
        self.assertEqual([], check(self.root))

    def test_inline_block_transformer_is_rejected(self):
        self._file(
            "resources/demo/data/demo/item/example.json",
            {"components": {"minecraft:block_transformer": [{"block_state_provider": {"type": "minecraft:simple_state_provider"}}]}},
        )
        self.assertTrue(any("registry ID" in w for w in check(self.root)))

    def test_removed_swing_in_raw_json(self):
        self._file(
            "resources/demo/data/demo/loot_table/example.json",
            {"components": {"minecraft:swing_animation": {"type": "stab", "duration": 6}}},
        )
        self.assertTrue(any("removed swing_animation" in w for w in check(self.root)))

    def test_legacy_trim_material(self):
        self._file(
            "resources/demo/data/demo/trim_material/ruby.json",
            {"asset_name": "ruby", "override_armor_assets": {}},
        )
        warnings = check(self.root)
        self.assertEqual(2, len(warnings))

    def test_valid_resources_remain_unchanged(self):
        path = self._file(
            "resources/demo/data/demo/recipe/a.json",
            {"type": "minecraft:blasting", "cookingtime": 200},
        )
        self._file(
            "resources/demo/resourcepack/assets/demo/shaders/fog.fsh",
            '#version 330\n#include "fog.glsl"\n',
        )
        self._file(
            "resources/demo/data/demo/item/example.json",
            {"components": {"minecraft:block_transformer": "demo:chisel_rules"}},
        )
        self.assertEqual([], check(self.root))
        self.assertEqual(200, json.loads(path.read_text())["cookingtime"])

    def test_rerun_is_deterministic(self):
        self._file(
            "resources/demo/data/demo/recipe/a.json",
            {"type": "smelting"},
        )
        self.assertEqual(check(self.root), check(self.root))


if __name__ == "__main__":
    unittest.main()
