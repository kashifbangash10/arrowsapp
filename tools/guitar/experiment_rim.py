"""Deterministic rim-first partition experiment, separate review artifacts."""
import json
import sys
from functools import lru_cache
from pathlib import Path
from PIL import Image, ImageDraw
ROOT=Path(__file__).resolve().parents[2]
trace=json.loads((ROOT/'tools/guitar/trace.json').read_text(encoding='utf-8-sig'))
W,H=trace['cols'],trace['rows']
im=Image.new('1',(W,H));ImageDraw.Draw(im).polygon([(x+1,y+1) for x,y in trace['outline']],fill=1)
mask={(x,y) for y in range(H) for x in range(W) if im.getpixel((x,y))}
DIRS=[('LEFT',(-1,0)),('DOWN',(0,1)),('RIGHT',(1,0)),('UP',(0,-1))]
def row_runs(y):
    runs=[]
    for x in sorted(x for x,row in mask if row==y):
        if not runs or runs[-1][-1]!=x-1:runs.append([x])
        else:runs[-1].append(x)
    return runs
def rim(first,last,right_lobe=False):
    rows={y:(row_runs(y)[-1] if right_lobe else max(row_runs(y),key=len) if y==33 else row_runs(y)[0]) for y in range(first,last+1)}
    result=[(rows[first][-1],first)]
    def horizontal(x):
        cx,y=result[-1]
        while cx!=x:
            cx+=1 if x>cx else -1;result.append((cx,y))
    horizontal(rows[first][0])
    for y in range(first+1,last+1):
        target=rows[y][0]
        entry=rows[y][-1] if y==last and target>=result[-1][0] else max(result[-1][0],target)
        if entry!=result[-1][0]:horizontal(entry)
        result.append((entry,y));horizontal(target)
    assert len(set(result))==len(result)
    assert set(result)<=mask,('outside',set(result)-mask)
    assert result[-1][0]==result[-2][0]-1
    return result
def snake(w,h,vertical):
    if not vertical:return [(x,y) for y in range(h) for x in (range(w) if y%2==0 else range(w-1,-1,-1))]
    return [(x,y) for x in range(w) for y in (range(h) if x%2==0 else range(h-1,-1,-1))]
def variants(x,y,w,h):
    result=[]
    for vertical in ([False,True] if h>1 else [False]):
        for fx in [False,True]:
            for fy in [False,True]:
                p=[(x+(w-1-a if fx else a),y+(h-1-b if fy else b)) for a,b in snake(w,h,vertical)]
                vector=(p[-1][0]-p[-2][0],p[-1][1]-p[-2][1])
                result.append((next(n for n,v in DIRS if v==vector),p))
    return result
@lru_cache(None)
def maze_paths(w,h):
    # Enumerate compact editable template variants deterministically. No random
    # decisions, and the committed level still stores every resulting cell.
    points={(x,y)for x in range(w)for y in range(h)}
    results={name:[]for name,_ in DIRS}
    def visit(path,used):
        x,y=path[-1]
        if len(path)==len(points):
            dx=x-path[-2][0];dy=y-path[-2][1]
            if (x+dx,y+dy) in points:return
            direction=next(name for name,v in DIRS if v==(dx,dy))
            turns=sum((b[0]-a[0],b[1]-a[1])!=(c[0]-b[0],c[1]-b[1]) for a,b,c in zip(path,path[1:],path[2:]))
            results[direction].append((turns,list(path)))
            return
        for _,(dx,dy)in DIRS:
            nxt=x+dx,y+dy
            if nxt in points and nxt not in used:
                visit(path+[nxt],used|{nxt})
    for start in sorted(points,key=lambda p:(p[1],p[0])):
        if start[0] in (0,w-1) or start[1] in (0,h-1):visit([start],{start})
    for name,paths in results.items():
        maximum=max((turns for turns,path in paths),default=0)
        results[name]=[path for turns,path in paths if turns>=maximum-1]
    return results
