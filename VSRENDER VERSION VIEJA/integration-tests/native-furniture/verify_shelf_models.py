#!/usr/bin/env python3
"""Check shelf placement variants and geometry before testing runtime interactions."""
import json,yaml
from pathlib import Path
root=Path(__file__).resolve().parents[2]/'examples/vsrender_demo'
items=yaml.safe_load((root/'configuration/shelf.yml').read_text())['items']
for name,count,appearances in [('demo_shelf',15,12),('demo_invisible_stand',1,1)]:
 block=items['vsrender:'+name]['behavior']['block'];pieces=block['behaviors'][0]['pieces'];states=block['states']
 assert pieces[0]==[0,0,0] and len(pieces)==len(set(map(tuple,pieces)))==count
 assert len(states['variants'])==4*count and len(states['appearances'])==appearances
 for part,(x,y,z) in enumerate(pieces):
  assert z==0 and 0<=y<5 and -1<=x<=1
  for face,rotation in [('north',0),('east',90),('south',180),('west',270)]:
   appearance=states['appearances'][states['variants'][f'part={part},facing={face}']['appearance']]
   model=json.loads((root/'resourcepack/assets/vsrender/models/block'/ (appearance['model']['path'].split('/')[-1]+'.json')).read_text())
   if name=='demo_invisible_stand':assert model['elements']==[]
   else:
    assert appearance['model']['y']==rotation
    for e in model['elements']:
     assert all(0<=a<b<=16 for a,b in zip(e['from'],e['to']))
     assert all('tintindex' not in f and 'cullface' not in f for f in e['faces'].values())
print('Shelf verified: five levels, fifteen native cells, four orientations, shared models; invisible stand has no geometry.')
