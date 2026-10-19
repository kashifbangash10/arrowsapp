"""Single-object, editable Grandmaster silhouettes; offline authoring only."""
import importlib, math
from PIL import Image
CATEGORIES = ['Safari','Forest Friends','Farm & Outback','Reptiles & Amphibians','Bird Wonders','Deep Ocean','Tiny Creatures','Flower Studio','Botanical','Orchard & Harvest','Sweet Bakery','Kitchen Craft','Tableware','Hand Tools','Craft & Study','Land Vehicles','Aviation','Watercraft','Musical Craft','Sport & Toys','Wearables','Architecture','Fantasy','Technology','Adventure']
def module(level_id):
    if not 251 <= level_id <= 500: raise ValueError(level_id)
    return importlib.import_module('grandmaster_shapes_a' if level_id <= 375 else 'grandmaster_shapes_b')
def name_for(level_id): return module(level_id).name_for(level_id)
def category_for(level_id): return CATEGORIES[(level_id-251)//10]
def silhouette(level_id,size=500): return module(level_id).silhouette(level_id,size)
def mask_for(level_id,target=5000):
    im=silhouette(level_id,1000).convert('1')
    bbox=im.getbbox()
    if bbox is None: raise ValueError(('Empty silhouette',level_id))
    im=im.crop(bbox)
    # Preserve aspect ratio while fitting the authored object within a uniform square.
    scale=940/max(im.size)
    im=im.resize((max(1,round(im.width*scale)),max(1,round(im.height*scale))),Image.Resampling.NEAREST)
    canvas=Image.new('1',(1000,1000))
    canvas.paste(im,((1000-im.width)//2,(1000-im.height)//2))
    fraction=sum(bool(x) for x in canvas.getdata())/1000000
    size=max(24,round(math.sqrt(target/(4*fraction))))
    small=canvas.resize((size,size),Image.Resampling.NEAREST)
    mask={(2*x+dx+2,2*y+dy+2) for y in range(size) for x in range(size)
          if small.getpixel((x,y)) for dx in (0,1) for dy in (0,1)}
    return size*2+4,size*2+4,mask

def components(mask):
    left=set(mask);groups=[]
    while left:
        pending=[left.pop()];group=set(pending)
        while pending:
            x,y=pending.pop()
            for p in ((x-1,y),(x+1,y),(x,y-1),(x,y+1)):
                if p in left:left.remove(p);group.add(p);pending.append(p)
        groups.append(group)
    return sorted(groups,key=len,reverse=True)
