"""Offline MASTER authoring; explicit runtime coordinates and independently replayed solutions."""
import argparse, importlib.util, json, random, time
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2];OUT=ROOT/'artifacts/campaign/master';OUT.mkdir(parents=True,exist_ok=True)
spec=importlib.util.spec_from_file_location('dense',ROOT/'tools/campaign/author_dense_campaign.py')
dense=importlib.util.module_from_spec(spec);spec.loader.exec_module(dense)
import master_shapes as shapes

def matches_source(level):
    """Reject checkpoints produced before an editable silhouette or name changed."""
    lid=level['id']
    if level['name']!=shapes.name_for(lid) or level['category']!=shapes.category_for(lid):return False
    occupied={tuple(cell) for cell in level['mask']}
    for target in (3400,3800):
        w,h,mask=shapes.mask_for(lid,target)
        if level['cols']==w and level['rows']==h and occupied==mask:return True
    return False

def meets_master(level):
    n=len(level['arrows']);m=level['metrics'];opening=len(m['openingMoves'])
    return n>=210 and opening<=max(3,n//20) and m['dependencyRounds']>= (60 if opening>3 else 55) and m['coverage']>=.98

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
        for bias in [target-1,56,64,72,80,96,120,48,32,16]:
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
            score=max(0,(60 if opening>3 else 55)-rounds)*100+max(0,opening-max(3,len(tiles)//20))*1000+abs(rounds-target)
            result=(score,depth,order,selected,actual_roots)
            if best is None or score<best[0]:best=result
            if rounds>=(60 if opening>3 else 55) and opening<=max(3,len(tiles)//20):break
        if best[0]<100:break
    _,depth,order,selected,roots=best
    arrows=[{'id':i+1,'exit':selected[i][0].split('|')[0],'cells':selected[i][1]} for i in range(len(tiles))]
    return arrows,[i+1 for i in order],{'arrows':len(arrows),'openingMoves':[i+1 for i in roots],'dependencyRounds':max(depth.values())}

def author(lid):
    for attempt in range(6):
        start=time.time();w,h,mask=shapes.mask_for(lid,target=3400+(attempt//3)*400);seed=lid+attempt*1000
        print(f'PACK {lid} {shapes.name_for(lid)} attempt={attempt+1} cells={len(mask)} grid={w}x{h}',flush=True)
        tiles=dense.cluster_mask(mask,w,h,seed);print(f'TILES {lid} count={len(tiles)} seconds={time.time()-start:.1f}',flush=True)
        choices=dense.choices_for_tiles(tiles,w,h)
        arrows,solution,metrics=select_directions(tiles,choices,w,h,65+((lid-101)//30)*3,seed)
        level={'id':lid,'name':shapes.name_for(lid),'category':shapes.category_for(lid),'cols':w,'rows':h,'difficulty':'MASTER','movementRule':'HEAD_CORRIDOR','arrows':arrows,'solution':solution,'mask':sorted(mask),'metrics':metrics}
        dense.validate(level)
        if meets_master(level):return level
        (OUT/f'candidate-{lid}.json').write_text(json.dumps(level,indent=2)+'\n')
        print(f'REFINE {lid}: {json.dumps(metrics)}',flush=True)
    raise ValueError(f'Could not certify MASTER criteria for {lid}')

def save(level):
    lid=level['id'];(OUT/f'level-{lid}.json').write_text(json.dumps(level,indent=2)+'\n',encoding='utf-8')
    (OUT/f'level-{lid}-solution.txt').write_text(', '.join(map(str,level['solution']))+'\n')
    dense.helper.OUT=OUT;preview=dict(level);preview['name']=f'level-{lid}';dense.helper.preview(preview)
    m=level['metrics'];print(f'SAVED {lid} {level["name"]}: {len(level["arrows"])} arrows, {len(m["openingMoves"])} openings, {m["dependencyRounds"]} rounds, {m["coverage"]:.1%} coverage',flush=True)

def emit():
    levels=[json.loads((OUT/f'level-{i}.json').read_text(encoding='utf-8-sig')) for i in range(101,251)]
    for l in levels:
        dense.validate(l)
        assert meets_master(l),l['id']
        assert matches_source(l),('Stale editable silhouette',l['id'])
    categories=[levels[i:i+15] for i in range(0,len(levels),15)]
    lines=['package com.amowpuzzle.aitricker.data.level','','import com.amowpuzzle.aitricker.domain.model.*','','/** 150 fixed MASTER boards in ten themed categories. No runtime authoring or random generation. */','object MasterCampaignLevels {','    val ids: Set<Int> = (101..250).toSet()','']
    # Separate category classes and short path methods keep bytecode safely below JVM method limits.
    for fun,typ,otherwise in [('level','Level?','null'),('solutionIds','List<Int>','emptyList()')]:
        lines += [f'    fun {fun}(id: Int): {typ} = when (id) {{']
        lines += [f'        in {group[0]["id"]}..{group[-1]["id"]} -> MasterCategory{index+1:02}.{fun}(id)' for index,group in enumerate(categories)]
        lines += [f'        else -> {otherwise}','    }','']
    for fun,key in [('name','name'),('category','category')]:
        lines += [f'    fun {fun}(id: Int): String? = when (id) {{']
        lines += [f'        {l["id"]} -> {json.dumps(l[key])}' for l in levels]
        lines += ['        else -> null','    }','']
    lines += ['}','']
    src=ROOT/'app/src/main/java/com/amowpuzzle/aitricker/data/level'
    fixed=(src/'FixedCampaignLevels.kt').read_text(encoding='utf-8-sig')
    helper=fixed[fixed.index('    private fun path('):fixed.rfind('\n}')]
    for index,category in enumerate(categories):
        lines += [f'private object MasterCategory{index+1:02} {{']
        for fun,typ,expr,otherwise in [('level','Level?',lambda l:f'level{l["id"]}()','null'),('solutionIds','List<Int>',lambda l:f'solution{l["id"]}()','emptyList()')]:
            lines += [f'    fun {fun}(id: Int): {typ} = when (id) {{']
            lines += [f'        {l["id"]} -> {expr(l)}' for l in category]
            lines += [f'        else -> {otherwise}','    }','']
        for l in category:
            lid=l['id'];m=l['metrics'];groups=[l['arrows'][i:i+60] for i in range(0,len(l['arrows']),60)]
            lines += [f'    // {l["name"]} / {l["category"]}: {len(l["arrows"])} arrows, {len(m["openingMoves"])} openings, {m["dependencyRounds"]} rounds.',f'    private fun solution{lid}(): List<Int> = listOf('+', '.join(map(str,l['solution']))+')',f'    private fun level{lid}() = Level(',f'        id = {lid}, cols = {l["cols"]}, rows = {l["rows"]}, difficulty = Difficulty.MASTER,','        presentation = LevelPresentation.DARK_WHITE,','        movementRule = MovementRule.HEAD_CORRIDOR,','        arrows = '+' + '.join(f'paths{lid}_{i}()' for i in range(len(groups))),'    )','']
            for i,group in enumerate(groups):
                lines += [f'    private fun paths{lid}_{i}(): List<ArrowPath> = listOf(']
                for a in group:
                    xy=', '.join(str(n) for p in dense.helper.corners(a['cells']) for n in p)
                    lines += [f'        path({a["id"]}, Direction.{a["exit"]}, {xy}),']
                lines += ['    )','']
        lines += [helper,'}','']
    (src/'MasterCampaignLevels.kt').write_text('\n'.join(lines),encoding='utf-8')
    (OUT/'level-names.json').write_text(json.dumps({str(l['id']):l['name'] for l in levels},indent=2)+'\n',encoding='utf-8')
    (OUT/'categories.json').write_text(json.dumps({str(l['id']):l['category'] for l in levels},indent=2)+'\n',encoding='utf-8')
    print('EMITTED 150 certified MASTER boards in ten private category classes',flush=True)

def main():
    parser=argparse.ArgumentParser();parser.add_argument('--start',type=int,default=101);parser.add_argument('--end',type=int,default=250);parser.add_argument('--ids');parser.add_argument('--emit',action='store_true');args=parser.parse_args()
    if args.emit:emit();return
    ids=map(int,args.ids.split(',')) if args.ids else range(args.start,args.end+1)
    for lid in ids:
        assert 101<=lid<=250
        path=OUT/f'level-{lid}.json'
        if path.exists():
            l=dense.validate(json.loads(path.read_text()))
            if meets_master(l) and matches_source(l):print(f'Already certified {lid}',flush=True);continue
        save(author(lid))
if __name__=='__main__':main()
