"""Repair dog head-corridor dependencies while retaining its exact occupied mask."""
import importlib.util,json
from pathlib import Path
spec=importlib.util.spec_from_file_location('dense',Path(__file__).with_name('author_dense_campaign.py'));m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m)
l=json.loads((m.OUT/'dog-baseline.json').read_text());tiles=[set(map(tuple,a['cells'])) for a in l['arrows']]
choices=[{k:[tuple(p) for p in v] for k,v in c.items()} for c in json.loads((m.OUT/'dog-choices.json').read_text())]

def seed_metric(tiles,choices):
    blockers=m.dependencies(tiles,choices,69,69)
    bits=[[sum(1<<j for j in bs) for bs in b.values()] for b in blockers]
    roots=[i for i,b in enumerate(bits) if 0 in b]
    def closure(done):
        while True:
            old=done
            for i,bs in enumerate(bits):
                if not (done>>i)&1 and any(b and b&done==b for b in bs):done|=1<<i
            if old==done:return done
    done=0;seeds=[]
    while done.bit_count()<len(tiles):
        options=[(closure(done|(1<<i)).bit_count(),i) for i in roots if not(done>>i)&1]
        if not options:return (1000,seeds)
        _,i=max(options);seeds.append(i);done=closure(done|(1<<i))
    return len(seeds),seeds

m.helper.W=m.helper.H=69
for iteration in range(20):
    count,seeds=seed_metric(tiles,choices);print('DOG_REPAIR',iteration,'tiles',len(tiles),'minimumSeeds',count,flush=True)
    if count<=6:break
    owner={p:i for i,t in enumerate(tiles) for p in t}
    candidates=set()
    for i in seeds:
        for p in tiles[i]:
            for q in m.helper.neighbors(p):
                if q in owner and owner[q]!=i:candidates.add(tuple(sorted((i,owner[q]))))
    best=None
    for i,j in sorted(candidates):
        union=tiles[i]|tiles[j]
        if len(union)>40:continue
        variants={}
        for name,d in m.DIRS:
            path=m.helper.find_path(union,d,budget=10000)
            if path:variants[name]=path
        if not variants:continue
        tt=[t for k,t in enumerate(tiles) if k not in (i,j)]+[union]
        cc=[c for k,c in enumerate(choices) if k not in (i,j)]+[variants]
        next_count,_=seed_metric(tt,cc)
        score=(next_count,-len(variants),len(union))
        if best is None or score<best[0]:best=(score,tt,cc,i,j)
    if best is None or best[0][0]>=count:
        # Splitting a winding cluster introduces a useful inner exit without moving cells.
        for i in seeds:
            variants_seen=set()
            for full_path in choices[i].values():
                key=tuple(full_path)
                if key in variants_seen:continue
                variants_seen.add(key)
                if len(variants_seen)>5:break
                for cut in range(2,len(full_path)-1):
                    parts=[set(full_path[:cut]),set(full_path[cut:])]
                    new_choices=[]
                    for cells in parts:
                        variants={}
                        for name,d in m.DIRS:
                            path=m.helper.find_path(cells,d,budget=7000)
                            if path:variants[name]=path
                        if not variants:break
                        new_choices.append(variants)
                    if len(new_choices)!=2:continue
                    tt=[t for k,t in enumerate(tiles) if k!=i]+parts
                    cc=[c for k,c in enumerate(choices) if k!=i]+new_choices
                    next_count,_=seed_metric(tt,cc)
                    score=(next_count,-sum(map(len,new_choices)),len(tiles[i]))
                    if best is None or score<best[0]:best=(score,tt,cc,i,'SPLIT')
    if best is None or best[0][0]>=count:
        print('NO_IMPROVING_REPARTITION',flush=True);break
    _,tiles,choices,i,j=best;print('REPARTITION',i,j,best[0],flush=True)
(m.OUT/'dog-head-tiles.json').write_text(json.dumps({'tiles':[sorted(t) for t in tiles],'choices':choices}))
arrows,solution,metrics=m.choose_directions(tiles,choices,69,69,38,3,49)
l.update({'arrows':arrows,'solution':solution,'metrics':metrics,'difficulty':'EXPERT','movementRule':'HEAD_CORRIDOR'})
m.save_level(m.validate(l))
