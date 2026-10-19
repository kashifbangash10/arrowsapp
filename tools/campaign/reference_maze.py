"""Manual trace of the user's 36-arrow rectangular reference. Corners are editable.
Grid origin is screenshot (94,453), pitch 41 pixels. Runtime stores coordinates only.
"""
import json, math
from pathlib import Path
from PIL import Image,ImageDraw
ROOT=Path(__file__).resolve().parents[2]
CORNERS=[
[(1,0),(0,0),(0,3),(1,3),(1,1),(4,1),(4,4),(9,4),(9,2),(13,2)],
[(6,0),(2,0)],
[(7,0),(11,0)],
[(12,0),(13,0),(13,1),(12,1)],
[(11,1),(7,1),(7,3),(5,3),(5,2),(6,2),(6,1),(5,1)],
[(2,2),(3,2)],
[(3,3),(2,3),(2,4),(1,4)],
[(8,2),(8,3)],
[(10,3),(10,4)],
[(11,6),(12,6),(12,4),(11,4),(11,3),(13,3)],
[(13,4),(13,6)],
[(10,5),(11,5)],
[(7,5),(5,5)],
[(3,6),(3,5),(2,5)],
[(1,5),(1,6)],
[(0,4),(0,9)],
[(4,6),(4,5)],
[(8,6),(8,5),(9,5),(9,6),(10,6)],
[(2,9),(1,9),(1,7),(4,7),(4,8),(5,8),(5,6),(7,6)],
[(7,7),(8,7)],
[(6,7),(6,9)],
[(8,8),(9,8),(9,7)],
[(10,7),(13,7)],
[(11,8),(10,8)],
[(13,8),(13,15),(3,15)],
[(2,15),(0,15),(0,10),(1,10),(1,14),(12,14),(12,8)],
[(2,10),(2,12)],
[(4,9),(4,10),(3,10),(3,8),(2,8)],
[(6,12),(3,12),(3,11)],
[(11,13),(9,13),(9,11),(8,11),(8,12),(7,12),(7,10),(6,10),(6,11),(5,11),(5,9)],
[(8,10),(8,9),(7,9),(7,8)],
[(10,9),(9,9),(9,10)],
[(10,10),(11,10),(11,9)],
[(11,11),(10,11)],
[(10,12),(11,12)],
[(2,13),(8,13)]
]
D={(1,0):'RIGHT',(-1,0):'LEFT',(0,1):'DOWN',(0,-1):'UP'}
def expand(corners):
    cells=[corners[0]]
    for x,y in corners[1:]:
        a,b=cells[-1];assert (x==a)!=(y==b)
        dx=(x>a)-(x<a);dy=(y>b)-(y<b)
        while (a,b)!=(x,y):a+=dx;b+=dy;cells.append((a,b))
    return cells
paths=[expand(c) for c in CORNERS]
owner={}
for i,p in enumerate(paths):
    for c in p:
        assert c not in owner,('Overlap',i+1,owner.get(c),c)
        owner[c]=i+1
def blockers(path):
    x,y=path[-1];a,b=path[-2];dx=x-a;dy=y-b;out=set()
    for k in range(1,17):
        if (x+k*dx,y+k*dy) in owner:out.add(owner[(x+k*dx,y+k*dy)])
    return out
bs={i+1:blockers(p) for i,p in enumerate(paths)}
left=set(bs);seq=[];rounds=0;opens=[]
while left:
    free=sorted(i for i in left if not bs[i]&left)
    if not seq:opens=free
    if not free:break
    left-=set(free);seq+=free;rounds+=1
print({'arrows':len(paths),'occupied':len(owner),'density':len(owner)/224,'openings':opens,'rounds':rounds,'unsolved':sorted(left),'selfblocked':[i for i in bs if i in bs[i]]})
out=ROOT/'artifacts/campaign/reference';out.mkdir(parents=True,exist_ok=True)
def draw():
    im=Image.new('RGB',(800,970),'#101318');d=ImageDraw.Draw(im);s=48;ox=88;oy=90
    for i,p in enumerate(paths):
        pts=[(ox+x*s,oy+y*s) for x,y in p];d.line(pts,fill='white',width=8,joint='curve')
        for x,y in pts:d.ellipse((x-4,y-4,x+4,y+4),fill='white')
        x,y=pts[-1];a,b=pts[-2];dx=(x-a)/s;dy=(y-b)/s
        d.polygon([(x+dx*8,y+dy*8),(x-dx*9-dy*9,y-dy*9+dx*9),(x-dx*9+dy*9,y-dy*9-dx*9)],fill='white')
    im.save(out/'trace.png')
draw()
(out/'trace.json').write_text(json.dumps({'corners':CORNERS,'paths':paths,'solution':seq,'openings':opens,'rounds':rounds,'unsolved':sorted(left)},indent=2))

assert not left and len(seq)==36
source=ROOT/'app/src/main/java/com/amowpuzzle/aitricker/data/level/ReferenceMazeLevel.kt'
lines=['package com.amowpuzzle.aitricker.data.level','','import com.amowpuzzle.aitricker.domain.model.*','','/** Manual coordinate trace of the supplied 36-arrow rectangular board.',' * Screenshot lattice: origin (94,453), pitch 41 px; converted to grid + one-cell margin.',' * 3 openings, 20 dependency rounds, 221 occupied cells. No runtime generation.',' * Corners below are ordered tail to head; all IDs remain stable. */','internal object ReferenceMazeLevel {','    val solutionIds: List<Int> = listOf('+', '.join(map(str,seq))+')','','    val level: Level = Level(','        id = 6, cols = 16, rows = 18, difficulty = Difficulty.HARD,','        presentation = LevelPresentation.DARK_WHITE,','        movementRule = MovementRule.HEAD_CORRIDOR,','        arrows = listOf(']
for i,(corners,path) in enumerate(zip(CORNERS,paths)):
    dx=path[-1][0]-path[-2][0];dy=path[-1][1]-path[-2][1]
    coords=', '.join(str(n+1) for c in corners for n in c)
    lines.append(f'            path({i+1}, Direction.{D[dx,dy]}, {coords}),')
lines+=['        )','    )','','    private fun path(id: Int, exit: Direction, vararg xy: Int): ArrowPath {','        require(xy.size >= 4 && xy.size % 2 == 0)','        val corners = xy.toList().chunked(2).map { Cell(it[0], it[1]) }','        val cells = mutableListOf(corners.first())','        for ((from, to) in corners.zipWithNext()) {','            require((from.col == to.col) != (from.row == to.row))','            val dx = (to.col - from.col).coerceIn(-1, 1)','            val dy = (to.row - from.row).coerceIn(-1, 1)','            var cursor = from','            while (cursor != to) {','                cursor = Cell(cursor.col + dx, cursor.row + dy)','                cells.add(cursor)','            }','        }','        return ArrowPath(id, cells, exit)','    }','}']
source.write_text('\n'.join(lines)+'\n')
print('Wrote',source)
