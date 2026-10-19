"""Deterministic offline silhouette authoring. The app uses emitted explicit Kotlin only."""
import argparse
import json
import math
from pathlib import Path
from PIL import Image, ImageDraw

ROOT=Path(__file__).resolve().parents[2]
OUT=ROOT/'artifacts/campaign/shapes'
DIRS=[('LEFT',(-1,0)),('DOWN',(0,1)),('RIGHT',(1,0)),('UP',(0,-1))]
SPECS=[(15,'heart','NORMAL',27,27),(20,'diamond','NORMAL',33,35),
       (25,'lightning','HARD',39,47),(30,'star','HARD',49,49),
       (40,'butterfly','EXPERT',49,47),(45,'rocket','EXPERT',47,63)]

def make_mask(name,width,height):
    im=Image.new('1',(width,height));draw=ImageDraw.Draw(im)
    def polygon(points):
        draw.polygon([(round(x*(width-5))+2,round(y*(height-5))+2) for x,y in points],fill=1)
    if name=='heart':
        polygon([(0,.22),(.03,.11),(.14,.02),(.27,0),(.4,.04),(.5,.15),(.6,.04),(.73,0),(.86,.02),(.97,.11),(1,.22),(.98,.4),(.91,.56),(.78,.7),(.5,1),(.22,.7),(.09,.56),(.02,.4)])
    elif name=='diamond':
        polygon([(.5,0),(1,.45),(.5,1),(0,.45)])
    elif name=='lightning':
        polygon([(.52,0),(1,0),(.66,.39),(.98,.39),(.15,1),(.4,.57),(0,.57)])
    elif name=='star':
        polygon([(.5+(.5 if i%2==0 else .23)*math.cos(-math.pi/2+i*math.pi/5),.52+(.5 if i%2==0 else .23)*math.sin(-math.pi/2+i*math.pi/5)) for i in range(10)])
    elif name=='butterfly':
        polygon([(.48,.28),(.37,.12),(.18,.02),(.05,0),(0,.13),(.03,.3),(.12,.46),(.27,.52),(.08,.6),(.04,.75),(.1,.94),(.27,1),(.41,.86),(.49,.63),(.51,.63),(.59,.86),(.73,1),(.9,.94),(.96,.75),(.92,.6),(.73,.52),(.88,.46),(.97,.3),(1,.13),(.95,0),(.82,.02),(.63,.12),(.52,.28)])
        polygon([(.48,.2),(.52,.2),(.54,.83),(.46,.83)])
    elif name=='rocket':
        polygon([(.5,0),(.68,.11),(.77,.29),(.77,.57),(1,.76),(1,.89),(.72,.78),(.68,.84),(.6,.84),(.5,1),(.4,.84),(.32,.84),(.28,.78),(0,.89),(0,.76),(.23,.57),(.23,.29),(.32,.11)])
    return {(x,y) for y in range(height) for x in range(width) if im.getpixel((x,y))}

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

