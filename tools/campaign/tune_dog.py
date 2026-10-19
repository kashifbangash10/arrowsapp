"""Offline deterministic dog retuning. Keeps every original occupied cell."""
import re,json,hashlib,sys
from pathlib import Path
from PIL import Image,ImageDraw
ROOT=Path(__file__).resolve().parents[2]
SOURCE=ROOT/'app/src/main/java/com/amowpuzzle/aitricker/data/level/DogLevel.kt'
OUT=ROOT/'artifacts/campaign';OUT.mkdir(parents=True,exist_ok=True)
W=H=69
DIRS={'LEFT':(-1,0),'DOWN':(0,1),'RIGHT':(1,0),'UP':(0,-1)}
# Reuse the established bounded deterministic Hamiltonian cluster search.
context={'__file__':str(ROOT/'tools/guitar/author_cluster.py')}
author_functions=(ROOT/'tools/guitar/author_cluster.py').read_text().split('remaining=set(mask)')[0]
# A rigid swept body ignores its own translated cells; its exit ray may cross itself.
author_functions=author_functions.replace(' and not any((p[0]+k*dx,p[1]+k*dy) in cells for k in range(1,max(W,H)))','')
exec(author_functions,context)
find_path=context['find_path']
def parse(text):
    arrows=[]
    for m in re.finditer(r'arrow\((\d+), Direction\.(\w+), ([\d, ]+)\)',text):
        ns=list(map(int,m[3].split(',')));cs=list(zip(ns[::2],ns[1::2]));ps=[cs[0]]
        for p,q in zip(cs,cs[1:]):
            dx=(q[0]>p[0])-(q[0]<p[0]);dy=(q[1]>p[1])-(q[1]<p[1])
            assert bool(dx)!=bool(dy)
            while ps[-1]!=q:
                x,y=ps[-1];ps.append((x+dx,y+dy))
        arrows.append({'id':int(m[1]),'exit':m[2],'cells':ps})
    return arrows
def render(arrows,target):
    s,pad=14,20;im=Image.new('RGB',(W*s+pad*2,H*s+pad*2),'#101318');d=ImageDraw.Draw(im)
    for a in arrows:
        ps=[(pad+(x+.5)*s,pad+(y+.5)*s) for x,y in a['cells']]
        d.line(ps,fill='white',width=3,joint='curve')
        x,y=ps[-1];dx,dy=DIRS[a['exit']];tip=(x+dx*s*.32,y+dy*s*.32);bx,by=tip[0]-dx*s*.52,tip[1]-dy*s*.52
        d.polygon([tip,(bx-dy*s*.28,by+dx*s*.28),(bx+dy*s*.28,by-dx*s*.28)],fill='white')
    im.save(target)
text=SOURCE.read_text(encoding='utf-8-sig');original=parse(text)
baseline=OUT/'dog-original.json'
if baseline.exists():
    original=json.loads(baseline.read_text())
    for a in original:a['cells']=list(map(tuple,a['cells']))
else:baseline.write_text(json.dumps(original,indent=2)+'\n')
PATH_CACHE={}
original_tiles={a['id']:set(a['cells']) for a in original}
for a in original:
    PATH_CACHE[frozenset(a['cells'])]={a['exit']:a['cells']}
def options(cells):
    key=frozenset(cells)
    result=PATH_CACHE.setdefault(key,{})
    for name,vec in DIRS.items():
        if name not in result:result[name]=find_path(cells,vec,budget=10000)
    return {name:p for name,p in result.items() if p}
def assign(tiles):
    owner={p:i for i,t in tiles.items() for p in t}
    choices={i:options(t) for i,t in tiles.items()}
    if any(not v for v in choices.values()):return None
    blockers={i:{name:{owner[p] for p in context['rays'](tiles[i],DIRS[name]) if p in owner and owner[p]!=i} for name in v} for i,v in choices.items()}
    def closure(seed):
        done=set(seed)
        while True:
            more={i for i,b in blockers.items() if i not in done and any(bs and bs<=done for bs in b.values())}
            if not more:return done
            done|=more
    roots=sorted(i for i,b in blockers.items() if any(not bs for bs in b.values()))
    seeds=set();covered=set()
    while len(covered)<len(tiles):
        opts=[(len(closure(covered|{i})),i) for i in roots if i not in covered]
        if not opts:return None
        _,i=max(opts);seeds.add(i);covered=closure(covered|{i})
    for i in sorted(seeds):
        if len(closure(seeds-{i}))==len(tiles):seeds.remove(i)
    left=set(tiles);depth={};selected={};order=[]
    while left:
        opts=[]
        for i in sorted(left):
            for name,bs in blockers[i].items():
                if bs&left or (not bs and i not in seeds):continue
                dep=max((depth[j] for j in bs),default=0)
                opts.append(((bool(bs),dep,len(bs),len(tiles[i])),i,name,bs))
        if not opts:return None
        _,i,name,bs=max(opts,key=lambda x:(x[0],-x[1]))
        depth[i]=max((depth[j] for j in bs),default=0)+1
        selected[i]={'id':i,'exit':name,'cells':choices[i][name]};left.remove(i);order.append(i)
    return [selected[i] for i in sorted(tiles)],order,seeds,max(depth.values())
