"""Validate and render the actual committed Kotlin level, not an authoring draft.
Run: python tools/guitar/audit_level.py. Requires Pillow only for the PNG preview.
"""
import hashlib, json, math, re
from pathlib import Path
from PIL import Image, ImageDraw
ROOT=Path(__file__).resolve().parents[2]
SOURCE=ROOT/'app/src/main/java/com/amowpuzzle/aitricker/data/level/GuitarLevel.kt'
src=SOURCE.read_text(encoding='utf-8-sig');W=H=69
D={'UP':(0,-1),'RIGHT':(1,0),'DOWN':(0,1),'LEFT':(-1,0)}
arrows=[]
for match in re.finditer(r'arrow\((\d+), Direction\.(\w+), ([\d, ]+)\)',src):
    id=int(match[1]);direction=match[2];nums=list(map(int,match[3].split(',')));vertices=list(zip(nums[::2],nums[1::2]));cells=[vertices[0]]
    for p,q in zip(vertices,vertices[1:]):
        assert (p[0]==q[0]) != (p[1]==q[1]),(id,p,q)
        dx=(q[0]>p[0])-(q[0]<p[0]);dy=(q[1]>p[1])-(q[1]<p[1]);x,y=p
        while (x,y)!=q:x+=dx;y+=dy;cells.append((x,y))
    assert len(cells)==len(set(cells)),('self-overlap',id)
    assert (cells[-1][0]-cells[-2][0],cells[-1][1]-cells[-2][1])==D[direction]
    assert all(0<=x<W and 0<=y<H for x,y in cells)
    arrows.append({'id':id,'exit':direction,'cells':cells})
assert len(arrows)==115 and len({a['id'] for a in arrows})==115
allcells=[p for a in arrows for p in a['cells']];assert len(allcells)==len(set(allcells))
owner={p:a['id'] for a in arrows for p in a['cells']}
bs={}
for a in arrows:
    contacts=set();dx,dy=D[a['exit']]
    for x,y in a['cells']:
        x+=dx;y+=dy
        while 0<=x<W and 0<=y<H:
            b=owner.get((x,y))
            if b is not None and b!=a['id']:contacts.add(b)
            x+=dx;y+=dy
    bs[a['id']]=contacts
left=set(bs);rounds=[]
while left:
    free=sorted(i for i in left if not(bs[i]&left));assert free,('cycle',left)
    rounds.append(free);left-=set(free)
sequence=list(map(int,re.search(r'val solutionIds: List<Int> = listOf\(\s*([\d,\s]+)\)',src)[1].replace('\n','').split(',')))
assert len(sequence)==len(set(sequence))==len(arrows)
byid={a['id']:a for a in arrows};active=dict(byid)
# Independent replay: translate EVERY original body cell one step at a time.
for id in sequence:
    a=active.pop(id);obstacles={p for b in active.values() for p in b['cells']};dx,dy=D[a['exit']]
    for distance in range(1,max(W,H)+1):
        shifted={(x+dx*distance,y+dy*distance) for x,y in a['cells']}
        assert not shifted&obstacles,('illegal sweep',id,distance)
