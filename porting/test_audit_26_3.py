import json
import tempfile
import unittest
from zipfile import ZipFile, ZIP_DEFLATED
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

    def test_pack_metadata_range_excluding_26_3_is_flagged(self):
        self._file("data/demo/context_int_provider/number.json", {"type": "minecraft:constant", "value": 2})
        self._file(
            "pack.mcmeta",
            {"pack": {"min_format": [100, 0], "max_format": [120, 0]}},
        )
        warnings = check(self.root)
        self.assertTrue(any("does not include datapack 121.0" in warning for warning in warnings))

    def test_resource_pack_metadata_range_excluding_26_3_is_flagged(self):
        self._file("assets/demo/models/item/example.json", {"parent": "minecraft:item/generated"})
        self._file(
            "pack.mcmeta",
            {"pack": {"min_format": [75, 0], "max_format": [97, 0]}},
        )
        warnings = check(self.root)
        self.assertTrue(any("does not include resource-pack 97.1" in warning for warning in warnings))

    def test_resource_pack_integer_max_format_includes_minor_97_1(self):
        self._file("assets/demo/models/item/example.json", {"parent": "minecraft:item/generated"})
        self._file(
            "pack.mcmeta",
            {"pack": {"min_format": [97, 1], "max_format": 97}},
        )
        self.assertEqual([], check(self.root))

    def test_resource_pack_single_element_list_max_format_includes_minor_97_1(self):
        self._file("assets/demo/models/item/example.json", {"parent": "minecraft:item/generated"})
        self._file(
            "pack.mcmeta",
            {"pack": {"min_format": [97, 1], "max_format": [97]}},
        )
        self.assertEqual([], check(self.root))

    def test_datapack_integer_max_format_includes_121_0(self):
        self._file("data/demo/context_int_provider/value.json",
                   {"type": "minecraft:constant", "value": 2})
        self._file("pack.mcmeta",
                   {"pack": {"min_format": 120, "max_format": 121}})
        self.assertEqual([], check(self.root))

    def test_resource_pack_integer_max_format_below_97_stays_incompatible(self):
        self._file("assets/demo/models/item/example.json", {"parent": "minecraft:item/generated"})
        self._file("pack.mcmeta",
                   {"pack": {"min_format": 75, "max_format": 96}})
        self.assertTrue(any("does not include resource-pack 97.1" in w
                            for w in check(self.root)))

    def test_pack_format_alone_is_not_enough_for_26_3(self):
        self._file("data/demo/context_int_provider/value.json",
                   {"type": "minecraft:constant", "value": 1})
        self._file("pack.mcmeta", {"pack": {"pack_format": 121}})
        self.assertTrue(any("requires both min_format and max_format" in w
                            for w in check(self.root)))

    def test_supported_formats_alone_is_not_a_26_3_range(self):
        self._file("assets/demo/models/item/stick.json",
                   {"parent": "minecraft:item/generated"})
        self._file("pack.mcmeta",
                   {"pack": {"pack_format": 97, "supported_formats": [97, 97]}})
        self.assertTrue(any("requires both min_format and max_format" in w
                            for w in check(self.root)))

    def test_partial_pack_metadata_range_is_reported(self):
        self._file("assets/demo/models/item/stick.json",
                   {"parent": "minecraft:item/generated"})
        self._file("pack.mcmeta", {"pack": {"min_format": [97, 1]}})
        self.assertTrue(any("requires both min_format and max_format" in w
                            for w in check(self.root)))

    def test_malformed_pack_metadata_bound_is_reported(self):
        self._file("assets/demo/models/item/stick.json",
                   {"parent": "minecraft:item/generated"})
        self._file("pack.mcmeta",
                   {"pack": {"min_format": [97, 1], "max_format": ["97", 1]}})
        self.assertTrue(any("invalid min_format/max_format" in w
                            for w in check(self.root)))

    def test_reversed_pack_metadata_range_is_reported(self):
        self._file("assets/demo/models/item/stick.json",
                   {"parent": "minecraft:item/generated"})
        self._file("pack.mcmeta",
                   {"pack": {"min_format": [97, 2], "max_format": [97, 1]}})
        self.assertTrue(any("min_format is greater than max_format" in w
                            for w in check(self.root)))

    def test_resource_pack_metadata_with_97_1_is_supported(self):
        self._file("assets/demo/models/item/example.json", {"parent": "minecraft:item/generated"})
        self._file(
            "pack.mcmeta",
            {"pack": {"min_format": [75, 0], "max_format": [97, 1]}},
        )
        self.assertEqual([], check(self.root))

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

    def test_26_3_standalone_predicate_renamed_condition_and_removed_check(self):
        self._file(
            "data/demo/predicate/damage.json",
            {"condition": "minecraft:value_check", "value": {"type": "minecraft:score"}, "range": {"min": 1}},
        )
        warnings = check(self.root)
        self.assertTrue(any("predicate field condition was renamed to type" in w for w in warnings))

    def test_26_3_removed_predicate_types_must_be_replaced(self):
        self._file(
            "data/demo/predicate/block.json",
            {"type": "minecraft:block_state_property", "block": "minecraft:stone"},
        )
        warnings = check(self.root)
        self.assertTrue(any("minecraft:match_block" in w for w in warnings))

    def test_26_3_item_modifier_reference_replaced_by_direct_id(self):
        self._file(
            "data/demo/item_modifier/modify.json",
            [{"function": "minecraft:reference", "name": "demo:enchanted"}],
        )
        warnings = check(self.root)
        self.assertTrue(any("field function was renamed to type" in w for w in warnings))

    def test_26_3_predicate_reference_type_removed(self):
        self._file(
            "data/demo/predicate/protection.json",
            {"type": "minecraft:reference", "name": "demo:protected"},
        )
        self.assertTrue(any("minecraft:reference was removed" in w for w in check(self.root)))

    def test_26_3_loot_pool_entry_renamed_fields_but_not_pool_fields(self):
        self._file(
            "data/demo/loot_table/test.json",
            {"pools": [{
                "conditions": [{"condition": "minecraft:survives_explosion"}],
                "functions": [{"function": "minecraft:set_count", "count": 2}],
                "entries": [{
                    "type": "minecraft:item",
                    "name": "minecraft:stone",
                    "conditions": [{"condition": "minecraft:random_chance", "chance": .5}],
                    "functions": [{"function": "minecraft:set_count", "count": 2}],
                }]
            }]},
        )
        warnings = check(self.root)
        self.assertEqual(1, len([w for w in warnings if "pool entry conditions" in w]))
        self.assertEqual(1, len([w for w in warnings if "pool entry functions" in w]))
        self.assertTrue(any("loot function field function" in w for w in warnings))

    def test_26_3_renamed_tag_entry_and_loot_modifier_fields(self):
        self._file(
            "data/demo/loot_table/treasure.json",
            {"pools": [{"entries": [
                {"type": "minecraft:tag", "name": "minecraft:logs", "expand": True},
                {"type": "minecraft:item", "name": "minecraft:diamond",
                 "modifier": {"type": "minecraft:set_loot_table", "name": "demo:loot"}},
            ]}]},
        )
        warnings = check(self.root)
        self.assertTrue(any("name to items" in w for w in warnings))
        self.assertTrue(any("loot_table_id" in w for w in warnings))

    def test_26_3_limit_count_range_not_bare_number(self):
        self._file(
            "data/demo/item_modifier/limit.json",
            {"type": "minecraft:limit_count", "limit": 10},
        )
        self.assertTrue(any("structured range" in w for w in check(self.root)))

    def test_26_3_modern_inline_ids_do_not_produce_false_positives(self):
        self._file(
            "data/demo/loot_table/example.json",
            {"pools": [{"entries": [{
                "type": "minecraft:item", "name": "minecraft:diamond",
                "condition": "demo:allowed",
                "modifier": ["demo:enchanted", {"type": "minecraft:set_count", "count": 1}],
            }]}]},
        )
        self._file(
            "data/demo/predicate/allowed.json",
            {"type": "minecraft:all_of", "terms": ["demo:other", {"type": "minecraft:random_chance", "chance": .5}]},
        )
        self._file(
            "data/demo/item_modifier/enchant.json",
            {"type": "minecraft:sequence", "functions": ["demo:modifier"]},
        )
        self.assertEqual([], check(self.root))

    def test_26_3_unrelated_json_modifiers_are_not_interpreted_as_loot(self):
        self._file(
            "data/demo/worldgen/noise_settings/noise.json",
            {"function": "minecraft:set_count", "condition": "minecraft:value_check", "name": "demo:target"},
        )
        self.assertEqual([], check(self.root))

    def test_26_3_valid_vanilla_brewing_recipe_has_no_warnings(self):
        self._file("data/demo/recipe/awkward.json", {
            "type": "minecraft:brewing",
            "input": {"item": "minecraft:potion",
                      "potion_contents": {"potion": "minecraft:water"}},
            "reagent": {"item": "minecraft:nether_wart"},
            "output": {"id": "minecraft:potion", "components": {
                "minecraft:potion_contents": {"potion": "minecraft:awkward"}}},
        })
        self.assertEqual([], check(self.root))

    def test_26_3_brewing_recipe_requires_reagent(self):
        self._file("data/demo/recipe/bad.json", {
            "type": "minecraft:brewing",
            "input": {"item": "minecraft:bucket"},
            "output": {"id": "minecraft:water_bucket"},
        })
        self.assertTrue(any("requires reagent object" in w for w in check(self.root)))

    def test_26_3_brewing_ingredients_require_item(self):
        self._file("data/demo/recipes/bad.json", {
            "type": "brewing", "input": {"potion_contents": {}},
            "reagent": {"item": "minecraft:nether_wart"},
            "output": {"id": "minecraft:potion"},
        })
        self.assertTrue(any("brewing input requires item" in w for w in check(self.root)))

    def test_26_3_brewing_output_requires_item_stack(self):
        self._file("data/demo/recipe/bad.json", {
            "type": "minecraft:brewing", "input": {"item": "minecraft:potion"},
            "reagent": {"item": "minecraft:nether_wart"},
            "output": {"components": {}},
        })
        self.assertTrue(any("output requires an item stack with id" in w
                            for w in check(self.root)))

    def test_26_3_brewing_potion_contents_requires_object(self):
        self._file("data/demo/recipe/bad.json", {
            "type": "minecraft:brewing",
            "input": {"item": "minecraft:potion", "potion_contents": "minecraft:water"},
            "reagent": {"item": "minecraft:nether_wart"},
            "output": {"id": "minecraft:potion"},
        })
        self.assertTrue(any("potion_contents must be an object" in w
                            for w in check(self.root)))

    def test_26_3_brewing_validation_is_restricted_to_vanilla_recipe_files(self):
        self._file("assets/demo/items/example.json", {"type": "minecraft:brewing"})
        self.assertEqual([], check(self.root))

    def test_audits_datapack_zip_without_extracting(self):
        package = self.root / "legacy-data.zip"
        with ZipFile(package, "w", ZIP_DEFLATED) as archive:
            archive.writestr("pack.mcmeta", json.dumps({"pack": {"pack_format": 95}}))
            archive.writestr("data/demo/predicate/condition.json",
                json.dumps({"type": "minecraft:value_check", "value": 3}))
            archive.writestr("data/demo/recipe/bake.json",
                json.dumps({"type": "minecraft:smelting"}))
        warnings = check(package)
        self.assertTrue(any("datapack 121.0" in w for w in warnings))
        self.assertTrue(any("minecraft:int_value_check" in w for w in warnings))
        self.assertTrue(any("requires cookingtime" in w for w in warnings))
        self.assertFalse((self.root / "pack.mcmeta").exists())
        self.assertFalse((self.root / "data").exists())

    def test_audits_resource_pack_zip_metadata_and_shaders(self):
        package = self.root / "legacy-resource.zip"
        with ZipFile(package, "w", ZIP_DEFLATED) as archive:
            archive.writestr("pack.mcmeta", json.dumps({"pack": {"pack_format": 96}}))
            archive.writestr("assets/demo/shaders/fog.fsh",
                "#version 330\\n#moj_import <fog.glsl>\\n")
            archive.writestr("assets/demo/models/block/stone.json",
                json.dumps({"elements": [{"shade": False}]}))
        warnings = check(package)
        self.assertTrue(any("resource-pack 97.1" in w for w in warnings))
        self.assertTrue(any("moj_import" in w for w in warnings))
        self.assertTrue(any("shade_direction_override" in w for w in warnings))

    def test_zip_auditor_skips_unsafe_paths_and_large_members(self):
        package = self.root / "unsafe.zip"
        with ZipFile(package, "w") as archive:
            archive.writestr("../outside.json", "{}")
            archive.writestr("data/demo/loot_table/huge.json", " " * (8 * 1024 * 1024 + 1))
        warnings = check(package)
        self.assertTrue(any("unsafe ZIP entry" in w for w in warnings))
        self.assertTrue(any("audit size limit" in w for w in warnings))
        self.assertFalse((self.root.parent / "outside.json").exists())

    def test_modern_zip_packs_stay_clean(self):
        package = self.root / "modern.zip"
        with ZipFile(package, "w", ZIP_DEFLATED) as archive:
            archive.writestr("pack.mcmeta",
                json.dumps({"pack": {"min_format": [97, 1], "max_format": [97, 1]}}))
            archive.writestr("assets/demo/models/item/stick.json",
                json.dumps({"parent": "minecraft:item/generated"}))
        self.assertEqual([], check(package))

    def test_26_3_valid_block_transformer_registry_rules(self):
        self._file(
            "data/demo/block_transformer/chisel.json",
            [
                {
                    "block_state_provider": {
                        "type": "minecraft:simple",
                        "state": {"id": "minecraft:polished_andesite"},
                    },
                    "particle": "scrape",
                    "disallowed_faces": ["up", "north"],
                    "sound": "minecraft:item.axe.strip",
                    "drop_strategy": "clicked_face",
                    "transform_type": "single_block",
                    "item_damage_per_use": 1,
                    "consume_on_use": False,
                    "update_from_neighbors": True,
                },
                {"block_state_provider": "demo:conditional_state", "particle": "none"},
            ],
        )
        self.assertEqual([], check(self.root))

    def test_26_3_block_transformer_requires_array_and_provider(self):
        self._file(
            "data/demo/block_transformer/chisel.json",
            {"block_state_provider": "minecraft:stone"},
        )
        self.assertTrue(any("list of rules" in w for w in check(self.root)))
        self._file("data/demo/block_transformer/chisel.json", [{}, 9])
        warnings = check(self.root)
        self.assertTrue(any("requires a block_state_provider" in w for w in warnings))
        self.assertTrue(any("rule 1 must be an object" in w for w in warnings))

    def test_26_3_block_transformer_rejects_obsolete_state_provider_and_fields(self):
        self._file(
            "data/demo/block_transformer/wax.json",
            [{
                "block_state_provider": {
                    "type": "minecraft:simple_state_provider",
                    "Name": "minecraft:stone",
                    "Properties": {"axis": "y"},
                },
                "particle": "fire",
                "disallowed_faces": ["above", 23],
                "drop_strategy": "anywhere",
                "transform_type": "cascade",
                "update_from_neighbors": "true",
                "consume_on_use": 1,
                "item_damage_per_use": -1,
            }],
        )
        warnings = check(self.root)
        for expected in (
            "minecraft:simple", "field Name is now id", "field Properties",
            "invalid particle", "disallowed_faces", "invalid drop_strategy",
            "invalid transform_type", "update_from_neighbors must be boolean",
            "consume_on_use must be boolean", "nonnegative integer",
        ):
            with self.subTest(error=expected):
                self.assertTrue(any(expected in w for w in warnings), warnings)

    def test_26_3_block_transformer_item_component_refs_are_left_to_vanilla(self):
        self._file(
            "data/demo/item/transformer.json",
            {"components": {"minecraft:block_transformer": "demo:chisel"}},
        )
        self.assertEqual([], check(self.root))

    def test_rerun_is_deterministic(self):
        self._file(
            "resources/demo/data/demo/recipe/a.json",
            {"type": "smelting"},
        )
        self.assertEqual(check(self.root), check(self.root))


if __name__ == "__main__":
    unittest.main()
