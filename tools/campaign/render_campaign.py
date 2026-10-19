"""Board-data previews and level manifest from CampaignExport's actual compiled Kotlin data.

Run after tests and the JVM export. These are layout diagrams, not device screenshots.

"""

import ast, json, math, re

from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

ROOT=Path(__file__).resolve().parents[2]

OUT=ROOT/'artifacts/campaign'

catalog=json.loads((OUT/'catalog.json').read_text())

levels=catalog['levels']

by_id={level['id']:level for level in levels}

assert len(levels)==catalog['levelCount'] and sorted(by_id)==list(range(1,len(levels)+1))

NAMES={6:'Dense maze',15:'Heart',20:'Diamond',25:'Lightning',30:'Star',40:'Butterfly',45:'Rocket',49:'Dog',50:'Guitar'}

# Labels are metadata only. Coordinates and solution metrics always come from the compiled catalog.

shape_names_path=OUT/'expansion'/'shape-names.json'

if shape_names_path.exists():

    shape_names=json.loads(shape_names_path.read_text(encoding='utf-8-sig'))

    if isinstance(shape_names,list):

        shape_names={i+51:name for i,name in enumerate(shape_names)}

    else:

        shape_names={int(level_id):name for level_id,name in shape_names.items()}

else:

    # Read the literal label list without importing/executing offline authoring code.

    authoring=ast.parse((ROOT/'tools/campaign/author_expert_expansion.py').read_text(encoding='utf-8-sig'))

    name_node=next(node.value for node in authoring.body if isinstance(node,ast.Assign)

                   and any(isinstance(target,ast.Name) and target.id=='NAMES' for target in node.targets))

    shape_names={i+51:name for i,name in enumerate(ast.literal_eval(name_node))}

assert set(shape_names)==set(range(51,101))

assert all(isinstance(name,str) and name.strip() for name in shape_names.values())

assert len(set(shape_names.values()))==50

NAMES.update(shape_names)

# Modern exports include the names and categories from the actual Kotlin classes.

NAMES.update({level['id']:level['name'] for level in levels if level.get('name')})

master_levels=[level for level in levels if 101<=level['id']<=250]

grandmaster_levels=[level for level in levels if 251<=level['id']<=500]

legend_levels=[level for level in levels if 501<=level['id']<=750]

extreme_levels=[level for level in levels if 751<=level['id']<=1000]

apex_levels=[level for level in levels if 1001<=level['id']<=1500]

if master_levels:

    assert all(level.get('name') and level.get('category') for level in master_levels), 'Re-export the compiled catalog before rendering Master boards'

    assert {level['id'] for level in master_levels}==set(range(101,251))

    assert len({level['category'] for level in master_levels})==10

if grandmaster_levels:

    assert {level['id'] for level in grandmaster_levels}==set(range(251,501))

    assert all(level.get('name') and level.get('category') for level in grandmaster_levels), 'Re-export compiled Grandmaster metadata'

    assert len({level['category'] for level in grandmaster_levels})==25

for tier,group,start,end in [('Legend',legend_levels,501,750),('Extreme',extreme_levels,751,1000)]:

    if group:

        assert {level['id'] for level in group}==set(range(start,end+1))

        assert all(level.get('name') and level.get('category') for level in group)

        assert len({level['category'] for level in group})==25

if apex_levels:

    assert {level['id'] for level in apex_levels}==set(range(1001,1501))

    assert all(level.get('name') and level.get('category') for level in apex_levels)

    assert len({level['category'] for level in apex_levels})==50

VECTORS={'UP':(0,-1),'DOWN':(0,1),'LEFT':(-1,0),'RIGHT':(1,0)}

font_path=Path('C:/Windows/Fonts/segoeui.ttf')

def font(size):

    return ImageFont.truetype(str(font_path),size) if font_path.exists() else ImageFont.load_default()