def author(spec):
    global W,H
    level_id,shape_name,difficulty,W,H=spec
    mask=make_mask(shape_name,W,H)
    remaining=set(mask);tiles=[]
    palette=[(2,2)]
    while remaining:
        x,y=min(remaining,key=lambda p:(p[1],p[0]));preferred=(x//3+y//2)%len(palette)
        options=palette[preferred:]+palette[:preferred]+[(1,1)]
        for w,h in options:
            cells={(x+a,y+b) for a in range(w) for b in range(h)}
            if cells<=remaining:
                tiles.append(cells);remaining-=cells;break
    # Compose the small authored blocks into winding, asymmetric compact paths.
    for i,cells in enumerate(tiles):
        if len(cells)<4:continue
        x=min(p[0] for p in cells);y=min(p[1] for p in cells)
        target=[8,12,16,20,12][(x//2+y//2)%5]
        while len(tiles[i])<target:
            cells=tiles[i];near={q for p in cells for q in neighbors(p)}-cells
            candidates=[j for j,c in enumerate(tiles) if j>i and len(c)==4 and c&near and len(c|cells)<=target]
            candidates.sort(key=lambda j:(abs(min(x for x,y in tiles[j])-x)+abs(min(y for x,y in tiles[j])-y),j))
            for j in candidates:
                union=cells|tiles[j]
                if all(find_path(union,d,budget=10000) for _,d in DIRS):
                    tiles[i]=union;tiles[j]=set();break
            else:break
    for iteration in range(3):
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
                if len(good)==4:
                    tiles[j]=union;tiles[i]=set();merged+=1;break
        pass
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
    if difficulty!='EXPERT':
        for i in root_candidates:
            if len(seeds)>=(3 if difficulty=='NORMAL' else 2):break
            seeds.add(i)
    target_rounds={'heart':8,'diamond':11,'lightning':15,'star':18,'butterfly':42,'rocket':57}[shape_name]
    original_seeds=set(seeds)
    best=None
    # Reproducible finite search for checkpoint difficulty, preserving all path coordinates.
    # Every candidate only points at already removable clusters, so cycles cannot be introduced.
    desired_roots=3 if difficulty!='EXPERT' else (2 if shape_name=='butterfly' else 1)
    for offset in range(min(len(root_candidates),12)):
        trial_seeds=set(original_seeds)
        candidates=root_candidates[offset:]+root_candidates[:offset]
        for candidate in candidates:
            if len(trial_seeds)>=desired_roots:break
            trial_seeds.add(candidate)
        for bias in range(-1,target_rounds+1):
            left=set(range(len(tiles)));depth={};order=[];selected={};roots=[]
            while left:
                options=[]
                for i in sorted(left):
                    for name,bs in blockers[i].items():
                        if bs&left or (not bs and i not in trial_seeds):continue
                        dep=max((depth[j] for j in bs),default=0)
                        score=(not bool(bs),-abs(dep-bias),len(bs),len(tiles[i]))
                        options.append((score,i,name,bs))
                if not options:raise Exception('Deadlock')
                _,i,name,bs=max(options,key=lambda x:(x[0],-x[1]))
                if not bs:roots.append(i)
                depth[i]=max((depth[j] for j in bs),default=0)+1
                selected[i]=(name,choices[i][name]);left.remove(i);order.append(i)
            score=abs(max(depth.values())-target_rounds)
            candidate=(score,depth,order,selected,roots)
            if best is None or score<best[0]:best=candidate
            if score==0:break
        if best[0]==0:break
    _,depth,order,selected,roots=best
    arrows=[{'id':i+1,'exit':selected[i][0],'cells':selected[i][1]} for i in range(len(tiles))]
    result={'cols':W,'rows':H,'arrows':arrows,'solution':[i+1 for i in order], 'metrics':{'arrows':len(tiles),'openingMoves':[i+1 for i in roots], 'dependencyRounds':max(depth.values()),'occupiedCells':len(owner),'tracedCells':len(mask)}}
    result.update({'id':level_id,'name':shape_name,'difficulty':difficulty,'mask':sorted(mask)})
    result['metrics']['coverage']=round(len(owner)/len(mask),4)
    independently_validate(result)
    return result

def independently_validate(level):
    """Replay full-cell rigid translations, independently from authoring ray dependencies."""
    vectors=dict(DIRS)
    arrows={a['id']:a for a in level['arrows']}
    occupied={tuple(p):a['id'] for a in arrows.values() for p in a['cells']}
    assert len(occupied)==sum(len(a['cells']) for a in arrows.values()),'Overlap'
    active=set(arrows)
    def clear(arrow_id,remaining):
        a=arrows[arrow_id];dx,dy=vectors[a['exit']]
        for distance in range(1,max(level['cols'],level['rows'])+1):
            for x,y in a['cells']:
                other=occupied.get((x+dx*distance,y+dy*distance))
                if other is not None and other!=arrow_id and other in remaining:return False
        return True
    openings=[]
    for arrow_id,a in arrows.items():
        cells=[tuple(p) for p in a['cells']]
        assert len(cells)>=2 and len(set(cells))==len(cells)
        assert all(0<=x<level['cols'] and 0<=y<level['rows'] for x,y in cells)
        assert all(abs(a[0]-b[0])+abs(a[1]-b[1])==1 for a,b in zip(cells,cells[1:]))
        assert (cells[-1][0]-cells[-2][0],cells[-1][1]-cells[-2][1])==vectors[a['exit']]
        if clear(arrow_id,active):openings.append(arrow_id)
    for arrow_id in level['solution']:
        assert arrow_id in active and clear(arrow_id,active),('Blocked solution',arrow_id)
        active.remove(arrow_id)
    assert not active
    remaining,rounds=set(arrows),0
    while remaining:
        free={i for i in remaining if clear(i,remaining)}
        assert free,'Unsolvable cycle'
        remaining-=free;rounds+=1
    assert sorted(openings)==sorted(level['metrics']['openingMoves'])
    assert rounds==level['metrics']['dependencyRounds']
    level['metrics']['independentValidation']='PASS'
    level['metrics']['turns']=sum(sum((b[0]-a[0],b[1]-a[1])!=(c[0]-b[0],c[1]-b[1]) for a,b,c in zip(ar['cells'],ar['cells'][1:],ar['cells'][2:])) for ar in arrows.values())

def corners(cells):
    return [cells[0]]+[b for a,b,c in zip(cells,cells[1:],cells[2:]) if (b[0]-a[0],b[1]-a[1])!=(c[0]-b[0],c[1]-b[1])]+[cells[-1]]

def preview(level):
    s,pad=16,30
    im=Image.new('RGB',(level['cols']*s+2*pad,level['rows']*s+2*pad),'#101318');draw=ImageDraw.Draw(im)
    for a in level['arrows']:
        points=[(pad+(x+.5)*s,pad+(y+.5)*s) for x,y in corners(a['cells'])]
        draw.line(points,fill='white',width=3,joint='curve')
        for x,y in points:draw.ellipse((x-1.5,y-1.5,x+1.5,y+1.5),fill='white')
        x,y=points[-1];dx,dy=dict(DIRS)[a['exit']];tip=(x+dx*s*.3,y+dy*s*.3)
        bx,by=tip[0]-dx*s*.48,tip[1]-dy*s*.48
        draw.polygon([tip,(bx-dy*s*.24,by+dx*s*.24),(bx+dy*s*.24,by-dx*s*.24)],fill='white')
    im.save(OUT/f"{level['name']}-preview.png")

def emit_kotlin(levels):
    lines=['package com.amowpuzzle.aitricker.data.level','','import com.amowpuzzle.aitricker.domain.model.*','import kotlin.math.sign','',
           '/** Fixed, editable campaign silhouettes. No runtime generation or randomness. */','object ShapeLevels {',
           '    val ids: Set<Int> = setOf('+', '.join(str(l['id']) for l in levels)+')','','    fun level(id: Int): Level? = when (id) {']
    lines += [f"        {l['id']} -> {l['name']}" for l in levels]
    lines += ['        else -> null','    }','','    fun solutionIds(id: Int): List<Int> = when (id) {']
    lines += [f"        {l['id']} -> listOf("+', '.join(map(str,l['solution']))+')' for l in levels]
    lines += ['        else -> emptyList()','    }','']
    for l in levels:
        m=l['metrics']
        lines += [f"    /** {l['name'].title()}: {m['arrows']} arrows, {len(m['openingMoves'])} openings, {m['dependencyRounds']} dependency rounds. */",
                  f"    private val {l['name']}: Level by lazy {{",'        Level(',f"            id = {l['id']}, cols = {l['cols']}, rows = {l['rows']},",
                  f"            difficulty = Difficulty.{l['difficulty']},",'            presentation = LevelPresentation.GUITAR_WHITE,',
                  '            movementRule = MovementRule.SWEPT_TRANSLATION,','            arrows = listOf(']
        for a in l['arrows']:
            coords=', '.join(str(v) for p in corners(a['cells']) for v in p)
            lines.append(f"                arrow({a['id']}, Direction.{a['exit']}, {coords}),")
        lines += ['            )','        )','    }','']
    lines += ['    private fun arrow(id: Int, exit: Direction, vararg coordinates: Int): ArrowPath {',
              '        require(coordinates.size >= 4 && coordinates.size % 2 == 0)',
              '        val corners = coordinates.toList().chunked(2).map { Cell(it[0], it[1]) }',
              '        val cells = mutableListOf(corners.first())','        for ((from, to) in corners.zipWithNext()) {',
              '            require((from.col == to.col) xor (from.row == to.row))','            val dc = (to.col - from.col).sign',
              '            val dr = (to.row - from.row).sign','            var cell = from','            while (cell != to) {',
              '                cell = Cell(cell.col + dc, cell.row + dr)','                cells.add(cell)','            }','        }',
              '        return ArrowPath(id, cells, exit)','    }','}','']
    (ROOT/'app/src/main/java/com/amowpuzzle/aitricker/data/level/ShapeLevels.kt').write_text('\n'.join(lines),encoding='utf-8')

def main():
    parser=argparse.ArgumentParser()
    parser.add_argument('--shape',choices=[s[1] for s in SPECS])
    parser.add_argument('--emit-only',action='store_true')
    args=parser.parse_args();OUT.mkdir(parents=True,exist_ok=True)
    if not args.emit_only:
        for spec in SPECS:
            if args.shape and spec[1]!=args.shape:continue
            level=author(spec)
            (OUT/f'{spec[1]}.json').write_text(json.dumps(level,indent=2)+'\n')
            (OUT/f'{spec[1]}-solution.txt').write_text(' -> '.join(map(str,level['solution']))+'\n')
            preview(level)
            print(spec[1],json.dumps(level['metrics']),flush=True)
    if all((OUT/f'{s[1]}.json').exists() for s in SPECS):
        levels=[json.loads((OUT/f'{s[1]}.json').read_text()) for s in SPECS]
        emit_kotlin(levels)

if __name__=='__main__':main()