assert not active
assert rounds[0]==[110] and len(rounds)==62
trace=json.loads((ROOT/'tools/guitar/trace.json').read_text(encoding='utf-8-sig'))
maskimage=Image.new('1',(W,H));ImageDraw.Draw(maskimage).polygon([(x+1,y+1) for x,y in trace['outline']],fill=1)
mask={(x,y) for y in range(H) for x in range(W) if maskimage.getpixel((x,y))}
assert set(allcells)<=mask
metrics={'source_sha256':hashlib.sha256(SOURCE.read_bytes()).hexdigest(),'arrows':len(arrows),'initially_free':rounds[0], 'dependency_rounds':len(rounds),'occupied_cells':len(allcells),'traced_mask_cells':len(mask),'trace_coverage_percent':round(100*len(allcells)/len(mask),2),'bounds':[min(x for x,y in allcells),min(y for x,y in allcells),max(x for x,y in allcells),max(y for x,y in allcells)],'directions':{d:sum(a['exit']==d for a in arrows) for d in D},'shortest_path_cells':min(len(a['cells']) for a in arrows),'longest_path_cells':max(len(a['cells']) for a in arrows),'turns':sum(sum((q[0]-p[0],q[1]-p[1])!=(r[0]-q[0],r[1]-q[1]) for p,q,r in zip(a['cells'],a['cells'][1:],a['cells'][2:])) for a in arrows),'solvable':True,'independent_full_body_replay':True}
OUT=ROOT/'artifacts/guitar';OUT.mkdir(exist_ok=True,parents=True)
(OUT/'validation.json').write_text(json.dumps(metrics,indent=2)+'\n')
(OUT/'solution.txt').write_text('LEVEL 1 GUITAR - complete valid removal sequence (stable arrow IDs)\n\n'+', '.join(map(str,sequence))+'\n\nDependency rounds (simultaneously free at each round):\n'+'\n'.join(f'{n+1}: '+', '.join(map(str,r)) for n,r in enumerate(rounds))+'\n')
(OUT/'blocking-graph.json').write_text(json.dumps({str(i):sorted(b) for i,b in bs.items()},indent=2)+'\n')
# Match Android BoardGeometry: .20 stroke, .10 corner radius, .12 tail extension,
# .32 head tip, .52 head length and .28 half width. No background picture layer.
size=1104;unit=size/W;AA=3
im=Image.new('RGB',(size*AA,size*AA),'#101318');draw=ImageDraw.Draw(im)
svg=[f'<svg xmlns="http://www.w3.org/2000/svg" width="{size}" height="{size}" viewBox="0 0 69 69"><rect width="69" height="69" fill="#101318"/>']
labels=[]
def px(p):return (p[0]*unit*AA,p[1]*unit*AA)
for a in arrows:
    points=[(x+.5,y+.5) for x,y in a['cells']]
    first,second=points[:2];points.insert(0,(first[0]-(second[0]-first[0])*.12,first[1]-(second[1]-first[1])*.12))
    cmd=[f'M {points[0][0]} {points[0][1]}'];line=[points[0]]
    for prev,cur,nxt in zip(points,points[1:],points[2:]):
        d1=(cur[0]-prev[0],cur[1]-prev[1]);d2=(nxt[0]-cur[0],nxt[1]-cur[1]);l1=math.hypot(*d1);l2=math.hypot(*d2);r=min(.10,l1*.45,l2*.45)
        p1=(cur[0]-d1[0]/l1*r,cur[1]-d1[1]/l1*r);p2=(cur[0]+d2[0]/l2*r,cur[1]+d2[1]/l2*r)
        cmd.append(f'L {p1[0]} {p1[1]} Q {cur[0]} {cur[1]} {p2[0]} {p2[1]}');line.append(p1)
        for n in range(1,9):
            t=n/8;line.append(((1-t)**2*p1[0]+2*(1-t)*t*cur[0]+t*t*p2[0],(1-t)**2*p1[1]+2*(1-t)*t*cur[1]+t*t*p2[1]))
    cmd.append(f'L {points[-1][0]} {points[-1][1]}');line.append(points[-1])
    svg.append(f'<path id="arrow-{a["id"]}" d="{" ".join(cmd)}" fill="none" stroke="#FFFFFF" stroke-width=".20" stroke-linecap="round" stroke-linejoin="round"/>')
    width=round(.20*unit*AA);draw.line([px(p) for p in line],fill='white',width=width,joint='curve')
    for p in [line[0],line[-1]]:
        x,y=px(p);r=width/2;draw.ellipse((x-r,y-r,x+r,y+r),fill='white')
    x,y=points[-1];dx,dy=D[a['exit']];tip=(x+dx*.32,y+dy*.32);bx=tip[0]-dx*.52;by=tip[1]-dy*.52
    triangle=[tip,(bx-dy*.28,by+dx*.28),(bx+dy*.28,by-dx*.28)]
    svg.append('<polygon points="'+' '.join(f'{x},{y}' for x,y in triangle)+'" fill="#FFFFFF"/>');draw.polygon([px(p) for p in triangle],fill='white')
    labels.append(f'<text x="{x+.35}" y="{y-.3}" font-size=".65" fill="#FFFFFF" font-family="sans-serif" paint-order="stroke" stroke="#101318" stroke-width=".16">{a["id"]}</text>')
(OUT/'guitar-preview.svg').write_text('\n'.join(svg+['</svg>']))
(OUT/'guitar-arrow-ids.svg').write_text('\n'.join(svg+labels+['</svg>']))
im.resize((size,size),Image.Resampling.LANCZOS).save(OUT/'guitar-preview.png')
print(json.dumps(metrics,indent=2))
