"""Offline expert expansion. Every silhouette is an editable vector drawing; runtime uses fixed coordinates only."""
import argparse, importlib.util, json, math, hashlib
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont
ROOT=Path(__file__).resolve().parents[2]
OUT=ROOT/'artifacts/campaign/expansion';OUT.mkdir(parents=True,exist_ok=True)
spec=importlib.util.spec_from_file_location('dense',ROOT/'tools/campaign/author_dense_campaign.py')
dense=importlib.util.module_from_spec(spec);spec.loader.exec_module(dense)
NAMES=['Apple','Maple Leaf','Oak Tree','Pine Tree','Cactus','Mushroom','Sunflower','Tulip','Fish','Whale','Turtle','Cat','Rabbit','Fox','Bear','Owl','Penguin','Elephant','Duck','Swan','Snail','Crab','Jellyfish','Octopus','Bat','Crown','Castle','House','Key','Padlock','Anchor','Sailboat','Airplane','Helicopter','Car','Locomotive','Trophy','Light Bulb','Umbrella','Tea Cup','Ice Cream','Pizza Slice','Hot Air Balloon','Cloud','Crescent Moon','Ringed Planet','Gemstone','Puzzle Piece','Game Controller','Music Note']

def silhouette(index,size=300):
    im=Image.new('1',(size,size));d=ImageDraw.Draw(im)
    def coords(p):return [v*size/100 for v in p]
    def poly(p,fill=1):d.polygon([(x*size/100,y*size/100) for x,y in p],fill=fill)
    def rect(p,fill=1):d.rectangle(coords(p),fill=fill)
    def oval(p,fill=1):d.ellipse(coords(p),fill=fill)
    def line(p,width):d.line([(x*size/100,y*size/100) for x,y in p],fill=1,width=round(width*size/100),joint='curve')
    i=index
    if i==0: # Apple, distinct cleft, stem and leaf
        oval((12,27,56,87));oval((42,27,88,87));poly([(17,56),(28,90),(43,94),(52,90),(68,94),(84,76),(88,50)])
        poly([(46,32),(48,12),(56,8),(59,13),(54,34)]);poly([(57,26),(63,10),(83,7),(77,22)])
    elif i==1:
        poly([(49,4),(58,28),(69,17),(70,35),(89,28),(82,47),(96,52),(73,65),(80,78),(57,74),(56,96),(44,96),(45,74),(21,78),(28,65),(5,52),(19,47),(12,28),(32,36),(30,17),(42,28)])
    elif i==2:
        oval((10,30,52,68));oval((29,9,69,56));oval((52,28,93,70));oval((27,32,76,76))
        poly([(40,64),(58,64),(59,84),(72,95),(28,95),(42,83)])
    elif i==3:
        poly([(50,3),(76,32),(65,32),(86,57),(72,57),(95,83),(59,83),(59,96),(41,96),(41,83),(5,83),(28,57),(14,57),(35,32),(24,32)])
    elif i==4:
        rect((41,20,59,94));oval((41,10,59,32));rect((14,38,31,61));oval((14,30,31,46));rect((20,53,50,70));rect((70,29,87,49));oval((70,21,87,39));rect((53,43,81,60));rect((26,88,76,97))
    elif i==5:
        oval((6,8,94,75));rect((4,52,96,75),0);rect((38,48,62,84));oval((29,77,73,95));rect((18,44,83,54))
    elif i==6:
        for k in range(10):
            a=2*math.pi*k/10;cx=50+25*math.cos(a);cy=36+25*math.sin(a);oval((cx-11,cy-11,cx+11,cy+11))
        oval((30,16,70,56));rect((46,48,54,96));poly([(46,80),(17,63),(16,78),(45,91)]);poly([(54,68),(81,56),(78,74),(54,81)])
    elif i==7:
        poly([(18,12),(38,24),(50,5),(62,24),(82,12),(78,48),(64,61),(54,62),(54,94),(45,94),(45,62),(31,58),(21,43)])
        poly([(45,87),(19,68),(15,54),(31,59),(45,73)]);poly([(54,78),(69,57),(86,48),(81,68),(54,91)])
    elif i==8:
        oval((6,28,73,79));poly([(67,48),(96,22),(88,55),(97,84),(67,68)]);poly([(32,33),(50,10),(58,37)]);poly([(31,70),(49,92),(57,72)])
    elif i==9:
        oval((5,36,77,83));poly([(64,58),(80,53),(84,26),(94,18),(99,28),(95,53),(82,73),(65,78)]);poly([(36,69),(45,94),(61,83),(54,66)])
        rect((23,16,32,40));poly([(26,22),(12,12),(17,6),(29,14),(41,6),(46,13),(32,22)])
    elif i==10:
        oval((20,25,80,80));oval((41,6,61,33));oval((7,23,31,45));oval((70,23,94,45));oval((8,65,34,86));oval((67,65,93,86));poly([(43,72),(50,97),(58,75)])
    elif i==11:
        poly([(23,37),(20,8),(40,23),(61,23),(80,8),(78,44),(65,60),(62,82),(75,95),(23,95),(30,79),(30,58)]);oval((25,31,75,69));line([(66,86),(84,81),(87,63),(79,56)],13)
    elif i==12:
        oval((25,38,75,94));oval((30,25,74,62));oval((26,3,43,42));oval((53,4,71,44));oval((68,70,91,91));oval((16,82,50,96));oval((50,83,79,97))
    elif i==13:
        poly([(19,9),(42,27),(60,25),(80,6),(78,44),(61,60),(63,79),(80,81),(95,68),(95,89),(80,97),(52,92),(28,96),(31,66),(18,42)]);poly([(18,37),(50,58),(79,37),(61,68),(36,68)])
    elif i==14:
        oval((22,21,79,74));oval((15,6,40,35));oval((60,6,85,35));oval((23,53,77,97));oval((7,48,32,82));oval((69,48,95,82));rect((28,82,44,98));rect((57,82,73,98))
    elif i==15:
        poly([(18,7),(39,20),(63,20),(82,7),(78,37),(87,58),(77,79),(62,89),(67,98),(51,95),(36,98),(39,88),(23,79),(13,56),(20,36)]);oval((20,20,80,85))
    elif i==16:
        oval((30,5,70,51));oval((24,28,76,93));poly([(29,40),(11,74),(19,81),(31,66)]);poly([(71,40),(89,74),(81,81),(69,66)]);poly([(31,84),(19,97),(48,97),(49,87)]);poly([(55,86),(54,97),(83,97),(70,84)])
    elif i==17:
        oval((8,23,76,77));oval((60,18,94,63));poly([(80,40),(96,42),(96,84),(81,90),(73,79),(82,74)]);rect((18,66,34,96));rect((55,66,71,96));line([(12,45),(5,30)],5)
    elif i==18:
        oval((7,49,79,91));oval((57,14,89,47));rect((61,32,81,66));poly([(82,29),(99,36),(83,44)]);poly([(17,58),(4,36),(32,58)])
    elif i==19:
        oval((8,61,83,95));poly([(68,69),(68,43),(74,25),(74,14),(65,8),(54,10),(49,21),(59,28),(62,39),(54,60),(45,75)]);poly([(54,18),(38,24),(53,28)]);poly([(29,70),(6,43),(11,76)])
    elif i==20:
        oval((13,20,71,82));poly([(10,72),(80,70),(86,41),(94,41),(93,83),(82,95),(14,95),(4,87)]);line([(86,51),(78,31)],6);line([(92,46),(96,26)],6)
    elif i==21:
        oval((26,38,76,84));oval((3,15,27,44));oval((74,15,98,44));rect((10,13,18,26),0);rect((83,13,91,26),0);line([(18,39),(26,58),(40,66)],9);line([(84,39),(74,58),(61,66)],9)
        for y in (57,72,85):line([(32,y),(12,y-7),(3,y+3)],7);line([(70,y),(89,y-7),(97,y+3)],7)
    elif i==22:
        oval((12,5,88,71));rect((9,49,91,76),0);rect((16,40,85,57));line([(23,49),(23,77),(17,94)],8);line([(41,50),(35,75),(43,96)],8);line([(58,50),(67,75),(59,96)],8);line([(77,48),(76,80),(87,92)],8)
    elif i==23:
        oval((28,5,73,60));poly([(25,44),(75,44),(77,69),(86,81),(92,64),(98,68),(95,92),(81,97),(69,82),(61,98),(51,99),(47,80),(38,96),(23,97),(8,88),(3,66),(11,62),(19,83),(28,76)])
    elif i==24:
        poly([(49,20),(39,7),(35,28),(22,18),(4,15),(12,47),(21,69),(36,61),(42,77),(50,92),(58,77),(64,61),(79,69),(88,47),(96,15),(77,18),(65,29),(61,7),(52,20)])
    elif i==25:
        poly([(5,23),(28,42),(36,12),(51,37),(66,12),(75,42),(96,23),(84,88),(16,88)]);rect((14,81,87,96))
    elif i==26:
        poly([(8,13),(20,13),(20,25),(31,25),(31,13),(43,13),(43,27),(57,27),(57,13),(69,13),(69,25),(80,25),(80,13),(93,13),(93,97),(7,97)]);rect((41,67,59,100),0);oval((41,59,59,78),0)
    elif i==27:
        poly([(3,45),(50,5),(97,45),(86,45),(86,95),(14,95),(14,45)]);rect((68,11,80,33));rect((43,64,59,97),0)
    elif i==28:
        oval((7,4,68,66));oval((24,20,51,48),0);poly([(41,51),(58,39),(94,77),(83,89),(75,81),(68,89),(56,77),(65,69)])
    elif i==29:
        oval((20,3,81,68));oval((34,17,67,60),0);rect((12,43,89,96));oval((44,60,58,75),0);rect((48,68,54,83),0)
    elif i==30:
        oval((36,3,65,29));oval((44,10,57,21),0);rect((43,22,58,75));rect((23,31,78,44));poly([(44,67),(30,74),(16,62),(15,49),(2,72),(14,70),(27,86),(50,99),(73,86),(87,70),(99,72),(86,49),(83,63),(70,74),(57,67)])
    elif i==31:
        poly([(49,4),(49,67),(5,67)]);poly([(58,15),(94,66),(58,66)]);rect((49,8,55,79));poly([(5,76),(96,76),(79,97),(22,97)])
    elif i==32:
        poly([(46,4),(54,4),(58,37),(95,64),(95,76),(58,60),(57,82),(74,91),(74,98),(50,93),(26,98),(26,91),(43,82),(42,60),(5,76),(5,64),(42,37)])
    elif i==33:
        rect((19,7,87,14));rect((48,11,56,28));oval((29,25,88,69));poly([(33,43),(3,26),(3,46),(38,58)]);rect((1,18,12,39));line([(39,68),(35,82),(83,82)],7);line([(73,66),(74,83)],6)
    elif i==34:
        poly([(8,43),(24,20),(67,20),(82,41),(94,46),(96,78),(5,78),(5,53)]);oval((15,66,36,94));oval((67,66,88,94))
    elif i==35:
        rect((8,16,46,76));rect((45,43,83,76));rect((65,17,82,44));rect((58,12,89,23));poly([(80,60),(98,84),(77,84)]);oval((11,68,36,94));oval((44,68,69,94));oval((74,71,94,92));rect((3,11,50,22))
    elif i==36:
        poly([(25,8),(76,8),(74,48),(63,64),(56,68),(56,83),(77,85),(81,96),(20,96),(24,85),(44,83),(44,68),(34,62),(26,47)]);line([(27,19),(9,18),(10,45),(32,57)],10);line([(75,19),(92,18),(90,45),(69,57)],10)
    elif i==37:
        oval((19,5,82,68));poly([(28,53),(39,76),(63,76),(74,52)]);rect((37,73,65,86));rect((40,90,62,98))
    elif i==38:
        oval((4,4,97,80));rect((1,48,99,100),0);poly([(4,48),(15,42),(28,51),(40,44),(51,51),(63,44),(75,51),(85,42),(97,48)]);rect((47,40,56,86));line([(52,84),(48,96),(34,96),(28,87)],10)
    elif i==39:
        oval((61,26,98,74));oval((70,36,88,64),0);poly([(8,22),(75,22),(71,70),(62,84),(24,84),(13,71)]);oval((3,82,91,96))
    elif i==40:
        oval((16,7,53,46));oval((46,7,83,46));oval((31,1,69,38));rect((15,30,84,52));poly([(20,51),(79,51),(50,99)])
    elif i==41:
        poly([(10,14),(23,7),(41,5),(62,9),(82,19),(95,35),(49,96)]);line([(10,15),(27,13),(48,16),(68,23),(85,37)],12)
    elif i==42:
        oval((15,3,85,74));poly([(24,54),(42,81),(58,81),(76,54)]);rect((37,84,64,99))
    elif i==43:
        oval((4,38,49,83));oval((21,17,70,77));oval((55,30,96,84));rect((23,57,78,85))
    elif i==44:
        oval((8,4,92,96));oval((38,0,104,79),0)
    elif i==45:
        oval((24,8,80,84));poly([(4,65),(16,39),(29,31),(28,46),(16,60),(33,62),(74,42),(90,27),(79,27),(77,14),(94,15),(99,30),(85,52),(54,74),(24,88),(9,82)]);poly([(25,68),(74,43),(74,52),(29,77)],0)
    elif i==46:
        poly([(23,8),(76,8),(98,35),(51,97),(3,35)])
    elif i==47:
        rect((21,22,80,80));oval((38,5,63,34));oval((68,38,96,64));oval((6,39,33,64));oval((39,69,64,96))
    elif i==48:
        poly([(26,20),(73,20),(85,33),(97,80),(92,92),(79,94),(64,73),(37,73),(21,94),(7,90),(3,78),(15,33)]);rect((27,11,40,24));rect((59,11,73,24))
    elif i==49:
        poly([(30,24),(82,5),(82,77),(66,80),(66,29),(45,37),(45,85),(29,88)]);oval((9,69,45,97));oval((48,63,82,91))
    return im