def board(level,extent=800):

    # Supersampling preserves rounded endpoints and readable heads at small contact-sheet sizes.

    im=Image.new('RGB',(extent,extent),'#101318');draw=ImageDraw.Draw(im)

    margin=extent*.035

    scale=min((extent-margin*2)/level['cols'],(extent-margin*2)/level['rows'])

    ox=(extent-level['cols']*scale)/2;oy=(extent-level['rows']*scale)/2

    width=max(1,round(scale*.28));radius=width/2

    for arrow in level['arrows']:

        points=[(ox+(x+.5)*scale,oy+(y+.5)*scale) for x,y in arrow['cells']]

        p,q=points[:2];size=math.hypot(q[0]-p[0],q[1]-p[1])

        points[0]=(p[0]-(q[0]-p[0])/size*scale*.12,p[1]-(q[1]-p[1])/size*scale*.12)

        draw.line(points,fill='white',width=width,joint='curve')

        for x,y in points:draw.ellipse((x-radius,y-radius,x+radius,y+radius),fill='white')

        x,y=points[-1];dx,dy=VECTORS[arrow['exit']]

        tip=(x+dx*scale*.32,y+dy*scale*.32)

        bx=tip[0]-dx*scale*.62;by=tip[1]-dy*scale*.62

        draw.polygon([tip,(bx-dy*scale*.34,by+dx*scale*.34),(bx+dy*scale*.34,by-dx*scale*.34)],fill='white')

    return im



