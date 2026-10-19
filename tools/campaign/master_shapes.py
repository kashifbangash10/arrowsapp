"""Editable, deterministic silhouettes for Master levels 101-250.

Only this OFFLINE authoring module draws filled masks. The Android game stores
explicit individual arrow paths, never an image. Each category contains five
recognizable objects and three named compositions per object: a solo silhouette,
a contextual companion, and a scene. No random generation or rotated duplicates.
"""
import importlib.util
import math
import json
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parents[2]
OUT = ROOT / 'artifacts/campaign/master'
_spec = importlib.util.spec_from_file_location('expert_vectors', ROOT / 'tools/campaign/author_expert_expansion.py')
_expert = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(_expert)

# (category, (object name, editable drawing identifier) x 5)
CATEGORIES = [
    ('Nature & Garden', [('Maple Leaf', 1), ('Oak Tree', 2), ('Cactus', 4), ('Mushroom', 5), ('Sunflower', 6)]),
    ('Woodland Animals', [('Rabbit', 12), ('Fox', 13), ('Bear', 14), ('Deer', 'deer'), ('Squirrel', 'squirrel')]),
    ('Sea Life', [('Whale', 9), ('Turtle', 10), ('Crab', 21), ('Dolphin', 'dolphin'), ('Seahorse', 'seahorse')]),
    ('Birds', [('Owl', 15), ('Penguin', 16), ('Swan', 19), ('Eagle', 'eagle'), ('Peacock', 'peacock')]),
    ('Transport', [('Sailboat', 31), ('Car', 34), ('Locomotive', 35), ('Bicycle', 'bicycle'), ('Excavator', 'excavator')]),
    ('Space', [('Rocket', 'rocket'), ('Satellite', 'satellite'), ('Telescope', 'telescope'), ('Flying Saucer', 'ufo'), ('Astronaut', 'astronaut')]),
    ('Music', [('Guitar', 'guitar'), ('Violin', 'violin'), ('Piano', 'piano'), ('Trumpet', 'trumpet'), ('Drum', 'drum')]),
    ('Food & Treats', [('Apple', 0), ('Ice Cream', 40), ('Pizza Slice', 41), ('Cupcake', 'cupcake'), ('Pineapple', 'pineapple')]),
    ('Buildings', [('Castle', 26), ('Lighthouse', 'lighthouse'), ('Windmill', 'windmill'), ('Pagoda', 'pagoda'), ('Bridge', 'bridge')]),
    ('Treasures & Play', [('Crown', 25), ('Key', 28), ('Trophy', 36), ('Gemstone', 46), ('Game Controller', 48)]),
]

# Secondary icons are semantic, visible parts of each composition, not generic frames.
COMPANIONS = [
    [('Seedling', 'seedling'), ('Maple Leaf', 1), ('Sun', 'sun'), ('Acorn', 'acorn'), ('Butterfly', 'butterfly')],
    [('Carrot', 'carrot'), ('Pine Tree', 3), ('Honey Pot', 'honey'), ('Pine Tree', 3), ('Acorn', 'acorn')],
    [('Starfish', 'starfish'), ('Seaweed', 'seaweed'), ('Shell', 'shell'), ('Fish', 8), ('Shell', 'shell')],
    [('Crescent', 44), ('Fish', 8), ('Water Lily', 'lily'), ('Mountain', 'mountain'), ('Feather', 'feather')],
    [('Cloud', 43), ('Traffic Light', 'traffic'), ('Signal', 'traffic'), ('Helmet', 'helmet'), ('Boulder', 46)],
    [('Planet', 45), ('Planet', 45), ('Crescent', 44), ('Star', 'starfish'), ('Rocket', 'rocket')],
    [('Music Note', 49), ('Music Note', 49), ('Music Note', 49), ('Music Note', 49), ('Music Note', 49)],
    [('Leaf', 1), ('Cherry', 'cherry'), ('Tomato', 0), ('Cherry', 'cherry'), ('Palm', 'palm')],
    [('Flag', 'flag'), ('Sailboat', 31), ('Tulip', 7), ('Cloud', 43), ('House', 27)],
    [('Gemstone', 46), ('Padlock', 29), ('Medal', 'medal'), ('Crown', 25), ('Puzzle Piece', 47)],
]
SCENE_NAMES = [
    ['Maple Grove', 'Oak Woodland', 'Desert Cacti', 'Mushroom Patch', 'Sunflower Garden'],
    ['Rabbit Family', 'Fox Woodland', 'Bear Cub Trail', 'Deer Woodland', 'Squirrel Orchard'],
    ['Whale Pod', 'Turtle Reef', 'Crab Shore', 'Dolphin Pair', 'Seahorse Reef'],
    ['Owl Family', 'Penguin Colony', 'Swan Lake', 'Eagle Peaks', 'Peacock Garden'],
    ['Sailing Fleet', 'City Traffic', 'Rail Yard', 'Cycling Route', 'Construction Site'],
    ['Rocket Launch', 'Orbital Station', 'Stargazing', 'Saucer Formation', 'Moonwalk'],
    ['Guitar Duet', 'String Duet', 'Piano Recital', 'Brass Band', 'Drum Ensemble'],
    ['Apple Harvest', 'Ice Cream Party', 'Pizza Feast', 'Cupcake Bakery', 'Tropical Harvest'],
    ['Castle Kingdom', 'Lighthouse Coast', 'Windmill Village', 'Temple Garden', 'Bridge Town'],
    ['Royal Treasure', 'Key Collection', 'Champions Podium', 'Gem Collection', 'Arcade Night'],
]

