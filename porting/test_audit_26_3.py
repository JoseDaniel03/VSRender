import json
import tempfile
import unittest
from pathlib import Path

from audit_26_3 import check

class Audit26_3Tests(unittest.TestCase):
    def test_finds_legacy_shader_import(self):
        with tempfile.TemporaryDirectory() as tmp:
            path=Path(tmp)/"resources/example/resourcepack/assets/demo/shaders/core/demo.fsh"
            path.parent.mkdir(parents=True)
            path.write_text("#version 330\n#moj_import <fog.glsl>\n")
            self.assertEqual(1, len(check(Path(tmp))))

    def test_find_cooking_recipe_missing_explicit_time(self):
        with tempfile.TemporaryDirectory() as tmp:
            path=Path(tmp)/"examples/demo/data/demo/recipe/furnace.json"
            path.parent.mkdir(parents=True)
            path.write_text(json.dumps({"type":"minecraft:smelting","ingredient":"minecraft:stone","result":"minecraft:cobblestone"}))
            self.assertTrue(any("cookingtime" in warning for warning in check(Path(tmp))))

    def test_valid_recipe_and_shader_are_unchanged(self):
        with tempfile.TemporaryDirectory() as tmp:
            root=Path(tmp)
            p=root/"resources/demo/data/demo/recipe/a.json"
            p.parent.mkdir(parents=True)
            p.write_text(json.dumps({"type":"minecraft:blasting","cookingtime":200}))
            s=root/"resources/demo/resourcepack/assets/demo/shaders/demo.fsh"
            s.parent.mkdir(parents=True)
            s.write_text("#version 330\n#include \"fog.glsl\"\n")
            self.assertEqual([],check(root))
            self.assertIn('"cookingtime"',p.read_text())

if __name__=="__main__": unittest.main()
