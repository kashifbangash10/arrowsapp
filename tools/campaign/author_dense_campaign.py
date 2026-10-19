"""Offline deterministic authoring for the 2026-10-12 hard/expert campaign.

All runtime layouts are explicit Kotlin; this tool is never bundled into the app.
Movement follows each path, then its self-clear head ray (HEAD_CORRIDOR).
"""
import argparse, hashlib, importlib.util, json, random, re
from pathlib import Path
from PIL import Image, ImageDraw
ROOT=Path(__file__).resolve().parents[2]
OUT=ROOT/'artifacts/campaign/dense'
OUT.mkdir(parents=True,exist_ok=True)
spec=importlib.util.spec_from_file_location('shape_helpers',ROOT/'tools/campaign/author_shapes.py')
helper=importlib.util.module_from_spec(spec);spec.loader.exec_module(helper)
DIRS=helper.DIRS;VECTORS=dict(DIRS)

def self_clear(path,direction):
    dx,dy=VECTORS[direction];x,y=path[-1]
    return not any((px-x)*dy==(py-y)*dx and (px-x)*dx+(py-y)*dy>0 for px,py in path[:-1])

def choices_for_tiles(tiles,w,h,original=None):
    helper.W,helper.H=w,h;result=[]
    for i,cells in enumerate(tiles):
        choices={}
        for name,d in DIRS:
            p=helper.find_path(cells,d,budget=15000)
            if p:choices[name]=p
        if original:
            p=[tuple(v) for v in original[i]['cells']];name=original[i]['exit']
            if self_clear(p,name):choices[name]=p
            p=p[::-1];d=(p[-1][0]-p[-2][0],p[-1][1]-p[-2][1])
            name=next(n for n,v in DIRS if v==d)
            if self_clear(p,name):choices.setdefault(name,p)
        if not choices:raise ValueError(('No self-clear path',i,len(cells)))
        result.append(choices)
    return result

def dependencies(tiles,choices,w,h):
    owner={p:i for i,t in enumerate(tiles) for p in t};blockers=[]
    for i,alternatives in enumerate(choices):
        entries={}
        for name,path in alternatives.items():
            assert self_clear(path,name.split('|')[0])
            dx,dy=VECTORS[name.split('|')[0]];x,y=path[-1];bs=set();x+=dx;y+=dy
            while 0<=x<w and 0<=y<h:
                if (x,y) in owner:bs.add(owner[(x,y)])
                x+=dx;y+=dy
            assert i not in bs
            entries[name]=bs
        blockers.append(entries)
    return blockers

def choose_directions(tiles,choices,w,h,target,openings,seed):
    blockers=dependencies(tiles,choices,w,h)
    def closure(seedset):
        done=set(seedset)
        while True:
            more={i for i,b in enumerate(blockers) if i not in done and any(bs and bs<=done for bs in b.values())}
            if not more:return done
            done|=more
    root_candidates=[i for i,b in enumerate(blockers) if any(not bs for bs in b.values())]
    roots=[];covered=set()
    while len(covered)<len(tiles):
        opts=[(len(closure(covered|{i})),i) for i in root_candidates if i not in covered]
        if not opts:raise ValueError(('Unreachable cluster',len(covered),len(tiles)))
        _,i=max(opts);roots.append(i);covered=closure(covered|{i})
    best=None
    for attempt in range(12):
        rng=random.Random(seed*101+attempt)
        seeds=set(roots);candidates=list(root_candidates);rng.shuffle(candidates)
        for candidate in candidates:
            if len(seeds)>=openings:break
            seeds.add(candidate)
        for bias in [-1,0,2,4,6,8,10,12,16,24,48]:
            left=set(range(len(tiles)));depth={};order=[];selected={};actual_roots=[]
            tie={i:rng.random() for i in range(len(tiles))}
            while left:
                options=[]
                for i in sorted(left):
                    for name,bs in blockers[i].items():
                        if bs&left or (not bs and i not in seeds):continue
                        dep=max((depth[j] for j in bs),default=0)
                        options.append(((not bool(bs),-abs(dep-bias),len(bs),tie[i]),i,name,bs))
                if not options:raise ValueError('Cycle')
                _,i,name,bs=max(options,key=lambda x:x[0])
                if not bs:actual_roots.append(i)
                depth[i]=max((depth[j] for j in bs),default=0)+1
                selected[i]=(name,choices[i][name]);left.remove(i);order.append(i)
            score=abs(max(depth.values())-target)+max(0,len(actual_roots)-openings)*10
            if best is None or score<best[0]:best=(score,depth,order,selected,actual_roots)
            if score==0:break
        if best[0]==0:break
    _,depth,order,selected,roots=best
    arrows=[{'id':i+1,'exit':selected[i][0].split('|')[0],'cells':selected[i][1]} for i in range(len(tiles))]
    return arrows,[i+1 for i in order],{'arrows':len(arrows),'openingMoves':[i+1 for i in roots],'dependencyRounds':max(depth.values())}

