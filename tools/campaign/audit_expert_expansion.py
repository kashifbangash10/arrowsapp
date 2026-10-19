"""Read-only independent audit of emitted expert JSON and its removal certificates."""
import hashlib,json
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2];OUT=ROOT/'artifacts/campaign/expansion'
V={'UP':(0,-1),'DOWN':(0,1),'LEFT':(-1,0),'RIGHT':(1,0)}

def audit():
 rows=[];masks=set();layouts=set()
 for lid in range(51,101):
  l=json.loads((OUT/f'level-{lid}.json').read_text());assert l['id']==lid
  arrows={a['id']:a for a in l['arrows']};assert len(arrows)==len(l['arrows'])>=100
  owner={};deps={};turns=0;directions=set()
  for aid,a in arrows.items():
   cells=[tuple(c) for c in a['cells']];assert len(cells)>=2 and len(cells)==len(set(cells))
   for x,y in cells:
    assert 0<=x<l['cols'] and 0<=y<l['rows']
    assert (x,y) not in owner;owner[x,y]=aid
   for p,q in zip(cells,cells[1:]):assert abs(p[0]-q[0])+abs(p[1]-q[1])==1
   d=V[a['exit']];directions.add(a['exit']);assert (cells[-1][0]-cells[-2][0],cells[-1][1]-cells[-2][1])==d
   turns+=sum((q[0]-p[0],q[1]-p[1])!=(r[0]-q[0],r[1]-q[1]) for p,q,r in zip(cells,cells[1:],cells[2:]))
  assert set(owner)=={tuple(c) for c in l['mask']},('Mask mismatch',lid)
  for aid,a in arrows.items():
   x,y=a['cells'][-1];dx,dy=V[a['exit']];bs=set();x+=dx;y+=dy
   while 0<=x<l['cols'] and 0<=y<l['rows']:
    if (x,y) in owner:bs.add(owner[x,y])
    x+=dx;y+=dy
   assert aid not in bs,('Own ray blocked',lid,aid);deps[aid]=bs
  left=set(arrows);initial=sorted(aid for aid in left if not deps[aid]);assert initial==sorted(l['metrics']['openingMoves'])
  assert len(initial)<=max(3,len(arrows)//16)
  assert len(l['solution'])==len(arrows)
  for aid in l['solution']:
   assert aid in left and not deps[aid]&left,('Illegal certificate',lid,aid)
   left.remove(aid)
  assert not left
  left=set(arrows);rounds=0
  while left:
   free={aid for aid in left if not deps[aid]&left};assert free,('Dependency cycle',lid)
   left-=free;rounds+=1
  assert rounds==l['metrics']['dependencyRounds'] and rounds>=max(30,35 if len(initial)>3 else 30)
  norm=sorted(owner);ox=min(x for x,y in norm);oy=min(y for x,y in norm)
  masksha=hashlib.sha256(json.dumps(sorted((x-ox,y-oy) for x,y in norm)).encode()).hexdigest();assert masksha not in masks,('Duplicate silhouette',lid);masks.add(masksha)
  geometrysha=hashlib.sha256(json.dumps([(a['id'],a['exit'],a['cells']) for a in l['arrows']]).encode()).hexdigest();assert geometrysha not in layouts;layouts.add(geometrysha)
  rows.append({'level':lid,'name':l['name'],'arrows':len(arrows),'openings':initial,'dependencyRounds':rounds,'occupiedCells':len(owner),'maskCoverage':1.0,'turns':turns,'directions':sorted(directions),'maskSha256':masksha,'geometrySha256':geometrysha,'certificate':'PASS'})
 report={'result':'PASS','levelCount':len(rows),'distinctSilhouettes':len(masks),'distinctLayouts':len(layouts),'arrows':[min(r['arrows'] for r in rows),max(r['arrows'] for r in rows)],'openingMoves':[min(len(r['openings']) for r in rows),max(len(r['openings']) for r in rows)],'dependencyRounds':[min(r['dependencyRounds'] for r in rows),max(r['dependencyRounds'] for r in rows)],'totalArrows':sum(r['arrows'] for r in rows),'totalCells':sum(r['occupiedCells'] for r in rows),'totalTurns':sum(r['turns'] for r in rows),'levels':rows}
 (OUT/'independent-audit.json').write_text(json.dumps(report,indent=2)+'\n')
 print(json.dumps({k:v for k,v in report.items() if k!='levels'},indent=2))

if __name__=='__main__':audit()
