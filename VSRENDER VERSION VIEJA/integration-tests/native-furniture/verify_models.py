"""Check sofa back continuity and arms against neighbor orientation, not state names alone."""
import json
from pathlib import Path
import unittest

MODELS = Path(__file__).resolve().parents[2] / 'examples/vsrender_demo/resourcepack/assets/vsrender/models/block'

class FurnitureGeometryTest(unittest.TestCase):
    def test_perpendicular_neighbor_back_continues_through_each_corner(self):
        # North sofa + west-facing neighbor behind => neighbor back on east.
        # North sofa + east-facing neighbor behind => neighbor back on west.
        for shape, x in [('inner_left',13),('inner_right',0)]:
            for file in MODELS.glob(f'demo_sofa_{shape}_*.json'):
                with self.subTest(model=file.name):
                    elements=json.loads(file.read_text())['elements']
                    self.assertTrue(any(e['from']==[x,10,0] and e['to']==[x+3,16,13] for e in elements))
                    # No low end arm may overlap the tall corner back.
                    arm_x = 14 if x==13 else 0
                    self.assertFalse(any(e['from']==[arm_x,8,0] for e in elements))

    def test_connected_modules_remove_only_their_joining_arms(self):
        for left in [False,True]:
            for right in [False,True]:
                name=f'demo_sofa_straight_{str(left).lower()}_{str(right).lower()}.json'
                elements=json.loads((MODELS/name).read_text())['elements']
                with self.subTest(model=name):
                    self.assertEqual(not left,any(e['from']==[0,8,0] for e in elements))
                    self.assertEqual(not right,any(e['from']==[14,8,0] for e in elements))

if __name__=='__main__': unittest.main()