def validate(level):
    arrows={a['id']:a for a in level['arrows']};owner={tuple(c):a['id'] for a in arrows.values() for c in a['cells']}
    assert len(owner)==sum(len(a['cells']) for a in arrows.values())
    for a in arrows.values():
        cells=[tuple(p) for p in a['cells']]
        assert len(cells)>=2 and len(set(cells))==len(cells)
        assert all(0<=x<level['cols'] and 0<=y<level['rows'] for x,y in cells)
        assert all(abs(x[0]-y[0])+abs(x[1]-y[1])==1 for x,y in zip(cells,cells[1:]))
        assert (cells[-1][0]-cells[-2][0],cells[-1][1]-cells[-2][1])==VECTORS[a['exit']]
        assert self_clear(cells,a['exit'])
    def clear(aid,left):
        # Independent head ray. The remainder follows its already empty original route.
        a=arrows[aid];dx,dy=VECTORS[a['exit']];x,y=a['cells'][-1]
        for k in range(1,max(level['cols'],level['rows'])+1):
            if owner.get((x+k*dx,y+k*dy)) in left:return False
        return True
    left=set(arrows);openings=[i for i in sorted(left) if clear(i,left)]
    for aid in level['solution']:
        assert aid in left and clear(aid,left),('Blocked',level['id'],aid)
        left.remove(aid)
    assert not left
    left=set(arrows);rounds=0
    while left:
        free={i for i in left if clear(i,left)};assert free
        left-=free;rounds+=1
    assert sorted(openings)==sorted(level['metrics']['openingMoves'])
    assert rounds==level['metrics']['dependencyRounds']
    lengths=[len(a['cells']) for a in arrows.values()]
    turns=[sum((b[0]-a[0],b[1]-a[1])!=(c[0]-b[0],c[1]-b[1]) for a,b,c in zip(ar['cells'],ar['cells'][1:],ar['cells'][2:])) for ar in arrows.values()]
    level['metrics'].update({'occupiedCells':len(owner),'averageCells':round(sum(lengths)/len(lengths),2),'minCells':min(lengths),'maxCells':max(lengths),'turns':sum(turns),'windingArrows':sum(t>=3 for t in turns),'coverage':round(len(owner)/len(level['mask']),4),'independentValidation':'PASS'})
    return level