def mask_for(index,target=1950):
    drawing=silhouette(index,1000);occupied=sum(drawing.getdata());fraction=occupied/1000000
    size=max(20,round(math.sqrt(target/(4*fraction))))
    small=drawing.resize((size,size),Image.Resampling.NEAREST)
    mask={(2*x+dx+2,2*y+dy+2) for y in range(size) for x in range(size) if small.getpixel((x,y)) for dx in (0,1) for dy in (0,1)}
    return size*2+4,size*2+4,mask

def outlines():
    sheet=Image.new('RGB',(1000,2000),'#101318');draw=ImageDraw.Draw(sheet)
    for i,name in enumerate(NAMES):
        im=silhouette(i,170).convert('RGB');sheet.paste(im,(i%5*200+15,i//5*200+5));draw.text((i%5*200+10,i//5*200+177),f'{i+51}  {name}',fill='white')
    sheet.save(OUT/'shape-outlines.png')




def meets_expert(level):
    m=level['metrics'];n=len(level['arrows']);k=len(m['openingMoves']);depth=m['dependencyRounds']
    return n>=100 and k<=max(3,n//16) and depth>=max(30,35 if k>3 else 30)

def author(level_id):
    index=level_id-51
    for attempt in range(8):
        target=1950+(attempt//4)*250
        w,h,mask=mask_for(index,target)
        seed=level_id+attempt*1000
        print(f'Packing {level_id} {NAMES[index]} attempt={attempt+1} cells={len(mask)}',flush=True)
        tiles=dense.cluster_mask(mask,w,h,seed)
        choices=dense.choices_for_tiles(tiles,w,h)
        arrows,solution,metrics=dense.choose_directions(tiles,choices,w,h,40+(index//10)*2,2,seed)
        level={'id':level_id,'name':NAMES[index],'cols':w,'rows':h,'difficulty':'EXPERT',
            'movementRule':'HEAD_CORRIDOR','arrows':arrows,'solution':solution,'mask':sorted(mask),'metrics':metrics}
        dense.validate(level)
        if meets_expert(level):return level
        print(f'Refine {level_id}: arrows={len(arrows)} openings={len(metrics["openingMoves"])} rounds={metrics["dependencyRounds"]}',flush=True)
    raise ValueError(f'Could not certify Expert constraints for {level_id}')

def save(level):
    lid=level['id'];path=OUT/f'level-{lid:02}.json'
    path.write_text(json.dumps(level,indent=2)+'\n',encoding='utf-8')
    (OUT/f'level-{lid:02}-solution.txt').write_text(', '.join(map(str,level['solution']))+'\n')
    dense.helper.OUT=OUT;preview=dict(level);preview['name']=f'level-{lid:02}';dense.helper.preview(preview)
    m=level['metrics']
    print(f'SAVED {lid} {level["name"]}: {len(level["arrows"])} arrows, {len(m["openingMoves"])} openings, {m["dependencyRounds"]} rounds, {m["coverage"]:.1%} mask coverage',flush=True)

def emit():
    levels=[json.loads((OUT/f'level-{i:02}.json').read_text()) for i in range(51,101)]
    for level in levels:
        dense.validate(level);assert meets_expert(level),level['id']
    ids=list(range(51,101))
    lines=['package com.amowpuzzle.aitricker.data.level','','import com.amowpuzzle.aitricker.domain.model.*','',
        '/** Fifty distinct Expert silhouettes, stored as editable tail-to-head grid corners.',
        ' * Authored offline; every removal certificate is independently replayed. No runtime generator. */',
        'object ExpertExpansionLevels {','    val ids: Set<Int> = (51..100).toSet()','',
        '    fun level(id: Int): Level? = when (id) {']
    lines += [f'        {i} -> level{i}()' for i in ids]+['        else -> null','    }','',
        '    fun solutionIds(id: Int): List<Int> = when (id) {']
    lines += [f'        {i} -> solution{i}()' for i in ids]+['        else -> emptyList()','    }','',
        '    fun name(id: Int): String? = when (id) {']
    lines += [f'        {i} -> "{NAMES[i-51]}"' for i in ids]+['        else -> null','    }','']
    for level in levels:
        lid=level['id'];m=level['metrics']
        lines += [f'    // {level["name"]}: {len(level["arrows"])} arrows, {len(m["openingMoves"])} openings, {m["dependencyRounds"]} dependency rounds.',
            f'    private fun solution{lid}(): List<Int> = listOf('+', '.join(map(str,level['solution']))+')','',
            f'    private fun level{lid}() = Level(',f'        id = {lid}, cols = {level["cols"]}, rows = {level["rows"]}, difficulty = Difficulty.EXPERT,',
            '        presentation = LevelPresentation.DARK_WHITE,','        movementRule = MovementRule.HEAD_CORRIDOR,','        arrows = listOf(']
        for a in level['arrows']:
            corners=dense.helper.corners(a['cells']);xy=', '.join(str(n) for p in corners for n in p)
            lines += [f'            path({a["id"]}, Direction.{a["exit"]}, {xy}),']
        lines += ['        )','    )','']
    src=ROOT/'app/src/main/java/com/amowpuzzle/aitricker/data/level'
    fixed=(src/'FixedCampaignLevels.kt').read_text()
    helper=fixed[fixed.index('    private fun path('):fixed.rfind('\n}')]
    lines += [helper,'}','']
    (src/'ExpertExpansionLevels.kt').write_text('\n'.join(lines),encoding='utf-8')
    print('EMITTED ExpertExpansionLevels.kt with all50 certified unique shapes.',flush=True)

def main():
    parser=argparse.ArgumentParser();parser.add_argument('--start',type=int,default=51);parser.add_argument('--end',type=int,default=100)
    parser.add_argument('--ids');parser.add_argument('--emit',action='store_true');parser.add_argument('--outlines',action='store_true');args=parser.parse_args()
    if args.outlines:
        outlines();(OUT/'shape-names.json').write_text(json.dumps({str(i+51):name for i,name in enumerate(NAMES)},indent=2));return
    if args.emit:emit();return
    ids=map(int,args.ids.split(',')) if args.ids else range(args.start,args.end+1)
    for lid in ids:
        assert 51<=lid<=100
        saved=OUT/f'level-{lid:02}.json'
        if saved.exists():
            level=dense.validate(json.loads(saved.read_text()))
            if meets_expert(level):print(f'Already certified {lid}',flush=True);continue
        save(author(lid))

if __name__=='__main__':main()
