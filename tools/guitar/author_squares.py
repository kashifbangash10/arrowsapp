"""Deterministic editable trace authoring; all final paths are committed explicitly."""
import json
from pathlib import Path
from PIL import Image, ImageDraw
ROOT=Path(__file__).resolve().parents[2]
trace=json.loads((ROOT/'tools/guitar/trace.json').read_text(encoding='utf-8-sig'))
W,H=trace['cols'],trace['rows'];DIRS=[('LEFT',(-1,0)),('DOWN',(0,1)),('RIGHT',(1,0)),('UP',(0,-1))]
im=Image.new('1',(W,H));ImageDraw.Draw(im).polygon([(x+1,y+1) for x,y in trace['outline']],fill=1)
mask={(x,y) for y in range(H) for x in range(W) if im.getpixel((x,y))}
def neighbors(p):
    x,y=p
    return [(x+dx,y+dy) for _,(dx,dy) in DIRS]
def rays(cells, vector):
    dx,dy=vector;result=set()
    for x,y in cells:
        x+=dx;y+=dy
        while 0<=x<W and 0<=y<H:
            result.add((x,y));x+=dx;y+=dy
    return result-set(cells)
def find_path(cells, direction, budget=20000):
    if len(cells)<2:return None
    dx,dy=direction;cells=set(cells)
    candidates=[p for p in cells if (p[0]-dx,p[1]-dy) in cells and not any((p[0]+k*dx,p[1]+k*dy) in cells for k in range(1,max(W,H)))]
    candidates.sort(key=lambda p:(p[1],p[0]))
    adjacency={p:[q for q in neighbors(p) if q in cells] for p in cells}
    nodes=0
    def visit(path,left):
        nonlocal nodes
        nodes+=1
        if nodes>budget:return None
        if not left:return path[::-1]
        p=path[-1];options=[q for q in adjacency[p] if q in left]
        prev=path[-2] if len(path)>1 else p
        options.sort(key=lambda q:(sum(r in left for r in adjacency[q]),(q[0]-p[0],q[1]-p[1])==(p[0]-prev[0],p[1]-prev[1]),q[1],q[0]))
        for q in options:
            rest=left-{q}
            if rest:
                seen=set();stack=[q]
                while stack:
                    v=stack.pop()
                    for n in adjacency[v]:
                        if n in rest and n not in seen:seen.add(n);stack.append(n)
                if len(seen)!=len(rest):continue
                if sum(sum(n in rest or n==q for n in adjacency[v])==1 for v in rest)>1:continue
            answer=visit(path+[q],rest)
            if answer:return answer
        return None
    parity=sum(1 if (x+y)%2 else -1 for x,y in cells)
    if abs(parity)>1:return None
    for head in candidates:
        if parity and (1 if sum(head)%2 else -1)!=parity:continue
        prev=(head[0]-dx,head[1]-dy)
        answer=visit([head,prev],cells-{head,prev})
        if answer:return answer
    return None
remaining=set(mask);tiles=[]
palette=[(2,2)]
while remaining:
    x,y=min(remaining,key=lambda p:(p[1],p[0]));preferred=(x//3+y//2)%len(palette)
    options=palette[preferred:]+palette[:preferred]+[(1,1)]
    for w,h in options:
        cells={(x+a,y+b) for a in range(w) for b in range(h)}
        if cells<=remaining:
            tiles.append(cells);remaining-=cells;break
for iteration in range(0):
    merged=0
    for i,cells in enumerate(tiles):
        if not cells:continue
        xs={p[0] for p in cells};ys={p[1] for p in cells}
        if len(xs)>1 and len(ys)>1:continue
        near={q for p in cells for q in neighbors(p)}-cells
        candidates=[j for j,c in enumerate(tiles) if j!=i and c and c&near and len(c|cells)<=32]
        candidates.sort(key=lambda j:(len(tiles[j]|cells),min(y for x,y in tiles[j]),min(x for x,y in tiles[j])))
        for j in candidates:
            union=cells|tiles[j]
            vs=[(name,find_path(union,d,budget=10000)) for name,d in DIRS]
            good=[(name,p) for name,p in vs if p]
            if len(good)>=3 or (len(good)>=2 and len(xs)*len(ys)==1):
                tiles[j]=union;tiles[i]=set();merged+=1;break
    print('MERGED',iteration,merged,flush=True)
tiles=[t for t in tiles if len(t)>=2];choices=[]
for i,cells in enumerate(tiles):
    vs=[(name,find_path(cells,d)) for name,d in DIRS]
    valid={name:p for name,p in vs if p}
    if not valid:raise Exception(('NO PATH',i,cells))
    choices.append(valid)
owner={p:i for i,t in enumerate(tiles) for p in t}
blockers=[{name:{owner[p] for p in rays(tiles[i],dict(DIRS)[name]) if p in owner and owner[p]!=i} for name in choices[i]} for i in range(len(tiles))]
def closure(seed):
    done=set(seed)
    while True:
        more={i for i,b in enumerate(blockers) if i not in done and any(bs and bs<=done for bs in b.values())}
        if not more:return done
        done|=more
root_candidates=[i for i,b in enumerate(blockers) if any(not bs for bs in b.values())]
seeds=set();covered=set()
while len(covered)<len(tiles):
    opts=[(len(closure(covered|{i})),i) for i in root_candidates if i not in covered]
    if not opts:raise Exception('Cannot cover')
    _,i=max(opts);seeds.add(i);covered=closure(covered|{i})
print('SEEDS',len(seeds),'TILES',len(tiles),'COVER',len(owner),flush=True)
left=set(range(len(tiles)));depth={};order=[];selected={};roots=[]
while left:
    options=[]
    for i in sorted(left):
        for name,bs in blockers[i].items():
            if bs&left or (not bs and i not in seeds):continue
            dep=max((depth[j] for j in bs),default=0)
            options.append(((bool(bs),dep,len(bs),len(tiles[i])),i,name,bs))
    if not options:raise Exception('Deadlock')
    _,i,name,bs=max(options,key=lambda x:(x[0],-x[1]))
    if not bs:roots.append(i)
    depth[i]=max((depth[j] for j in bs),default=0)+1
    selected[i]=(name,choices[i][name]);left.remove(i);order.append(i)
arrows=[{'id':i+1,'exit':selected[i][0],'cells':selected[i][1]} for i in range(len(tiles))]
result={'cols':W,'rows':H,'arrows':arrows,'solution':[i+1 for i in order], 'metrics':{'arrows':len(tiles),'openingMoves':[i+1 for i in roots], 'dependencyDepth':max(depth.values()),'occupiedCells':len(owner),'tracedCells':len(mask)}}
(ROOT/'tools/guitar/squares_layout.json').write_text(json.dumps(result,indent=2)+'\n');print(result['metrics'],flush=True)
s=14;pad=20;im=Image.new('RGB',(W*s+pad*2,H*s+pad*2),'#101318');draw=ImageDraw.Draw(im)
for a in arrows:
    points=[(pad+(x+.5)*s,pad+(y+.5)*s) for x,y in a['cells']];draw.line(points,fill='white',width=3,joint='curve')
    x,y=points[-1];dx,dy=dict(DIRS)[a['exit']];tip=(x+dx*s*.32,y+dy*s*.32);bx=tip[0]-dx*s*.52;by=tip[1]-dy*s*.52
    draw.polygon([tip,(bx-dy*s*.28,by+dx*s*.28),(bx+dy*s*.28,by-dx*s*.28)],fill='white')
im.save(ROOT/'artifacts/guitar/squares-draft.png')