def cluster_mask(mask,w,h,seed,targets=None):
    helper.W,helper.H=w,h
    remaining=set(mask);tiles=[]
    palette=[(2,2)]
    while remaining:
        x,y=min(remaining,key=lambda p:(p[1],p[0]));preferred=(x//3+y//2)%len(palette)
        options=palette[preferred:]+palette[:preferred]+[(1,1)]
        for w,h in options:
            cells={(x+a,y+b) for a in range(w) for b in range(h)}
            if cells<=remaining:
                tiles.append(cells);remaining-=cells;break
    random.Random(seed * 197 + 31).shuffle(tiles)
    # Compose the small authored blocks into winding, asymmetric compact paths.
    for i,cells in enumerate(tiles):
        if len(cells)<4:continue
        x=min(p[0] for p in cells);y=min(p[1] for p in cells)
        palette = targets or [8,12,16,20,12,24,28,8,16]
        target=palette[(x//2+y//2+seed)%len(palette)]
        while len(tiles[i])<target:
            cells=tiles[i];near={q for p in cells for q in helper.neighbors(p)}-cells
            candidates=[j for j,c in enumerate(tiles) if j>i and len(c)==4 and c&near and len(c|cells)<=target]
            candidates.sort(key=lambda j:(abs(min(x for x,y in tiles[j])-x)+abs(min(y for x,y in tiles[j])-y),j))
            for j in candidates:
                union=cells|tiles[j]
                if all(helper.find_path(union,d,budget=10000) for _,d in DIRS):
                    tiles[i]=union;tiles[j]=set();break
            else:break
    for iteration in range(3):
        merged=0
        for i,cells in enumerate(tiles):
            if not cells:continue
            xs={p[0] for p in cells};ys={p[1] for p in cells}
            if len(xs)>1 and len(ys)>1:continue
            near={q for p in cells for q in helper.neighbors(p)}-cells
            candidates=[j for j,c in enumerate(tiles) if j!=i and c and c&near and len(c|cells)<=32]
            candidates.sort(key=lambda j:(len(tiles[j]|cells),min(y for x,y in tiles[j]),min(x for x,y in tiles[j])))
            for j in candidates:
                union=cells|tiles[j]
                vs=[(name,helper.find_path(union,d,budget=10000)) for name,d in DIRS]
                good=[(name,p) for name,p in vs if p]
                if len(good)==4:
                    tiles[j]=union;tiles[i]=set();merged+=1;break
        pass
    return [t for t in tiles if len(t)>=2]

def _straighter_find_path(cells, direction, budget=20000, preferred_head=None):
    if len(cells)<2:return None
    dx,dy=direction;cells=set(cells)
    candidates=[p for p in cells if (p[0]-dx,p[1]-dy) in cells and not any((p[0]+k*dx,p[1]+k*dy) in cells for k in range(1,max(helper.W,helper.H)))]
    candidates.sort(key=lambda p:(p[1],p[0]))
    if preferred_head is not None:candidates=[p for p in candidates if p==preferred_head]
    adjacency={p:[q for q in helper.neighbors(p) if q in cells] for p in cells}
    nodes=0
    def visit(path,left):
        nonlocal nodes
        nodes+=1
        if nodes>budget:return None
        if not left:return path[::-1]
        p=path[-1];options=[q for q in adjacency[p] if q in left]
        prev=path[-2] if len(path)>1 else p
        options.sort(key=lambda q:(sum(r in left for r in adjacency[q]),(q[0]-p[0],q[1]-p[1])!=(p[0]-prev[0],p[1]-prev[1]),q[1],q[0]))
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

# Normalize offline path searches for reuse across 50 levels with related small clusters.
_original_find_path=_straighter_find_path
_path_cache={}
def cached_find_path(cells,direction,budget=20000):
    ox=min(x for x,y in cells);oy=min(y for x,y in cells)
    normalized=tuple(sorted((x-ox,y-oy) for x,y in cells))
    key=(normalized,direction,budget)
    if key not in _path_cache:_path_cache[key]=_original_find_path(normalized,direction,budget)
    path=_path_cache[key]
    return [(x+ox,y+oy) for x,y in path] if path else None
helper.find_path=cached_find_path

SHAPES={15:('heart',37,37),20:('diamond',43,45),25:('lightning',45,55),30:('star',61,61),40:('butterfly',49,47),45:('rocket',47,63)}

def rectangle_mask(level_id):
    if level_id<=25:
        step=level_id-6;w=24+2*(step//7);h=26+2*(step%4)
    else:
        step=level_id-26;w=36+2*(step//12);h=38+2*(step%4)
    mask={(x,y) for y in range(2,h-2) for x in range(2,w-2)}
    if level_id%4==0:
        mask={p for p in mask if not ((p[0]<5 and p[1]<6) or (p[0]>=w-5 and p[1]>=h-6))}
    elif level_id%4==2:
        mask={p for p in mask if not ((p[0]<4 and p[1]<4) or(p[0]>=w-4 and p[1]<4) or (p[0]<4 and p[1]>=h-4) or(p[0]>=w-4 and p[1]>=h-4))}
    return w,h,mask

def author_level(level_id):
    difficulty='HARD' if level_id<=25 else 'EXPERT'
    if level_id in SHAPES:
        name,w,h=SHAPES[level_id];mask=helper.make_mask(name,w,h)
    else:
        name='maze';w,h,mask=rectangle_mask(level_id)
    tiles=cluster_mask(mask,w,h,level_id)
    choices=choices_for_tiles(tiles,w,h)
    target=(11+(level_id-6)//2) if difficulty=='HARD' else (24+(level_id-26)//2)
    openings=3 if difficulty=='HARD' else 2
    arrows,solution,metrics=choose_directions(tiles,choices,w,h,target,openings,level_id)
    level={'id':level_id,'name':name,'cols':w,'rows':h,'difficulty':difficulty,'movementRule':'HEAD_CORRIDOR',
           'arrows':arrows,'solution':solution,'mask':sorted(mask),'metrics':metrics}
    return validate(level)

def save_level(level):
    level_id=level['id'];(OUT/f'level-{level_id:02}.json').write_text(json.dumps(level,indent=2)+'\n')
    (OUT/f'level-{level_id:02}-solution.txt').write_text(' -> '.join(map(str,level['solution']))+'\n')
    helper.OUT=OUT;copy=dict(level);copy['name']=f'level-{level_id:02}';helper.preview(copy)
    print(level_id,level['name'],json.dumps(level['metrics']),flush=True)

def main():
    parser=argparse.ArgumentParser();parser.add_argument('--ids',default='7,15,20,25,26,30,40,45',help='Offline candidates; Level 6 is reserved for reference_maze.py and is excluded from production emission');args=parser.parse_args()
    for level_id in map(int,args.ids.split(',')):
        save_level(author_level(level_id))

if __name__=='__main__':main()
