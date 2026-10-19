"""Offline APEX authoring; fixed coordinate boards above the Extreme campaign.

Existing 1..1000 resources are read-only. Accepted resources are never regenerated
by a resumed run. Every saved file is independently replayed before publishing.
"""
import argparse
import hashlib
import json
import time
from pathlib import Path
import author_advanced_campaign as advanced

ROOT=advanced.ROOT
DEST=ROOT/'app/src/main/resources/levels/apex'
OUT=ROOT/'artifacts/campaign/apex'
MIN_DEPTH=425
advanced.BANDS['APEX']=(MIN_DEPTH,2000)


def source(lid):
    sid=251+(lid-1001)%250
    board=advanced.source(sid)
    prefix='Apex' if lid<=1250 else 'Apex Elite'
    return dict(board,id=lid,sourceLevel=sid,name=f'{prefix} {board["name"]}',
                category=f'{prefix} {board["category"]}',difficulty='APEX')


def author(lid, attempt_start=0, attempts=128):
    board=source(lid)
    palettes=([4]*15+[12], [4]*31+[12], [4]*7+[8,12], [4]*7+[12])
    best=None
    for attempt in range(attempt_start,attempt_start+attempts):
        seed=lid+attempt*2003
        tiles=advanced.dense.cluster_mask(board['mask'],board['cols'],board['rows'],seed,
                                         targets=palettes[attempt%len(palettes)])
        choices=advanced.dense.choices_for_tiles(tiles,board['cols'],board['rows'])
        for tie in range(4):
            arrows,solution,metrics=advanced.choose(tiles,choices,board['cols'],board['rows'],
                                                     seed+tie*7907,10000,min_openings=1)
            if MIN_DEPTH<=metrics['dependencyRounds']<=2000 and len(metrics['openingMoves'])<=5:
                return advanced.audit(dict(board,arrows=arrows,solution=solution,metrics=metrics))
            score=(max(0,len(metrics['openingMoves'])-5),max(0,MIN_DEPTH-metrics['dependencyRounds']))
            if best is None or score<best[0]:best=(score,metrics)
            # Root coverage is independent of the randomized tie order.
            if len(metrics['openingMoves'])>5:break
        print(f'RETRY {lid} attempt={attempt+1} best={best[1]}',flush=True)
    raise ValueError(f'Cannot meet measured APEX band for {lid}: {best[1]}')


def emit(level):
    lid=level['id'];lines=['APEX_V1',str(lid),','.join(map(str,level['solution']))]
    for arrow in level['arrows']:
        xy=','.join(str(n) for point in advanced.dense.helper.corners(arrow['cells']) for n in point)
        lines.append(f'{arrow["id"]}|{arrow["exit"]}|{xy}')
    data=('\n'.join(lines)+'\n').encode('utf-8')
    (DEST/f'level-{lid}.txt').write_bytes(data)
    record={key:level[key] for key in ('id','sourceLevel','name','category','cols','rows','difficulty','metrics')}
    record['sha256']=hashlib.sha256(data).hexdigest()
    (OUT/f'level-{lid}.json').write_text(json.dumps(record,indent=2)+'\n',encoding='utf-8')


def read_saved(lid):
    board=source(lid)
    record=json.loads((OUT/f'level-{lid}.json').read_text())
    data=(DEST/f'level-{lid}.txt').read_bytes()
    assert hashlib.sha256(data).hexdigest()==record['sha256'],('Resource hash mismatch',lid)
    for key in ('id','sourceLevel','name','category','cols','rows','difficulty'):
        assert record[key]==board[key],('Incorrect metadata',lid,key)
    lines=data.decode('utf-8').splitlines()
    assert lines[0]=='APEX_V1' and int(lines[1])==lid
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
    level=dict(board,arrows=arrows,solution=list(map(int,lines[2].split(','))),metrics=dict(record['metrics']))
    advanced.audit(level)
    assert level['metrics']==record['metrics'],('Recorded metrics differ from resource',lid)
    return record,advanced.layout_signature(level)


def finish():
    baseline=json.loads((ROOT/'artifacts/campaign/baseline-1000/catalog.json').read_text())
    old_layouts={advanced.layout_signature(level) for level in baseline['levels']}
    assert len(old_layouts)==1000
    records=[];layouts=set()
    for lid in range(1001,1501):
        record,signature=read_saved(lid)
        assert signature not in layouts and signature not in old_layouts,('Repeated playable layout',lid)
        layouts.add(signature);records.append(record)
    (DEST/'index.tsv').write_text('\n'.join('\t'.join(str(row[key]) for key in
        ('id','name','category','cols','rows')) for row in records)+'\n',encoding='utf-8')
    report={'result':'PASS','levelCount':len(records),'uniquePlayableLayouts':len(layouts),
            'savedResourcesReplayed':True,'originalLayoutsRepeated':0,
            'dependencyRounds':[min(r['metrics']['dependencyRounds'] for r in records),max(r['metrics']['dependencyRounds'] for r in records)],
            'arrows':[min(r['metrics']['arrows'] for r in records),max(r['metrics']['arrows'] for r in records)],
            'openingMoves':[min(len(r['metrics']['openingMoves']) for r in records),max(len(r['metrics']['openingMoves']) for r in records)]}
    (OUT/'authoring-audit.json').write_text(json.dumps(dict(report,levels=records),indent=2)+'\n',encoding='utf-8')
    print(json.dumps(report,indent=2))


def main():
    parser=argparse.ArgumentParser()
    parser.add_argument('--start',type=int,default=1001);parser.add_argument('--end',type=int,default=1500)
    parser.add_argument('--attempt-start',type=int,default=0);parser.add_argument('--attempts',type=int,default=128)
    parser.add_argument('--finish',action='store_true');args=parser.parse_args()
    DEST.mkdir(parents=True,exist_ok=True);OUT.mkdir(parents=True,exist_ok=True)
    if args.finish:finish();return
    failures=[]
    for lid in range(args.start,args.end+1):
        assert 1001<=lid<=1500
        if (OUT/f'level-{lid}.json').exists() and (DEST/f'level-{lid}.txt').exists():continue
        started=time.time()
        try:level=author(lid,args.attempt_start,args.attempts)
        except ValueError as error:
            failures.append(lid);print(f'FAILED {lid}: {error}',flush=True);continue
        emit(level);print(f'SAVED {lid}: {level["metrics"]} seconds={time.time()-started:.2f}',flush=True)
    if failures:raise SystemExit(f'Incomplete APEX levels: {failures}')


if __name__=='__main__':main()