def _parts(level_id):
    if not 101 <= level_id <= 250:
        raise ValueError('Master level ID must be in 101..250')
    offset = level_id - 101
    return offset // 15, (offset % 15) // 3, offset % 3

def category_for(level_id):
    return CATEGORIES[_parts(level_id)[0]][0]

def name_for(level_id):
    category, index, variant = _parts(level_id)
    base = CATEGORIES[category][1][index][0]
    if variant == 0:
        return base
    if variant == 1:
        return base + ' & ' + COMPANIONS[category][index][0]
    return SCENE_NAMES[category][index]

category = category_for
name = name_for

def drawing(identifier, size=500):
    if isinstance(identifier, int):
        return _expert.silhouette(identifier, size)
    im = Image.new('1', (size, size)); d = ImageDraw.Draw(im)
    def poly(points, fill=1):
        d.polygon([(x*size/100, y*size/100) for x, y in points], fill=fill)
    def rect(box, fill=1):
        d.rectangle([v*size/100 for v in box], fill=fill)
    def oval(box, fill=1):
        d.ellipse([v*size/100 for v in box], fill=fill)
    def line(points, width=8, fill=1):
        d.line([(x*size/100, y*size/100) for x,y in points], fill=fill, width=max(1,round(width*size/100)), joint='curve')
    i = identifier
    if i == 'deer':
        oval((15,42,75,75)); poly([(61,54),(64,22),(77,14),(89,29),(81,44),(77,65)])
        poly([(73,20),(60,11),(57,3),(65,4),(68,11),(75,13),(76,2),(83,2),(82,16),(94,9),(97,15),(86,24)])
        poly([(20,64),(31,64),(28,96),(17,96)]);poly([(59,65),(70,62),(77,95),(65,95)])
        poly([(19,49),(4,31),(5,48),(17,59)]); poly([(68,27),(55,21),(57,32),(69,36)])
    elif i == 'squirrel':
        oval((4,5,57,64)); oval((16,13,39,39),0); oval((19,35,58,85)); oval((42,38,77,94))
        oval((56,18,88,50));poly([(59,26),(59,6),(71,20),(80,8),(83,28)])
        poly([(68,55),(94,51),(96,64),(72,68)]);oval((45,84,88,98))
    elif i == 'dolphin':
        # Leaping dolphin: curved back, distinct beak, dorsal fin, flipper and forked tail.
        poly([(24,77),(18,60),(20,43),(30,29),(47,20),(64,19),(79,25),(89,36),(99,40),(97,47),(85,47),(76,42),(65,39),(52,39),(41,44),(33,54),(30,65),(30,77)])
        poly([(46,24),(43,3),(62,20)]);poly([(53,38),(62,63),(47,54),(43,42)])
        poly([(25,73),(5,77),(2,87),(16,88),(27,82),(38,94),(50,93),(43,81),(30,72)])
    elif i == 'seahorse':
        poly([(67,5),(82,15),(87,28),(75,38),(67,37),(61,50),(76,64),(73,82),(59,96),(37,95),(25,84),(24,72),(36,66),(47,70),(47,80),(39,83),(53,84),(60,77),(59,68),(41,59),(37,45),(43,30),(35,25),(39,15),(54,10)])
        poly([(53,10),(51,1),(63,5),(72,1),(72,12)]);poly([(40,35),(27,39),(36,53),(44,53)]);poly([(72,21),(98,23),(97,36),(73,36)])
    elif i == 'eagle':
        poly([(49,25),(58,22),(64,29),(73,25),(87,11),(98,8),(91,33),(77,49),(62,57),(59,76),(68,95),(51,89),(34,95),(42,75),(38,58),(23,50),(9,33),(2,8),(14,12),(27,25),(41,30)])
        poly([(51,26),(50,11),(59,6),(68,10),(70,18),(82,20),(69,26)]);poly([(24,32),(6,30),(13,46),(33,51)]);poly([(76,32),(95,30),(87,46),(67,51)])
    elif i == 'peacock':
        for k in range(7):
            a=math.pi*(0.12+0.76*k/6);cx=50+34*math.cos(a);cy=54-34*math.sin(a)
            oval((cx-16,cy-23,cx+16,cy+23))
        oval((15,22,86,75))
        for k in range(7):
            a=math.pi*(0.12+0.76*k/6);cx=50+34*math.cos(a);cy=54-34*math.sin(a)
            oval((cx-4,cy-7,cx+4,cy+7),0)
        oval((37,25,65,70),0);oval((38,60,66,87));oval((43,29,59,44));rect((47,39,56,68))
        poly([(57,34),(67,38),(57,41)]);poly([(44,83),(37,98),(50,95),(62,98),(58,82)])
        line([(46,32),(42,23)],4);line([(51,31),(51,21)],4);line([(56,32),(61,23)],4)
    elif i == 'bicycle':
        oval((2,49,46,94));oval((13,60,35,83),0);oval((54,49,98,94));oval((65,60,87,83),0)
        line([(25,72),(40,35),(61,70),(25,72),(53,44),(70,44),(78,72)],9)
        line([(32,31),(49,31)],8);line([(70,44),(66,20),(80,18)],8)
    elif i == 'excavator':
        oval((8,74,69,96));rect((15,73,61,96));rect((19,40,53,76));rect((31,26,54,56));rect((35,32,47,49),0)
        poly([(52,52),(65,16),(76,11),(92,55),(83,62),(69,32),(61,64)])
        poly([(82,58),(98,61),(95,80),(77,85),(71,75),(83,74)])
    elif i == 'rocket':
        poly([(50,2),(67,20),(73,45),(72,73),(61,84),(39,84),(28,73),(27,45),(33,20)])
        poly([(30,51),(15,65),(12,90),(36,77)]);poly([(70,51),(85,65),(88,90),(64,77)])
        poly([(39,83),(43,98),(50,92),(57,98),(61,83)]);oval((39,30,61,52),0)
    elif i == 'satellite':
        poly([(34,39),(55,18),(74,38),(53,60)]);poly([(5,19),(22,3),(42,23),(25,40)]);poly([(60,59),(78,41),(98,62),(80,80)])
        line([(26,30),(67,71)],9);poly([(23,59),(43,78),(61,60),(40,43)])
        line([(50,74),(39,91)],8);oval((30,83,46,99));line([(69,23),(82,10)],7)
    elif i == 'telescope':
        poly([(11,39),(76,7),(91,37),(26,69)]);poly([(8,39),(21,33),(36,64),(23,71)])
        poly([(72,4),(84,1),(99,34),(88,41)]);rect((45,52,56,70));line([(50,64),(23,97)],10);line([(50,64),(77,97)],10);line([(50,64),(50,96)],8)
    elif i == 'ufo':
        oval((28,8,73,56));oval((3,35,97,71));poly([(12,53),(87,53),(74,78),(27,78)])
        line([(30,76),(22,92)],9);line([(70,76),(78,92)],9);oval((35,20,65,40),0)
    elif i == 'astronaut':
        oval((24,2,77,52));oval((35,12,66,34),0);rect((28,41,73,76));rect((35,49,63,62),0)
        poly([(29,43),(15,38),(5,59),(15,70),(27,58)]);poly([(71,44),(84,35),(97,57),(87,68),(74,58)])
        poly([(31,69),(48,69),(46,95),(19,95),(21,85),(30,85)]);poly([(53,69),(70,69),(72,84),(82,86),(83,97),(55,97)])
        rect((21,42,31,70));rect((71,44,80,69))
    elif i == 'guitar':
        oval((6,51,60,98));oval((15,36,59,77));oval((32,36,57,54),0);oval((4,39,22,58),0)
        poly([(34,51),(76,9),(86,20),(45,62)]);poly([(73,11),(83,2),(98,14),(89,26),(79,22)])
        oval((26,63,39,76),0)
    elif i == 'violin':
        oval((16,53,65,97));oval((27,30,71,68));oval((11,37,34,62),0);oval((59,42,82,67),0)
        poly([(43,41),(69,5),(79,13),(55,50)]);oval((68,1,84,17));line([(83,18),(35,98)],7)
    elif i == 'piano':
        oval((18,3,84,58));poly([(17,27),(43,23),(82,36),(94,64),(94,80),(8,80),(8,55)])
        rect((8,60,94,76));rect((16,77,26,98));rect((76,76,87,98))
        for x in (23,39,55,71):rect((x,60,x+5,70),0)
    elif i == 'trumpet':
        poly([(5,43),(51,43),(61,36),(68,19),(94,12),(94,77),(68,69),(61,54),(54,52),(15,52),(15,69),(50,69),(50,52),(59,52),(59,75),(50,82),(16,82),(5,72)])
        rect((24,27,32,48));rect((39,27,47,48));rect((19,23,37,30));rect((34,23,52,30))
    elif i == 'drum':
        oval((8,27,92,58));rect((8,43,92,78));oval((8,61,92,92));oval((8,26,92,54))
        for x in (21,46,71):poly([(x,52),(x+7,52),(x+7,78),(x,78)],0)
        line([(21,5),(72,30)],7);line([(79,5),(32,30)],7);oval((14,1,28,13));oval((73,1,87,13))
    elif i == 'cupcake':
        poly([(15,54),(85,54),(75,97),(25,97)]);oval((8,28,92,66));oval((20,13,79,51));oval((33,2,68,36))
        for x in (35,52,69):line([(x,67),(x-3,85)],5,0)
    elif i == 'pineapple':
        oval((20,31,80,98));poly([(31,42),(12,12),(36,23),(33,2),(49,20),(62,2),(62,26),(85,12),(72,42)])
        line([(32,52),(63,84)],5,0);line([(64,51),(33,85)],5,0)
    elif i == 'lighthouse':
        poly([(37,26),(64,26),(76,96),(24,96)]);rect((31,15,70,34));poly([(24,17),(50,2),(77,17)])
        rect((41,18,59,26),0);rect((36,49,66,58),0);rect((43,80,57,97),0)
        poly([(31,21),(4,12),(4,34)]);poly([(69,21),(96,12),(96,34)])
    elif i == 'windmill':
        poly([(39,39),(61,39),(76,97),(24,97)]);poly([(32,39),(50,17),(68,39)])
        line([(16,5),(84,70)],12);line([(84,5),(16,70)],12);oval((40,28,60,48));rect((44,77,58,98),0)
    elif i == 'pagoda':
        poly([(12,29),(26,22),(50,3),(75,22),(89,29)]);rect((29,27,71,44));poly([(5,53),(22,42),(79,42),(95,53)])
        rect((23,51,78,71));poly([(1,81),(18,68),(82,68),(99,81)]);rect((18,79,83,96));rect((44,80,58,98),0)
    elif i == 'bridge':
        rect((3,66,97,80));rect((18,16,30,96));rect((71,16,83,96));poly([(17,19),(24,5),(32,19)]);poly([(70,19),(77,5),(85,19)])
        line([(4,46),(24,28),(50,51),(77,28),(97,46)],8)
        for x,y in ((8,46),(38,41),(50,51),(62,41),(92,44)):rect((x-2,y,x+3,68))
    elif i == 'seedling':
        rect((45,38,55,97));oval((11,25,52,57));oval((49,9,91,47));poly([(25,87),(76,87),(91,99),(9,99)])
    elif i == 'sun':
        oval((20,20,80,80))
        for k in range(8):
            a=k*math.pi/4;line([(50+32*math.cos(a),50+32*math.sin(a)),(50+48*math.cos(a),50+48*math.sin(a))],9)
    elif i == 'acorn':
        oval((21,28,79,96));oval((11,24,89,53));rect((44,5,56,31))
    elif i == 'butterfly':
        oval((4,8,49,57));oval((52,8,97,57));oval((12,49,50,92));oval((51,49,90,92));rect((44,22,57,88));line([(48,29),(35,5)],6);line([(53,29),(67,5)],6)
    elif i == 'carrot':
        poly([(19,29),(74,23),(54,68),(19,99)]);poly([(39,30),(29,7),(46,17),(55,1),(62,20),(83,6),(73,32)])
    elif i == 'honey':
        oval((15,19,85,99));rect((24,7,76,24));rect((31,42,69,68),0)
    elif i == 'starfish':
        poly([(50+47*math.cos(-math.pi/2+k*math.pi/5)*(1 if k%2==0 else .46),50+47*math.sin(-math.pi/2+k*math.pi/5)*(1 if k%2==0 else .46)) for k in range(10)])
    elif i == 'seaweed':
        line([(48,98),(53,67),(39,47),(49,17)],13);line([(51,85),(27,68),(21,37)],12);line([(52,70),(74,49),(74,18)],12)
    elif i == 'shell':
        oval((7,8,93,84));poly([(8,48),(34,95),(66,95),(93,48)]);line([(50,82),(50,20)],6,0);line([(43,80),(23,31)],5,0);line([(57,80),(77,31)],5,0)
    elif i == 'lily':
        oval((3,57,97,93));poly([(50,80),(40,48),(10,37),(20,70)]);poly([(50,79),(59,49),(89,37),(80,69)]);poly([(30,62),(31,22),(50,38),(69,22),(70,62)]);poly([(37,43),(50,4),(64,43)])
    elif i == 'mountain':
        poly([(3,95),(43,8),(64,51),(76,31),(98,95)])
    elif i == 'feather':
        oval((27,2,87,83));poly([(17,99),(10,94),(60,35),(66,41)])
    elif i == 'traffic':
        rect((22,3,78,78));rect((44,72,56,99))
        for y in (15,37,59):oval((39,y,61,y+14),0)
    elif i == 'helmet':
        oval((4,10,93,85));rect((1,64,99,99),0);poly([(52,59),(93,57),(93,84),(70,92),(59,79)]);rect((18,61,43,75))
    elif i == 'cherry':
        oval((2,49,47,96));oval((53,49,98,96));line([(24,53),(46,13),(77,53)],9);poly([(45,16),(60,1),(85,8),(64,22)])
    elif i == 'palm':
        poly([(42,95),(58,95),(54,38),(44,34)])
        for p in [[(49,35),(18,10),(2,18),(31,29)],[(50,36),(83,9),(99,17),(71,30)],[(48,34),(17,32),(3,59),(28,45)],[(52,33),(83,34),(98,60),(72,46)],[(43,35),(44,3),(59,2),(57,35)]]:poly(p)
    elif i == 'flag':
        rect((15,2,25,98));poly([(23,6),(81,6),(70,25),(87,42),(24,43)])
    elif i == 'medal':
        poly([(15,3),(38,3),(55,49),(33,55)]);poly([(63,3),(86,3),(68,55),(46,49)]);oval((18,37,82,99));oval((35,54,65,83),0)
    else:
        raise ValueError('Unknown editable vector: '+str(identifier))
    return im