def sheet(ids,cols,name,tile=440):

    rows=math.ceil(len(ids)/cols);top=90;label=70

    im=Image.new('RGB',(cols*tile,rows*(tile+label)+top),'#101318');d=ImageDraw.Draw(im)

    d.text((20,16),'TAP AWAY  /  CAMPAIGN',font=font(26),fill='white')

    d.text((20,52),'Compiled level data diagrams - white arrows / dark background',font=font(15),fill='#BBC2CD')

    for i,level_id in enumerate(ids):

        level=by_id[level_id];x=(i%cols)*tile;y=top+(i//cols)*(tile+label)

        im.paste(board(level,880).resize((tile,tile),Image.Resampling.LANCZOS),(x,y))

        title=f"{level_id:02}  {NAMES.get(level_id,level['difficulty'].title())}"

        title_size=19

        while title_size>11 and d.textlength(title,font=font(title_size))>tile-24:title_size-=1

        d.text((x+12,y+tile+4),title,font=font(title_size),fill='white')

        metrics=f"{level['arrowCount']} arrows | {level['initiallyFree']} free | {level['dependencyRounds']} rounds"

        metric_size=14

        while metric_size>10 and d.textlength(metrics,font=font(metric_size))>tile-24:metric_size-=1

        d.text((x+12,y+tile+31),metrics,font=font(metric_size),fill='#BBC2CD')

    im.save(OUT/name)
    if '-category-' not in name:
        print(f'Rendered {name}', flush=True)



selected=[1,5,6,15,20,25,26,30,40,45,49,50,51,60,70,80,90,100,101,116,131,146,161,176,191,206,221,236,250]

sheet([i for i in selected if i in by_id],3,'campaign-preview.png')

board(by_id[6],1600).save(OUT/'reference-level-06.png')

sheet([i for i in by_id if i<=50],5,'all-50-levels.png',280)

if len(levels)>50:

    sheet([i for i in by_id if i<=100],5,'all-100-levels.png',280)

    sheet([i for i in by_id if 51<=i<=100],5,'expert-expansion-preview.png',320)

if master_levels:

    sheet([i for i in by_id if i<=250],10,'all-250-levels.png',240)

    sheet([level['id'] for level in master_levels],10,'master-expansion-preview.png',280)

    sheet([101,116,131,146,161,176,191,206,221,236,250],3,'master-highlights.png',520)

    categories=list(dict.fromkeys(level['category'] for level in master_levels))

    for index,category in enumerate(categories):

        category_ids=[level['id'] for level in master_levels if level['category']==category]

        assert len(category_ids)==15

        filename=f"master-category-{index+1:02}-"+re.sub(r'[^a-z0-9]+','-',category.lower()).strip('-')+'.png'

        sheet(category_ids,3,filename,420)

if grandmaster_levels:

    sheet([i for i in by_id if i<=500],10,'all-500-levels.png',160)

    sheet([level['id'] for level in grandmaster_levels],10,'grandmaster-expansion-preview.png',220)

    sheet(list(range(251,501,10)),5,'grandmaster-highlights.png',420)

    categories=list(dict.fromkeys(level['category'] for level in grandmaster_levels))

    for index,category in enumerate(categories):

        category_ids=[level['id'] for level in grandmaster_levels if level['category']==category]

        assert len(category_ids)==10

        filename=f"grandmaster-category-{index+1:02}-"+re.sub(r'[^a-z0-9]+','-',category.lower()).strip('-')+'.png'

        sheet(category_ids,2,filename,600)

for tier,group in [('legend',legend_levels),('extreme',extreme_levels),('apex',apex_levels)]:

    if not group:continue

    sheet([level['id'] for level in group],10,f'{tier}-expansion-preview.png',220)

    sheet([level['id'] for level in group[::10]]+[group[-1]['id']],5,f'{tier}-highlights.png',420)

    categories=list(dict.fromkeys(level['category'] for level in group))

    for index,category in enumerate(categories):

        category_ids=[level['id'] for level in group if level['category']==category]

        assert len(category_ids)==10

        filename=f"{tier}-category-{index+1:02}-"+re.sub(r'[^a-z0-9]+','-',category.lower()).strip('-')+'.png'

        sheet(category_ids,2,filename,600)

if extreme_levels:

    sheet([i for i in by_id if i<=1000],10,'all-1000-levels.png',160)

    sheet([3,30,107,500,501,750,751,1000],4,'difficulty-boundaries.png',520)

if apex_levels:

    sheet(list(by_id),10,'all-1500-levels.png',160)

    sheet([3,30,107,500,501,750,751,1000,1001,1250,1251,1500],4,'difficulty-boundaries.png',520)

(OUT/'boards').mkdir(exist_ok=True)

for level in levels:

    if level['id'] % 250 == 0:
        print(f'Rendering individual board {level["id"]}/{len(levels)}', flush=True)

    board(level,1000).save(OUT/'boards'/f"level-{level['id']:02}.png")

    (OUT/'boards'/f"level-{level['id']:02}-solution.txt").write_text(

        f"Level {level['id']} - {level['difficulty']} - complete valid removal sequence\n"+

        ', '.join(map(str,level['solutionIds']))+'\n')

rows=['| Level | Shape | Category | Difficulty | Arrows | Opening moves | Dependency rounds | Grid occupancy | Turns | Movement |',

      '|---:|---|---|---|---:|---:|---:|---:|---:|---|']

for l in levels:

    occupied=l.get('occupiedCells',sum(len(a['cells']) for a in l['arrows']))

    density=occupied/(l['cols']*l['rows'])

    rows.append(f"| {l['id']} | {NAMES.get(l['id'],'Winding board')} | {l.get('category','Original campaign')} | {l['difficulty'].title()} | {l['arrowCount']} | {l['initiallyFree']} | {l['dependencyRounds']} | {density:.1%} | {l.get('totalTurns','-')} | {l['movementRule']} |")

(OUT/'level-manifest.md').write_text('# Campaign level manifest\n\n'+

    'Exported from the compiled Kotlin catalog. A dependency round removes every arrow currently free. Grid occupancy includes the empty space around a silhouette.\n\n' +

    'The PNG boards and contact sheets are data diagrams, not screenshots of the Android renderer. Fresh 1500-level device captures are stored separately in `device-1500/`; `device/` contains historical captures.\n\n'+

    'Master comprises ten categories, each with five base subjects and three separately named compositions per subject (solo, companion, scene). These are 150 distinct occupied masks and puzzles, not 150 unrelated object species.\n\n'+

    ('Grandmaster adds 25 categories of ten single-object silhouettes. Each new occupied mask is one connected component; individual boards do not combine three separate shapes. Shape recognition still requires visual review.\n\n' if grandmaster_levels else '')+

    ('Legend (501-750) and Extreme (751-1000) extend the 250 Grandmaster subjects with separately authored harder path layouts. They reuse those occupied silhouettes and grid dimensions with more arrows and deeper blocking chains; they are not 500 additional subjects.\n\n' if legend_levels else '')+

    ('Apex (1001-1500) adds 500 further layouts on the same subject family, with a minimum dependency depth above the strongest Extreme board.\n\n' if apex_levels else '')+

    '\n'.join(rows)+'\n')

print(f'Rendered all {len(levels)} fixed boards, preview sheets, removal sequences and metric manifest.')