def maze_variants(x,y,w,h):
    output=[]
    for name,paths in maze_paths(w,h).items():
        if paths:
            path=paths[(x*7+y*11)%len(paths)]
            output.append((name,[(x+a,y+b)for a,b in path]))
    return output
def swept(cells,vector):
    dx,dy=vector;result=set()
    for x,y in cells:
        x+=dx;y+=dy
        while 0<=x<W and 0<=y<H:
            result.add((x,y));x+=dx;y+=dy
    return result-set(cells)
body=rim(20,66);neck=rim(2,29,True)
assert not set(body)&set(neck)
tiles=[{'cells':set(p),'variants':[('LEFT',p)]}for p in [body,neck]]
remaining=mask-set(body)-set(neck)
palette=[(3,4),(4,3),(2,5),(5,2),(3,3),(2,4),(4,2),(2,3),(3,2)]
while remaining:
    x,y=min(remaining,key=lambda p:(p[1],p[0]));start=(x//3+y//2)%len(palette)
    options=palette[start:]+palette[:start]+[(2,2),(5,1),(4,1),(3,1),(2,1)]
    for w,h in options:
        cells={(x+a,y+b)for a in range(w)for b in range(h)}
        if cells<=remaining:
            tiles.append({'cells':cells,'variants':(maze_variants if '--winding' in sys.argv else variants)(x,y,w,h)});remaining-=cells;break
    else:remaining.remove((x,y))
owner={p:i for i,t in enumerate(tiles)for p in t['cells']}
for i,t in enumerate(tiles):
    t['choices']={}
    for name,path in t['variants']:
        blockers={owner[p]for p in swept(path,dict(DIRS)[name])if p in owner and owner[p]!=i}
        t['choices'].setdefault(name,(path,blockers))
left=set(range(len(tiles)));selected={};depth={};order=[];roots=[]
while left:
    candidates=[]
    for i in sorted(left):
        for direction,(path,blockers)in tiles[i]['choices'].items():
            if blockers&left:continue
            dep=max((depth[j]for j in blockers),default=0)
            score=(int(bool(blockers)),dep,len(blockers),len(path))
            if not blockers and i!=0:continue
            candidates.append((score,-i,i,direction,path,blockers))
    if not candidates:raise RuntimeError(('No one-root peel',len(left),sorted(left)))
    _,_,i,direction,path,blockers=max(candidates)
    selected[i]=direction,path,blockers;depth[i]=max((depth[j]for j in blockers),default=0)+1
    if not blockers:roots.append(i)
    left.remove(i);order.append(i)
arrows=[{'id':i+1,'exit':selected[i][0],'cells':selected[i][1]}for i in range(len(tiles))]
result={'cols':W,'rows':H,'arrows':arrows,'solution':[i+1 for i in order],'metrics':{'arrows':len(tiles),'openingMoves':[i+1 for i in roots],'dependencyDepth':max(depth.values()),'occupiedCells':len(owner),'tracedCells':len(mask),'bodyRimLength':len(body),'neckRimLength':len(neck)}}
(ROOT/('tools/guitar/layout-rim-winding.json' if '--winding' in sys.argv else 'tools/guitar/layout-rim.json')).write_text(json.dumps(result,indent=2)+'\n',encoding='utf-8');print(result['metrics'])
s=14;pad=20;im=Image.new('RGB',(W*s+pad*2,H*s+pad*2),'#101318');draw=ImageDraw.Draw(im)
for a in arrows:
    points=[(pad+(x+.5)*s,pad+(y+.5)*s)for x,y in a['cells']]
    draw.line(points,fill='white',width=3,joint='curve')
    x,y=points[-1];dx,dy=dict(DIRS)[a['exit']];tip=(x+dx*s*.32,y+dy*s*.32);bx=tip[0]-dx*s*.52;by=tip[1]-dy*s*.52
    draw.polygon([tip,(bx-dy*s*.28,by+dx*s*.28),(bx+dy*s*.28,by-dx*s*.28)],fill='white')
im.save(ROOT/('artifacts/guitar/draft-rim-winding.png' if '--winding' in sys.argv else 'artifacts/guitar/draft-rim.png'))
