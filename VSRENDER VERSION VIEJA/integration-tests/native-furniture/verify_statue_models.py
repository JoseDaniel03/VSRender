#!/usr/bin/env python3
"""Validate real-cell statue geometry and all directional block appearances."""
import json
from pathlib import Path
import yaml

ROOT = Path(__file__).resolve().parents[2]
DEMO = ROOT / 'examples/vsrender_demo'
MODELS = DEMO / 'resourcepack/assets/vsrender/models/block'
EXPECTED = [(0,0,0),(-1,0,-1),(0,0,-1),(1,0,-1),(-1,0,0),(1,0,0),(-1,0,1),(0,0,1),(1,0,1),(0,1,0),(0,2,0),(-1,3,0),(0,3,0),(1,3,0),(0,4,0)]

def read_model(name):
    model = json.loads((MODELS / f'{name}.json').read_text())
    assert model['elements'], f'{name}: empty model'
    assert all(v.startswith('minecraft:block/') for v in model['textures'].values())
    for e in model['elements']:
        assert 'rotation' not in e, f'{name}: unsupported rotated bounds'
        assert all(0 <= a < b <= 16 for a,b in zip(e['from'],e['to'])), f'{name}: non-local bounds {e}'
        assert set(e['faces']) == {'north','south','east','west','up','down'}
        for face in e['faces'].values():
            assert 'cullface' not in face and 'tintindex' not in face
            assert face['texture'][1:] in model['textures']
    return model

def connected(a,b):
    # Positive face overlap or volume overlap: edge/corner-only contact is insufficient.
    overlaps = [min(a[1][i],b[1][i])-max(a[0][i],b[0][i]) for i in range(3)]
    return all(v >= 0 for v in overlaps) and sum(v > 0 for v in overlaps) >= 2

def main():
    item = yaml.safe_load((DEMO / 'configuration/statue.yml').read_text())['items']['vsrender:demo_statue_giant']
    assert item['material'] == 'paper' and item['model'] == 'vsrender:block/demo_statue_preview'
    assert item['behavior']['type'] == 'multiblock_item'
    block = item['behavior']['block']
    behavior, = block['behaviors']
    assert behavior['type'] == 'multiblock_block'
    coords = list(map(tuple,behavior['pieces']))
    assert coords == EXPECTED and len(set(coords)) == 15
    assert block['loot']['pools'] == [{'rolls':1,'entries':[{'type':'item','item':'vsrender:demo_statue_giant'}]}]
    assert block['settings'] == {'hardness':2.0,'resistance':5.0,'can_occlude':False,'is_suffocating':False,'is_redstone_conductor':False,'push_reaction':'block'}
    states = block['states']
    assert states['properties']['part'] == {'type':'int','default':0,'range':'0~14'}
    assert states['properties']['facing'] == {'type':'horizontal_direction','default':'north'}
    assert len(states['variants']) == 60 and len(states['appearances']) == 28
    boxes = []
    models = []
    for part,coord in enumerate(coords):
        model = read_model(f'demo_statue_part_{part}')
        models.append(model)
        for e in model['elements']:
            boxes.append(tuple(tuple(v+16*c for v,c in zip(e[key],coord)) for key in ('from','to')))
        for facing,rotation in [('north',0),('east',90),('south',180),('west',270)]:
            key = f'part={part},facing={facing}'
            appearance = states['appearances'][states['variants'][key]['appearance']]
            assert appearance == {'auto_state':'leaves','model':{'path':f'vsrender:block/demo_statue_part_{0 if part < 9 else part}','y':rotation}}
    assert [min(b[0][i] for b in boxes) for i in range(3)] == [-16,0,-16]
    assert [max(b[1][i] for b in boxes) for i in range(3)] == [32,80,32]
    reached = {0}
    while True:
        more = {j for j,b in enumerate(boxes) if j not in reached and any(connected(boxes[i],b) for i in reached)}
        if not more:
            break
        reached |= more
    assert len(reached) == len(boxes), 'Disconnected statue geometry'
    # Dark eyes are toward north, while hair occupies the rear (+Z).
    head = models[14]['elements']
    eyes = [e for e in head if e['faces']['north']['texture'] == '#dark' and e['to'][2] <= 3]
    assert len(eyes) == 2 and all(e['from'][1] >= 8 for e in eyes)
    assert any(e['from'][2] >= 12 and e['faces']['south']['texture'] == '#dark' for e in head)
    preview = read_model('demo_statue_preview')
    assert len(preview['elements']) == len(boxes)
    for e,b in zip(preview['elements'],boxes):
        for endpoint,key in enumerate(('from','to')):
            expected = [round(v*.18+(6.56 if i != 1 else .8),4) for i,v in enumerate(b[endpoint])]
            assert e[key] == expected, 'Preview must preserve complete statue geometry'
    print('Statue verified: 15 unique real cells, 60 variants, continuous 3x3x5 geometry, bounded preview.')

if __name__ == '__main__':
    main()