tiles=dict(original_tiles)
for i,j in [(85,90),(28,101),(29,113),(23,85),(98,119),(72,105),(70,126),(46,50),(86,109),(6,114)]:
    tiles[i]|=tiles.pop(j)
best=assign(tiles)
print('BASE',len(tiles),len(best[2]),best[3],flush=True)
for iteration in range(50):
    if len(best[2])<=6 and best[3]>=30:break
    current_owner={p:i for i,t in tiles.items() for p in t}
    pairs=set()
    for i in best[2]:
        for p in tiles[i]:
            for dx,dy in DIRS.values():
                j=current_owner.get((p[0]+dx,p[1]+dy))
                if j is not None and i!=j and len(tiles[i]|tiles[j])<=52:pairs.add(tuple(sorted((i,j))))
    candidates=[]
    for i,j in sorted(pairs):
        union=tiles[i]|tiles[j];opts=options(union)
        if not opts:continue
        candidates.append((len(opts),i,j,union))
    candidate_best=None
    bestscore=(len(best[2]),-best[3])
    for _,i,j,union in sorted(candidates,reverse=True):
        candidate=dict(tiles);candidate[i]=union;del candidate[j]
        answer=assign(candidate)
        if answer:
            score=(len(answer[2]),-answer[3])
            if score<bestscore:
                bestscore=score;candidate_best=(candidate,answer,i,j)
    if not candidate_best:
        print('TRY_TRANSFERS',len(pairs),flush=True)
        transfer_candidates=[]
        seen=set()
        for i,j in sorted(pairs):
            for source,target in [(i,j),(j,i)]:
                boundary={p for p in tiles[source] if any((p[0]+dx,p[1]+dy) in tiles[target] for dx,dy in DIRS.values())}
                pieces=[{p} for p in sorted(boundary)]
                pieces += [{p,(p[0]+dx,p[1]+dy)} for p in sorted(boundary) for dx,dy in DIRS.values() if (p[0]+dx,p[1]+dy) in tiles[source]]
                for piece in pieces:
                    if len(tiles[source]-piece)<2:continue
                    key=(source,target,frozenset(piece))
                    if key in seen:continue
                    seen.add(key)
                    remainder=tiles[source]-piece;enlarged=tiles[target]|piece
                    if not options(remainder) or not options(enlarged):continue
                    priority=len(options(remainder))+len(options(enlarged))
                    transfer_candidates.append((priority,source,target,piece,remainder,enlarged))
        print('VALID_TRANSFERS',len(transfer_candidates),flush=True)
        for _,i,j,piece,remainder,enlarged in sorted(transfer_candidates,key=lambda c:(-c[0],c[1],c[2],sorted(c[3]))):
            candidate=dict(tiles);candidate[i]=remainder;candidate[j]=enlarged
            answer=assign(candidate)
            if answer:
                score=(len(answer[2]),-answer[3])
                if score<bestscore:
                    bestscore=score;candidate_best=(candidate,answer,i,j)
                    print('TRANSFER_CANDIDATE',i,j,sorted(piece),'OPEN',len(answer[2]),'DEPTH',answer[3],flush=True)
                    if len(answer[2])<len(best[2]):break
        if not candidate_best:
            print('NO_IMPROVEMENT',flush=True);break
    tiles,best,i,j=candidate_best
    print('MERGE',iteration,i,j,'ARROWS',len(tiles),'OPEN',len(best[2]),'DEPTH',best[3],flush=True)
    if len(best[2])<=6 and best[3]>=30:break
