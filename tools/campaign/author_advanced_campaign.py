"""Offline LEGEND / EXTREME authoring: new paths on the 250 familiar silhouettes.

Existing resources stay read-only. New boards keep the same grid dimensions,
but re-partition paths and choose stronger dependency graphs. Tier acceptance
uses independently replayed certificates and nonoverlapping depth bands.
"""
import argparse, hashlib, heapq, json, random, time
from collections import deque
from pathlib import Path
import author_dense_campaign as dense
ROOT=Path(__file__).resolve().parents[2]
SOURCE=ROOT/'app/src/main/resources/levels/grandmaster'
DEST=ROOT/'app/src/main/resources/levels/advanced'
OUT=ROOT/'artifacts/campaign/advanced'
BANDS={'LEGEND':(190,249),'EXTREME':(275,600)}


def source(sid):
    info=next(row.split('\t') for row in (SOURCE/'index.tsv').read_text().splitlines() if row.startswith(str(sid)+'\t'))
    mask=set()
    for row in (SOURCE/f'level-{sid}.txt').read_text().splitlines()[3:]:
        aid,direction,xy=row.split('|');xy=list(map(int,xy.split(',')));corners=list(zip(xy[::2],xy[1::2]))
        cells=[corners[0]]
        for end in corners[1:]:
            x,y=cells[-1];dx=(end[0]>x)-(end[0]<x);dy=(end[1]>y)-(end[1]<y)
            while (x,y)!=end:x+=dx;y+=dy;cells.append((x,y))
        mask.update(cells)
    return {'name':info[1],'category':info[2],'cols':int(info[3]),'rows':int(info[4]),'mask':mask}


def choose(tiles,choices,w,h,seed,target,min_openings=2):
    """Event-driven search visits each newly legal orientation once."""
    blockers=dense.dependencies(tiles,choices,w,h)
    entries=[(i,name,bs) for i,opts in enumerate(blockers) for name,bs in opts.items()]
    reverse=[[] for _ in tiles]
    for j,(_,_,bs) in enumerate(entries):
        for i in bs:reverse[i].append(j)
    roots=sorted({i for i,_,bs in entries if not bs})
    def closure(starts):
        covered=set(starts);queue=deque(starts);counts=[len(bs) for _,_,bs in entries]
        while queue:
            i=queue.popleft()
            for j in reverse[i]:
                counts[j]-=1;tile=entries[j][0]
                if counts[j]==0 and tile not in covered:covered.add(tile);queue.append(tile)
        return covered
    seeds=[];covered=set()
    while len(covered)<len(tiles):
        best=None
        for i in roots:
            if i in covered:continue
            after=closure(covered|{i})
            if best is None or len(after)>len(best[1]):best=(i,after)
            if len(after)==len(tiles):break
        assert best,'Unreachable partition'
        seeds.append(best[0]);covered=best[1]
    # Greedy set cover can keep an early root that becomes redundant later.
    # Prune those roots before assigning exits so narrow and ring silhouettes
    # retain the same strict opening limit as other boards.
    for candidate in seeds[:]:
        rest=[i for i in seeds if i!=candidate]
        if rest and len(closure(rest))==len(tiles):seeds=rest
    rng=random.Random(seed);extra=roots[:];rng.shuffle(extra)
    for i in extra:
        if len(seeds)>=min_openings:break
        if i not in seeds:seeds.append(i)
    allowed=set(seeds);counts=[len(bs) for _,_,bs in entries];dep=[0]*len(entries)
    depths={};tie=[rng.random() for _ in tiles];ready=[]
    def enqueue(j):
        i,name,bs=entries[j]
        if not bs and i not in allowed:return
        heapq.heappush(ready,(-int(not bs),abs(dep[j]-target),-len(bs),-tie[i],j))
    for j,count in enumerate(counts):
        if count==0:enqueue(j)
    selected={};order=[]
    while ready:
        *_,j=heapq.heappop(ready);i,name,bs=entries[j]
        if i in selected:continue
        selected[i]=name;depths[i]=dep[j]+1;order.append(i)
        for other in reverse[i]:
            counts[other]-=1;dep[other]=max(dep[other],depths[i])
            if counts[other]==0:enqueue(other)
    assert len(order)==len(tiles),'Dependency cycle'
    arrows=[{'id':i+1,'exit':selected[i],'cells':choices[i][selected[i]]} for i in range(len(tiles))]
    openings=[i+1 for i in order if not blockers[i][selected[i]]]
    return arrows,[i+1 for i in order],{'arrows':len(arrows),'openingMoves':openings,'dependencyRounds':max(depths.values())}


