"""Offline GRANDMASTER authoring; explicit runtime coordinates and independently replayed solutions."""
import argparse, importlib.util, json, random, time
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2];OUT=ROOT/'artifacts/campaign/grandmaster';OUT.mkdir(parents=True,exist_ok=True)
spec=importlib.util.spec_from_file_location('dense',ROOT/'tools/campaign/author_dense_campaign.py')
dense=importlib.util.module_from_spec(spec);spec.loader.exec_module(dense)
import grandmaster_shapes as shapes

def matches_source(level):
    """Reject checkpoints produced before an editable silhouette or name changed."""
    lid=level['id']
    if level['name']!=shapes.name_for(lid) or level['category']!=shapes.category_for(lid):return False
    occupied={tuple(cell) for cell in level['mask']}
    for target in (5000,5600,6200):
        w,h,mask=shapes.mask_for(lid,target)
        if level['cols']==w and level['rows']==h and occupied==mask:return True
    return False

def meets_grandmaster(level):
    n=len(level['arrows']);m=level['metrics'];opening=len(m['openingMoves'])
    return n>=300 and opening<=max(3,n//25) and m['dependencyRounds']>= (125 if opening>5 else 115) and m['coverage']>=.98

def select_directions(tiles,choices,w,h,target,seed):
    blockers=dense.dependencies(tiles,choices,w,h)
    def closure(start):
        done=set(start)
        while True:
            added={i for i,b in enumerate(blockers) if i not in done and any(bs and bs<=done for bs in b.values())}
            if not added:return done
            done|=added
    candidates=[i for i,b in enumerate(blockers) if any(not bs for bs in b.values())]
    roots=[];covered=set()
    while len(covered)<len(tiles):
        options=[(len(closure(covered|{i})),i) for i in candidates if i not in covered]
        if not options:raise ValueError(('Unreachable tile',len(covered),len(tiles)))
        _,i=max(options);roots.append(i);covered=closure(covered|{i})
    best=None
    # Accept a valid high-depth graph immediately. No need to exhaustively search for an exact number.
    for attempt in range(3):
        rng=random.Random(seed*101+attempt);seeds=set(roots);shuffled=list(candidates);rng.shuffle(shuffled)
        for i in shuffled:
            if len(seeds)>=2:break
            seeds.add(i)
        for bias in [target-1,140,180,240,320,110,90,64,32]:
            left=set(range(len(tiles)));depth={};order=[];selected={};actual_roots=[];tie={i:rng.random() for i in range(len(tiles))}
            while left:
                best_option=None
                for i in sorted(left):
                    for name,bs in blockers[i].items():
                        if bs&left or (not bs and i not in seeds):continue
                        dep=max((depth[j] for j in bs),default=0)
                        option=((not bool(bs),-abs(dep-bias),len(bs),tie[i]),i,name,bs)
                        if best_option is None or option[0]>best_option[0]:best_option=option
                if best_option is None:raise ValueError('Dependency cycle')
                _,i,name,bs=best_option
                if not bs:actual_roots.append(i)
                depth[i]=max((depth[j] for j in bs),default=0)+1
                selected[i]=(name,choices[i][name]);left.remove(i);order.append(i)
            rounds=max(depth.values());opening=len(actual_roots)
            score=max(0,(125 if opening>5 else 115)-rounds)*100+max(0,opening-max(3,len(tiles)//25))*1000+abs(rounds-target)
            result=(score,depth,order,selected,actual_roots)
            if best is None or score<best[0]:best=result
            if rounds>=(125 if opening>5 else 115) and opening<=max(3,len(tiles)//25):break
        if max(best[1].values()) >= (125 if len(best[4])>5 else 115) and len(best[4])<=max(3,len(tiles)//25):break
    _,depth,order,selected,roots=best
    arrows=[{'id':i+1,'exit':selected[i][0].split('|')[0],'cells':selected[i][1]} for i in range(len(tiles))]
    return arrows,[i+1 for i in order],{'arrows':len(arrows),'openingMoves':[i+1 for i in roots],'dependencyRounds':max(depth.values())}

def author(lid):
    for attempt in range(9):
        start=time.time();w,h,mask=shapes.mask_for(lid,target=5000+(attempt//3)*600);seed=lid+attempt*1000
        print(f'PACK {lid} {shapes.name_for(lid)} attempt={attempt+1} cells={len(mask)} grid={w}x{h}',flush=True)
        groups=shapes.components(mask)
        if len(groups)!=1:raise ValueError(('Disconnected single-object silhouette',lid,[len(g) for g in groups]))
        tiles=dense.cluster_mask(mask,w,h,seed);print(f'TILES {lid} count={len(tiles)} seconds={time.time()-start:.1f}',flush=True)
        choices=dense.choices_for_tiles(tiles,w,h)
        arrows,solution,metrics=select_directions(tiles,choices,w,h,135+((lid-251)//50)*5,seed)
        level={'id':lid,'name':shapes.name_for(lid),'category':shapes.category_for(lid),'cols':w,'rows':h,'difficulty':'GRANDMASTER','movementRule':'HEAD_CORRIDOR','arrows':arrows,'solution':solution,'mask':sorted(mask),'metrics':metrics}
        dense.validate(level)
        if meets_grandmaster(level):return level
        (OUT/f'candidate-{lid}.json').write_text(json.dumps(level,indent=2)+'\n')
        print(f'REFINE {lid}: {json.dumps(metrics)}',flush=True)
    raise ValueError(f'Could not certify GRANDMASTER criteria for {lid}')

def save(level):
    lid=level['id'];(OUT/f'level-{lid}.json').write_text(json.dumps(level,indent=2)+'\n',encoding='utf-8')
    (OUT/f'level-{lid}-solution.txt').write_text(', '.join(map(str,level['solution']))+'\n')
    dense.helper.OUT=OUT;preview=dict(level);preview['name']=f'level-{lid}';dense.helper.preview(preview)
    m=level['metrics'];print(f'SAVED {lid} {level["name"]}: {len(level["arrows"])} arrows, {len(m["openingMoves"])} openings, {m["dependencyRounds"]} rounds, {m["coverage"]:.1%} coverage',flush=True)

def emit():
    levels=[json.loads((OUT/f'level-{i}.json').read_text(encoding='utf-8-sig')) for i in range(251,501)]
    for l in levels:
        dense.validate(l)
        assert meets_grandmaster(l),l['id']
        assert matches_source(l),('Stale editable silhouette',l['id'])
        assert len(shapes.components({tuple(c) for c in l['mask']}))==1,l['id']
    dest=ROOT/'app/src/main/resources/levels/grandmaster';dest.mkdir(parents=True,exist_ok=True)
    index=[]
    for l in levels:
        index.append('\t'.join(map(str,[l['id'],l['name'],l['category'],l['cols'],l['rows']])))
        lines=['GRANDMASTER_V1',str(l['id']),','.join(map(str,l['solution']))]
        for a in l['arrows']:
            xy=','.join(str(n) for point in dense.helper.corners(a['cells']) for n in point)
            lines.append(f"{a['id']}|{a['exit']}|{xy}")
        (dest/f"level-{l['id']}.txt").write_text('\n'.join(lines)+'\n',encoding='utf-8')
    (dest/'index.tsv').write_text('\n'.join(index)+'\n',encoding='utf-8')
    (OUT/'level-names.json').write_text(json.dumps({str(l['id']):l['name'] for l in levels},indent=2)+'\n',encoding='utf-8')
    (OUT/'categories.json').write_text(json.dumps({str(l['id']):l['category'] for l in levels},indent=2)+'\n',encoding='utf-8')
    print('EMITTED 250 fixed Grandmaster coordinate resources',flush=True)

def main():
    parser=argparse.ArgumentParser();parser.add_argument('--start',type=int,default=251);parser.add_argument('--end',type=int,default=500);parser.add_argument('--ids');parser.add_argument('--emit',action='store_true');parser.add_argument('--keep-going',action='store_true');args=parser.parse_args()
    if args.emit:emit();return
    ids=map(int,args.ids.split(',')) if args.ids else range(args.start,args.end+1)
    failures=[]
    for lid in ids:
        assert 251<=lid<=500
        path=OUT/f'level-{lid}.json'
        if path.exists():
            l=dense.validate(json.loads(path.read_text()))
            if meets_grandmaster(l) and matches_source(l):print(f'Already certified {lid}',flush=True);continue
        try: save(author(lid))
        except Exception as error:
            if not args.keep_going: raise
            failures.append(lid)
            print(f'NEEDS_REVIEW {lid}: {error}',flush=True)
    if failures: raise SystemExit('Uncertified IDs: '+','.join(map(str,failures)))
if __name__=='__main__':main()
