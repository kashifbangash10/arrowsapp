"""Offline, deterministic layout authoring from the manually traced reference.
No random source is used. Android only loads the committed explicit arrow data.
"""
import json, math
from pathlib import Path
from PIL import Image, ImageDraw
ROOT = Path(__file__).resolve().parents[2]
trace = json.loads((ROOT/'tools/guitar/trace.json').read_text(encoding='utf-8-sig'))
W,H = trace['cols'],trace['rows']
im = Image.new('1',(W,H)); d=ImageDraw.Draw(im)
d.polygon([(x+1,y+1) for x,y in trace['outline']],fill=1)
mask = {(x,y) for y in range(H) for x in range(W) if im.getpixel((x,y))}
DIRS = [('LEFT',(-1,0)),('DOWN',(0,1)),('RIGHT',(1,0)),('UP',(0,-1))]

def rays(cells, direction):
    dx,dy=direction; result=set()
    for x,y in cells:
        x+=dx; y+=dy
        while 0<=x<W and 0<=y<H:
            result.add((x,y)); x+=dx; y+=dy
    return result-set(cells)

def tile_shape(w,h,mode):
    # Four serpentine orientations allow a head on any outer face.
    if mode==0:
        return [(x,y) for y in range(h) for x in (range(w) if y%2==0 else range(w-1,-1,-1))]
    return [(x,y) for x in range(w) for y in (range(h) if x%2==0 else range(h-1,-1,-1))]

def variants(x,y,w,h):
    out=[]
    for mode in [0,1]:
        for fx in [False,True]:
            for fy in [False,True]:
                p=[(x+(w-1-a if fx else a),y+(h-1-b if fy else b)) for a,b in tile_shape(w,h,mode)]
                dx=p[-1][0]-p[-2][0];dy=p[-1][1]-p[-2][1]
                direction=next(n for n,v in DIRS if v==(dx,dy))
                # Keep the arrow's own head corridor clear, for compatibility with editor checks.
                hx,hy=p[-1]; own=set(p)
                hx+=dx;hy+=dy
                self_block=False
                while 0<=hx<W and 0<=hy<H:
                    if (hx,hy) in own: self_block=True
                    hx+=dx;hy+=dy
                if not self_block: out.append((direction,p))
    return out

remaining=set(mask);tiles=[]
# Repeating authored template palette, varied by position, no random generation.
palette=[(3,4),(4,3),(2,5),(5,2),(3,3),(2,4),(4,2),(2,3),(3,2)]
while remaining:
    x,y=min(remaining,key=lambda p:(p[1],p[0]))
    preferred=(x//3+y//2)%len(palette)
    options=palette[preferred:]+palette[:preferred]+[(2,2),(1,5),(5,1),(1,4),(4,1),(1,3),(3,1),(1,2),(2,1)]
    found=False
    for w,h in options:
        cells={(x+a,y+b) for a in range(w) for b in range(h)}
        if cells<=remaining:
            tiles.append({'cells':cells,'variants':variants(x,y,w,h),'box':(x,y,w,h)})
            remaining-=cells;found=True;break
    if not found:
        remaining.remove((x,y)) # isolated contour cell: gap is under one grid unit
print('MASK',len(mask),'TILES',len(tiles),'COVERED',sum(len(t['cells']) for t in tiles))
# A direction is fixed only when the complete shape can exit. Prefer pieces that
# depend on the most recent removals, building long provable dependency chains.
owner={p:i for i,t in enumerate(tiles) for p in t['cells']}
for i,t in enumerate(tiles):
    t['choices']={}
    for name,path in t['variants']:
        vector=dict(DIRS)[name]
        blockers={owner[p] for p in rays(t['cells'],vector) if p in owner and owner[p]!=i}
        t['choices'].setdefault(name,[]).append((path,blockers))
def closure(seed):
    done=set(seed)
    while True:
        more={i for i,t in enumerate(tiles) if i not in done and any(bs and bs<=done for vs in t['choices'].values() for _,bs in vs)}
        if not more: return done
        done|=more
root_candidates=[i for i,t in enumerate(tiles) if any(not bs for vs in t['choices'].values() for _,bs in vs)]
seeds=set(); covered=set()
while len(covered)<len(tiles):
    options=[(len(closure(covered|{i})),i) for i in root_candidates if i not in covered]
    if not options: break
    _,i=max(options); seeds.add(i); covered=closure(covered|{i})
print('SEEDS',len(seeds),[i+1 for i in seeds], 'CLOSURE',len(covered))
left=set(range(len(tiles))); removed=set(); depth={}; order=[]; selected={}; roots=[]
while left:
    choices=[]
    for i in sorted(left):
        t=tiles[i]
        for direction,vs in t['choices'].items():
            path,blockers=vs[0]
            if blockers & left or (not blockers and i not in seeds): continue
            dep=max((depth[j] for j in blockers),default=0)
            # Favor a deep chain, then a narrow local opening. First root is body bottom.
            x,y,w,h=t['box']
            if not order:
                score=(0,y,-x,len(path))
            else:
                score=(int(bool(blockers)),dep,len(blockers),len(path))
            choices.append((score,i,direction,path,blockers))
    if not choices: raise RuntimeError(f'No peelable piece: {len(left)}')
    _,i,direction,path,blockers=max(choices,key=lambda c:(c[0],-c[1]))
    if not blockers: roots.append(i)
    depth[i]=max((depth[j] for j in blockers),default=0)+1
    selected[i]=(direction,path,blockers)
    left.remove(i);removed.add(i);order.append(i)
# Stable IDs based on reading order, deliberately independent of solution order.
arrows=[{'id':i+1,'exit':selected[i][0],'cells':selected[i][1]} for i in range(len(tiles))]
result={'cols':W,'rows':H,'arrows':arrows,'solution':[i+1 for i in order],
        'metrics':{'arrows':len(tiles),'openingMoves':[i+1 for i in roots], 'dependencyDepth':max(depth.values()),'occupiedCells':len(owner),'tracedCells':len(mask)}}
(ROOT/'tools/guitar/layout.json').write_text(json.dumps(result,indent=2)+'\n')
print(result['metrics'])
# Draft board preview. The final preview will be rendered from committed level data.
s=14;pad=20
im=Image.new('RGB',(W*s+pad*2,H*s+pad*2),'#101318');draw=ImageDraw.Draw(im)
for a in arrows:
    points=[(pad+(x+.5)*s,pad+(y+.5)*s) for x,y in a['cells']]
    draw.line(points,fill='white',width=3,joint='curve')
    x,y=points[-1];dx,dy=dict(DIRS)[a['exit']]
    tip=(x+dx*s*.32,y+dy*s*.32);bx=tip[0]-dx*s*.52;by=tip[1]-dy*s*.52
    draw.polygon([tip,(bx-dy*s*.28,by+dx*s*.28),(bx+dy*s*.28,by-dx*s*.28)],fill='white')
im.save(ROOT/'artifacts/guitar/draft.png')