def silhouette(level_id, size=500):
    c, b, variant = _parts(level_id)
    base_id = CATEGORIES[c][1][b][1]
    companion_id = COMPANIONS[c][b][1]
    result = Image.new('1',(size,size))
    def put(identifier, box):
        x0,y0,x1,y1 = [round(v*size/100) for v in box]
        icon=drawing(identifier,600)
        bbox=icon.getbbox()
        if bbox: icon=icon.crop(bbox)
        # Preserve the original object's aspect ratio in every composition.
        ratio=min((x1-x0)/icon.width,(y1-y0)/icon.height)
        icon=icon.resize((max(1,round(icon.width*ratio)),max(1,round(icon.height*ratio))),Image.Resampling.NEAREST)
        x=x0+(x1-x0-icon.width)//2;y=y0+(y1-y0-icon.height)//2
        result.paste(1,(x,y,x+icon.width,y+icon.height),icon)
    if variant==0:
        put(base_id,(3,3,97,97))
    elif variant==1:
        put(base_id,(1,18,79,99))
        put(companion_id,(71,1,99,30))
    else:
        put(base_id,(0,10,66,98))
        put(base_id,(67,44,99,97))
        put(companion_id,(68,2,99,33))
    return result

def mask_for(level_id,target=3400):
    drawing_im=silhouette(level_id,1000)
    occupied=sum(drawing_im.getdata());fraction=occupied/1000000
    size=max(20,round(math.sqrt(target/(4*fraction))))
    small=drawing_im.resize((size,size),Image.Resampling.NEAREST)
    mask={(2*x+dx+2,2*y+dy+2) for y in range(size) for x in range(size)
          if small.getpixel((x,y)) for dx in (0,1) for dy in (0,1)}
    return size*2+4,size*2+4,mask