def audit(level):
    """Independent occupancy-ray oracle, separate from the orientation search."""
    arrows={a['id']:a for a in level['arrows']};owner={};turns=0
    for aid,a in arrows.items():
        cells=list(map(tuple,a['cells']));assert len(cells)>=2 and len(cells)==len(set(cells))
        for p in cells:
            assert p not in owner and 0<=p[0]<level['cols'] and 0<=p[1]<level['rows'];owner[p]=aid
        assert all(abs(p[0]-q[0])+abs(p[1]-q[1])==1 for p,q in zip(cells,cells[1:]))
        assert (cells[-1][0]-cells[-2][0],cells[-1][1]-cells[-2][1])==dense.VECTORS[a['exit']]
        turns+=sum((q[0]-p[0],q[1]-p[1])!=(r[0]-q[0],r[1]-q[1]) for p,q,r in zip(cells,cells[1:],cells[2:]))
    assert set(owner)==level['mask'],'Lost boundary cells'
    dependencies={}
    for aid,a in arrows.items():
        x,y=a['cells'][-1];dx,dy=dense.VECTORS[a['exit']];bs=set();x+=dx;y+=dy
        while 0<=x<level['cols'] and 0<=y<level['rows']:
            if (x,y) in owner:bs.add(owner[x,y])
            x+=dx;y+=dy
        assert aid not in bs;dependencies[aid]=bs
    remaining=set(arrows);depth={}
    for aid in level['solution']:
        assert aid in remaining and not dependencies[aid]&remaining,('Illegal removal',level['id'],aid)
        depth[aid]=1+max((depth[b] for b in dependencies[aid]),default=0);remaining.remove(aid)
    assert not remaining
    openings=sorted(aid for aid,bs in dependencies.items() if not bs)
    assert openings==sorted(level['metrics']['openingMoves'])
    assert max(depth.values())==level['metrics']['dependencyRounds']
    lo,hi=BANDS[level['difficulty']];assert lo<=max(depth.values())<=hi and 1<=len(openings)<=5
    assert any(len(a['cells'])>=10 for a in arrows.values()),'Need long winding paths'
    level['metrics'].update({'occupiedCells':len(owner),'turns':turns,'certificate':'PASS'})
    return level


def author(lid, attempt_start=0):
    tier='LEGEND' if lid<=750 else 'EXTREME';sid=251+(lid-501)%250;s=source(sid);lo,hi=BANDS[tier]
    for attempt in range(attempt_start,80):
        seed=lid+attempt*1000
        if tier=='LEGEND':palette=[4,8,8,12,8,8,12,4,8] if attempt<5 else [4,8,8,8,4,12,8,4,8]
        else:palette=[4,4,8,4,4,12,4,4,8] if attempt<5 else [4,4,4,4,4,12,4,4,4]
        if attempt>=16:
            # Some silhouettes (especially rings) need intermediate tile sizes
            # to connect dependency regions without adding easy opening moves.
            palettes=([4,8,8,4,8,12,4,8,4], [8,4,8,8,4,8,12,8,4],
                      [4,4,8,4,4,12,4,4,8], [8,8,4,8,12,4,8,8,4])
            palette=palettes[(attempt-16)%len(palettes)]
        tiles=dense.cluster_mask(s['mask'],s['cols'],s['rows'],seed,targets=palette)
        assert {p for t in tiles for p in t}==s['mask']
        choices=dense.choices_for_tiles(tiles,s['cols'],s['rows'])
        for target in ([210,190,170,150,230] if tier=='LEGEND' else [350,500,280]):
            arrows,solution,metrics=choose(tiles,choices,s['cols'],s['rows'],seed,target)
            if lo<=metrics['dependencyRounds']<=hi and len(metrics['openingMoves'])<=5:
                return audit(dict(s,id=lid,sourceLevel=sid,name=f'{tier.title()} {s["name"]}',category=f'{tier.title()} {s["category"]}',difficulty=tier,arrows=arrows,solution=solution,metrics=metrics))
        print(f'RETRY {lid} attempt={attempt+1} {metrics}',flush=True)
    raise ValueError(f'Cannot meet measured {tier} band for {lid}')


def emit(level):
    lid=level['id'];lines=['ADVANCED_V1',str(lid),','.join(map(str,level['solution']))]
    for a in level['arrows']:
        xy=','.join(str(n) for point in dense.helper.corners(a['cells']) for n in point)
        lines.append(f'{a["id"]}|{a["exit"]}|{xy}')
    text='\n'.join(lines)+'\n';(DEST/f'level-{lid}.txt').write_text(text,encoding='utf-8')
    record={key:level[key] for key in ('id','sourceLevel','name','category','cols','rows','difficulty','metrics')}
    record['sha256']=hashlib.sha256(text.encode()).hexdigest();(OUT/f'level-{lid}.json').write_text(json.dumps(record,indent=2)+'\n',encoding='utf-8')