arrows,order,seeds,depth=best
owner={p:i for i,t in tiles.items() for p in t}
# Independent verification: explicit full-body translations through each grid offset.
dep={}
for a in arrows:
    ps=a['cells'];assert len(ps)==len(set(ps))
    assert all(abs(q[0]-p[0])+abs(q[1]-p[1])==1 for p,q in zip(ps,ps[1:]))
    assert tuple(ps[-1][j]-ps[-2][j] for j in range(2))==DIRS[a['exit']]
    assert all(0<=x<W and 0<=y<H for x,y in ps)
    dx,dy=DIRS[a['exit']]
    dep[a['id']]={owner[p] for k in range(1,max(W,H)+1) for x,y in ps for p in [(x+dx*k,y+dy*k)] if p in owner and owner[p]!=a['id']}
assert sum(len(a['cells']) for a in arrows)==len(owner)
assert {p for a in original for p in a['cells']}=={p for a in arrows for p in a['cells']}
left=set(dep);assert len(order)==len(left) and set(order)==left
for i in order:
    assert not dep[i]&left,('invalid removal',i)
    left.remove(i)
left=set(dep);rounds=[]
while left:
    free=sorted(i for i in left if not dep[i]&left);assert free,('cycle',left)
    rounds.append(free);left-=set(free)
metrics={'arrows':len(arrows),'openingMoves':rounds[0],'dependencyRounds':len(rounds),'occupiedCells':len(owner),
         'dependencyEdges':sum(map(len,dep.values())),'independentTranslatedCellSolutionValid':True,
         'maxPathCells':max(len(a['cells']) for a in arrows),'minPathCells':min(len(a['cells']) for a in arrows),
         'turns':sum(sum((q[0]-p[0],q[1]-p[1])!=(r[0]-q[0],r[1]-q[1]) for p,q,r in zip(a['cells'],a['cells'][1:],a['cells'][2:])) for a in arrows),
         'unchangedOccupiedCells':True,'unchangedPerArrowClusters':tiles==original_tiles,
         'sourceSha256Before':hashlib.sha256(SOURCE.read_bytes()).hexdigest()}
print(json.dumps(metrics,indent=2),flush=True)
data={'cols':W,'rows':H,'arrows':arrows,'solution':order,'metrics':metrics,'rounds':rounds,'dependencies':{i:sorted(b) for i,b in dep.items()}}
(OUT/'dog-tuned.json').write_text(json.dumps(data,indent=2)+'\n')
render(original,OUT/'dog-before.png');render(arrows,OUT/'dog-preview.png')
if '--write' in sys.argv:
    assert len(metrics['openingMoves'])<=6 and metrics['dependencyRounds']>=30
    def corners(ps):
        return [ps[0]]+[q for p,q,r in zip(ps,ps[1:],ps[2:]) if (q[0]-p[0],q[1]-p[1])!=(r[0]-q[0],r[1]-q[1])]+[ps[-1]]
    lines=['                arrow(%d, Direction.%s, %s),'%(a['id'],a['exit'],', '.join(str(n) for p in corners(a['cells']) for n in p)) for a in arrows]
    text=re.sub(r'                arrow\(1, Direction\.[\s\S]*?(?=\n            \))','\n'.join(lines),text,count=1)
    text=text.replace('id = 2,','id = 49,',1)
    text=re.sub(r'/\*\*[\s\S]*?\*/','/**\n * Fixed expert dog silhouette for campaign Level 49.\n * The silhouette retains all original occupied cells; authored exits provide\n * %d opening moves and %d acyclic dependency rounds.\n */'%(len(metrics['openingMoves']),metrics['dependencyRounds']),text,count=1)
    lines=['        '+', '.join(map(str,order[i:i+16])) for i in range(0,len(order),16)]
    text=re.sub(r'    val solutionIds: List<Int> = listOf\([\s\S]*?\n    \)','    val solutionIds: List<Int> = listOf(\n'+',\n'.join(lines)+'\n    )',text,count=1)
    assert parse(text)==arrows
    SOURCE.write_text(text,encoding='utf-8')
    assert parse(SOURCE.read_text())==arrows
    (OUT/'dog-solution.txt').write_text('Level 49 dog: complete valid removal sequence\n'+', '.join(map(str,order))+'\n')
    metrics['sourceSha256After']=hashlib.sha256(SOURCE.read_bytes()).hexdigest()
    (OUT/'dog-validation.json').write_text(json.dumps(metrics,indent=2)+'\n')
    print('WROTE',SOURCE,flush=True)