def outlines():
    import hashlib
    OUT.mkdir(parents=True,exist_ok=True)
    metadata=[];seen=set()
    for c,(category_name,objects) in enumerate(CATEGORIES):
        sheet=Image.new('RGB',(1200,810),'#101318');d=ImageDraw.Draw(sheet)
        try:
            font=ImageFont.truetype('C:/Windows/Fonts/arial.ttf',15)
            title=ImageFont.truetype('C:/Windows/Fonts/arialbd.ttf',26)
        except OSError:font=None;title=None
        d.text((18,10),f'{101+c*15}-{115+c*15}  {category_name}',fill='white',font=title)
        for b in range(5):
            for v in range(3):
                level_id=101+c*15+b*3+v
                im=silhouette(level_id,210)
                x=b*240+15;y=v*250+55
                sheet.paste('white',(x,y,x+210,y+210),im)
                d.text((x,y+213),str(level_id)+' '+name_for(level_id),fill='white',font=font)
                cols,rows,mask=mask_for(level_id)
                digest=hashlib.sha256(repr(sorted(mask)).encode()).hexdigest()
                assert digest not in seen,('Duplicate mask',level_id)
                seen.add(digest)
                metadata.append({'id':level_id,'category':category_name,'name':name_for(level_id),'baseObject':objects[b][0],
                    'composition':['Solo','Companion','Scene'][v],'cols':cols,'rows':rows,'occupiedCells':len(mask),'maskSha256':digest})
        sheet.save(OUT/f'category-{c+1:02}-outlines.png')
    (OUT/'shape-metadata.json').write_text(json.dumps(sorted(metadata,key=lambda x:x['id']),indent=2)+'\n',encoding='utf-8')
    combined=Image.new('RGB',(1800,2025),'#101318')
    for c in range(10):
        thumb=Image.open(OUT/f'category-{c+1:02}-outlines.png').resize((600,405),Image.Resampling.LANCZOS)
        combined.paste(thumb,((c%3)*600,(c//3)*405))
    combined.crop((0,0,1800,1620)).save(OUT/'all-master-outlines.png')
    print(f'150 distinct masks validated; cells {min(x["occupiedCells"] for x in metadata)}-{max(x["occupiedCells"] for x in metadata)}',flush=True)

if __name__=='__main__':
    outlines()
