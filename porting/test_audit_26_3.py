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

    def test_legacy_pot_decorations_id_array_is_flagged(self):
        self._file(
            "resources/demo/data/demo/item/flower_pot.json",
            {"components": {"minecraft:pot_decorations": [
                "minecraft:brick", "minecraft:brick",
                "minecraft:brick", "minecraft:brick"
            ]}},
        )
        self.assertTrue(any("pot_decorations" in w for w in check(self.root)))

    def test_modern_pot_decorations_object_is_allowed(self):
        self._file(
            "resources/demo/data/demo/item/flower_pot.json",
            {"components": {"minecraft:pot_decorations": {
                "front": {"id": "minecraft:brick"}
            }}},
        )
        self.assertEqual([], check(self.root))

    def test_removed_brewing_fuel_item_tag_is_flagged(self):
        self._file(
            "resources/demo/datapack/data/minecraft/tags/item/brewing_fuel.json",
            {"values": ["minecraft:blaze_powder"]},
        )
        self.assertTrue(any("brewing_fuel item tag" in w for w in check(self.root)))

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

    def test_26_3_obsolete_number_provider_registry_requires_manual_choice(self):
        self._file(
            "resources/demo/data/demo/number_provider/power.json",
            {"type": "minecraft:sum", "inputs": [1, 2]},
        )
        warnings = check(self.root)
        self.assertTrue(any("registry was split" in warning for warning in warnings))
        self.assertTrue(any("context_int_provider" in warning for warning in warnings))

    def test_26_3_old_number_provider_tag_registry(self):
        self._file(
            "resources/demo/data/demo/tags/number_provider/values.json",
            {"values": ["demo:multiplier"]},
        )
        self.assertTrue(any("tags/number_provider" in warning for warning in check(self.root)))

    def test_26_3_int_and_float_provider_names_are_updated(self):
        self._file(
            "resources/demo/data/demo/context_int_provider/burn.json",
            {"type": "minecraft:sum", "inputs": [100, 200]},
        )
        self._file(
            "resources/demo/data/demo/context_float_provider/speed.json",
            {"type": "minecraft:product", "inputs": [1.0, 2.0]},
        )
        warnings = check(self.root)
        self.assertTrue(any("minecraft:add" in w for w in warnings))
        self.assertTrue(any("minecraft:mul" in w for w in warnings))
        self.assertEqual(2, len(warnings))

    def test_26_3_inline_uniform_provider_needs_type(self):
        self._file(
            "resources/demo/data/demo/context_int_provider/random.json",
            {"min": 1, "max": 3},
        )
        self.assertTrue(any("explicit type" in w for w in check(self.root)))

    def test_26_3_loot_table_rolls_are_context_providers(self):
        self._file(
            "resources/demo/data/demo/loot_table/mob.json",
            {"pools": [{
                "rolls": {"type": "minecraft:average", "inputs": [1, 2]},
                "bonus_rolls": {"type": "minecraft:minimum", "inputs": [0.0, 1.0]},
            }]},
        )
        warnings = check(self.root)
        self.assertTrue(any("context_int_provider" in w and "minecraft:avg" in w for w in warnings))
        self.assertTrue(any("context_float_provider" in w and "minecraft:min" in w for w in warnings))

    def test_26_3_fuel_components_use_new_provider_names(self):
        self._file(
            "resources/demo/data/demo/item/example.json",
            {"components": {
                "minecraft:cooking_fuel": {
                    "burn_time": {"type": "minecraft:sum", "inputs": [100, 200]},
                    "speed_multiplier": {"type": "minecraft:product", "inputs": [1.0, 2.0]},
                },
                "minecraft:brewing_fuel": {
                    "uses": {"min": 2, "max": 5}
                },
            }},
        )
        warnings = check(self.root)
        self.assertEqual(3, len(warnings))
        self.assertTrue(any("minecraft:add" in w for w in warnings))
        self.assertTrue(any("minecraft:mul" in w for w in warnings))
        self.assertTrue(any("minecraft:uniform" in w for w in warnings))

    def test_26_3_worldgen_block_state_provider_names(self):
        self._file(
            "resources/demo/data/demo/worldgen/block_state_provider/palette.json",
            {"type": "minecraft:weighted_state_provider", "entries": []},
        )
        warnings = check(self.root)
        self.assertTrue(any("minecraft:weighted" in w for w in warnings))

    def test_new_number_provider_references_and_context_free_worldgen_not_flagged(self):
        self._file(
            "resources/demo/data/demo/context_int_provider/burn.json",
            {"type": "minecraft:add", "inputs": [100, 200]},
        )
        self._file(
            "resources/demo/data/demo/context_float_provider/speed.json",
            {"type": "minecraft:mul", "inputs": ["demo:multiplier", 1.5]},
        )
        self._file(
            "resources/demo/data/demo/loot_table/loot.json",
            {"pools": [{"rolls": "demo:rolls", "bonus_rolls": 0.25}]},
        )
        self._file(
            "resources/demo/data/demo/worldgen/noise_settings/world.json",
            {"type": "minecraft:sum", "min": 1, "max": 8},
        )
        self.assertEqual([], check(self.root))

    def test_datapack_pack_metadata_uses_121_not_resource_pack_97(self):
        self._file(
            "data/demo/context_int_provider/value.json",
            {"type": "minecraft:constant", "value": 1},
        )
        self._file("pack.mcmeta", {"pack": {"pack_format": 100}})
        self.assertTrue(any("datapack 121.0" in w for w in check(self.root)))

    def test_version_range_does_not_trigger_obsolete_pack_format_warning(self):
        self._file(
            "pack.mcmeta",
            {"pack": {
                "pack_format": 75,
                "min_format": [75, 0],
                "max_format": [121, 0],
            }},
        )
        self.assertEqual([], check(self.root))

    def test_nested_datapack_metadata_uses_data_version(self):
        self._file(
            "resources/demo/datapack/pack.mcmeta",
            {"pack": {"pack_format": 90}},
        )
        self.assertTrue(any("datapack 121.0" in w for w in check(self.root)))

    def test_scan_standalone_datapack_directly(self):
        self._file(
            "data/demo/context_int_provider/rolls.json",
            {"type": "minecraft:sum", "inputs": [1, 3]},
        )
        self.assertTrue(any("minecraft:add" in w for w in check(self.root)))

    def test_scan_standalone_resource_pack_directly(self):
        self._file(
            "pack.mcmeta",
            {"pack": {"pack_format": 76, "description": "legacy"}},
        )
        self._file(
            "assets/demo/models/block/simple.json",
            {"elements": [{"shade": False}]},
        )
        warnings = check(self.root)
        self.assertTrue(any("shade_direction_override" in w for w in warnings))
        self.assertTrue(any("97.1" in w for w in warnings))

    def test_rerun_is_deterministic(self):
        self._file(
            "resources/demo/data/demo/recipe/a.json",
            {"type": "smelting"},
        )
        self.assertEqual(check(self.root), check(self.root))


if __name__ == "__main__":
    unittest.main()