def layout_signature(level):
    # Ignore IDs, labels, certificates and arrow ordering: those cannot make a
    # repeated playable layout unique.
    paths=sorted((a['exit'],tuple(map(tuple,a['cells']))) for a in level['arrows'])
    return hashlib.sha256(repr((level['cols'],level['rows'],paths)).encode()).hexdigest()


def read_saved(lid):
    record=json.loads((OUT/f'level-{lid}.json').read_text())
    data=(DEST/f'level-{lid}.txt').read_bytes()
    # Hash logical UTF-8 text; Windows authoring writes CRLF line endings.
    normalized=data.decode('utf-8').replace('\r\n','\n').encode('utf-8')
    assert hashlib.sha256(normalized).hexdigest()==record['sha256'],('Resource hash mismatch',lid)
    lines=data.decode('utf-8').splitlines()
    assert lines[0]=='ADVANCED_V1' and int(lines[1])==lid
    sid=251+(lid-501)%250;s=source(sid)
    tier='LEGEND' if lid<=750 else 'EXTREME'
    assert record['id']==lid and record['sourceLevel']==sid and record['difficulty']==tier
    assert record['cols']==s['cols'] and record['rows']==s['rows']
    assert record['name']==f'{tier.title()} {s["name"]}'
    assert record['category']==f'{tier.title()} {s["category"]}'
    arrows=[]
    for line in lines[3:]:
        aid,direction,xy=line.split('|');coords=list(map(int,xy.split(',')))
        assert len(coords)>=4 and len(coords)%2==0
        corners=list(zip(coords[::2],coords[1::2]));cells=[corners[0]]
        for end in corners[1:]:
            x,y=cells[-1];assert (x==end[0]) != (y==end[1])
            dx=(end[0]>x)-(end[0]<x);dy=(end[1]>y)-(end[1]<y)
            while (x,y)!=end:x+=dx;y+=dy;cells.append((x,y))
        arrows.append({'id':int(aid),'exit':direction,'cells':cells})
    assert len(arrows)==len({a['id'] for a in arrows})==record['metrics']['arrows']
    level=dict(record,mask=s['mask'],arrows=arrows,solution=list(map(int,lines[2].split(','))))
    original_metrics=dict(level['metrics'])
    audit(level)
    assert level['metrics']==original_metrics,('Recorded metrics differ from resource',lid)
    return record,layout_signature(level)


def finish():
    # Reparse serialized coordinate resources, replay every certificate and
    # independently remeasure difficulty before publishing the complete index.
    records=[];layouts=set()
    baseline=json.loads((ROOT/'artifacts/campaign/baseline-500/catalog.json').read_text())
    old_layouts={layout_signature(level) for level in baseline['levels']}
    for lid in range(501,1001):
        record,signature=read_saved(lid)
        assert signature not in layouts,('Repeated advanced playable layout',lid)
        assert signature not in old_layouts,('Repeated original playable layout',lid)
        layouts.add(signature);records.append(record)
    index=['\t'.join(str(r[key]) for key in ('id','name','category','cols','rows')) for r in records]
    (DEST/'index.tsv').write_text('\n'.join(index)+'\n',encoding='utf-8')
    report={'result':'PASS','levelCount':500,'uniquePlayableLayouts':len(layouts),
            'savedResourcesReplayed':True,'originalLayoutsRepeated':0,'tiers':{}}
    for tier in BANDS:
        rows=[r for r in records if r['difficulty']==tier]
        report['tiers'][tier]={'levels':len(rows),'arrows':[min(r['metrics']['arrows'] for r in rows),max(r['metrics']['arrows'] for r in rows)],'dependencyRounds':[min(r['metrics']['dependencyRounds'] for r in rows),max(r['metrics']['dependencyRounds'] for r in rows)],'openingMoves':[min(len(r['metrics']['openingMoves']) for r in rows),max(len(r['metrics']['openingMoves']) for r in rows)]}
    report['levels']=records;(OUT/'authoring-audit.json').write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8')
    print(json.dumps({k:v for k,v in report.items() if k!='levels'},indent=2))


def main():
    p=argparse.ArgumentParser();p.add_argument('--start',type=int,default=501);p.add_argument('--end',type=int,default=1000);p.add_argument('--finish',action='store_true');p.add_argument('--attempt-start',type=int,default=0);args=p.parse_args()
    DEST.mkdir(parents=True,exist_ok=True);OUT.mkdir(parents=True,exist_ok=True)
    if args.finish:finish();return
    for lid in range(args.start,args.end+1):
        assert 501<=lid<=1000
        if (OUT/f'level-{lid}.json').exists() and (DEST/f'level-{lid}.txt').exists():continue
        started=time.time();level=author(lid,args.attempt_start if lid==args.start else 0);emit(level);print(f'SAVED {lid}: {level["metrics"]} seconds={time.time()-started:.2f}',flush=True)
if __name__=='__main__':main()
