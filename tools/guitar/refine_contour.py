"""Add small traced gaps without introducing overlaps or cycles. No randomness."""
import json
from pathlib import Path
from PIL import Image,ImageDraw
ROOT=Path(__file__).resolve().parents[2]
d=json.loads((ROOT/'tools/guitar/cluster_layout.json').read_text());trace=json.loads((ROOT/'tools/guitar/trace.json').read_text(encoding='utf-8-sig'))
W,H=d['cols'],d['rows'];DIRS={'LEFT':(-1,0),'DOWN':(0,1),'RIGHT':(1,0),'UP':(0,-1)}
im=Image.new('1',(W,H));ImageDraw.Draw(im).polygon([(x+1,y+1) for x,y in trace['outline']],fill=1)
mask={(x,y) for y in range(H) for x in range(W) if im.getpixel((x,y))}
arrows=[dict(a,cells=[tuple(p) for p in a['cells']]) for a in d['arrows']]
def blockers(arrows):
    owner={p:a['id'] for a in arrows for p in a['cells']};out={}
    for a in arrows:
        dx,dy=DIRS[a['exit']];bs=set()
        for x,y in a['cells']:
            x+=dx;y+=dy
            while 0<=x<W and 0<=y<H:
                b=owner.get((x,y))
                if b is not None and b!=a['id']:bs.add(b)
                x+=dx;y+=dy
        out[a['id']]=bs
    return out

def solve(arrows):
    bs=blockers(arrows);left=set(bs);rounds=[];seq=[]
    while left:
        free=sorted(i for i in left if not(bs[i]&left))
        if not free:return None
        rounds.append(free);seq.extend(free);left-=set(free)
    return rounds,seq

def self_block(a):
    own=set(a['cells']);x,y=a['cells'][-1];dx,dy=DIRS[a['exit']]
    x+=dx;y+=dy
    while 0<=x<W and 0<=y<H:
        if (x,y) in own:return True
        x+=dx;y+=dy
    return False

def try_edit(a,newpath):
    if (newpath[-1][0]-newpath[-2][0],newpath[-1][1]-newpath[-2][1])!=DIRS[a['exit']]: return False
    old=a['cells'];a['cells']=newpath
    if not self_block(a):
        s=solve(arrows)
        if s and len(s[0][0])==1 and len(s[0])>=50:return True
    a['cells']=old;return False

used={p for a in arrows for p in a['cells']};changes=0
for sweep in range(4):
    count=0
    for a in arrows:
        if len(a['cells'])>38:continue
        # Replace one existing segment with a right-angle U detour through two missing cells.
        i=0
        while i<len(a['cells'])-1:
            p,q=a['cells'][i:i+2];dx=q[0]-p[0];dy=q[1]-p[1]
            for sign in [1,-1]:
                ox=-dy*sign;oy=dx*sign;u=(p[0]+ox,p[1]+oy);v=(q[0]+ox,q[1]+oy)
                if u in mask-used and v in mask-used:
                    if try_edit(a,a['cells'][:i+1]+[u,v]+a['cells'][i+1:]):
                        used|={u,v};count+=2;break
            i+=1
    for a in arrows:
        x,y=a['cells'][0]
        options=sorted([(x+dx,y+dy) for dx,dy in DIRS.values() if (x+dx,y+dy) in mask-used],key=lambda p:(p[1],p[0]))
        for p in options:
            if try_edit(a,[p]+a['cells']):used.add(p);count+=1;break
    changes+=count;print('REFINE',sweep,'added',count,flush=True)
    if not count:break
s=solve(arrows);d['arrows']=arrows;d['solution']=s[1]
d['metrics']={'arrows':len(arrows),'openingMoves':s[0][0],'dependencyDepth':len(s[0]),'occupiedCells':len(used),'tracedCells':len(mask),'maxPathCells':max(len(a['cells']) for a in arrows),'minPathCells':min(len(a['cells']) for a in arrows),'turns':sum(sum((q[0]-p[0],q[1]-p[1])!=(r[0]-q[0],r[1]-q[1]) for p,q,r in zip(a['cells'],a['cells'][1:],a['cells'][2:])) for a in arrows)}
(ROOT/'tools/guitar/guitar-layout.json').write_text(json.dumps(d,indent=2)+'\n');print(d['metrics'],flush=True)
s=14;pad=20;im=Image.new('RGB',(W*s+pad*2,H*s+pad*2),'#101318');draw=ImageDraw.Draw(im)
for a in arrows:
    points=[(pad+(x+.5)*s,pad+(y+.5)*s) for x,y in a['cells']];draw.line(points,fill='white',width=3,joint='curve')
    x,y=points[-1];dx,dy=DIRS[a['exit']];tip=(x+dx*s*.32,y+dy*s*.32);bx=tip[0]-dx*s*.52;by=tip[1]-dy*s*.52
    draw.polygon([tip,(bx-dy*s*.28,by+dx*s*.28),(bx+dy*s*.28,by-dx*s*.28)],fill='white')
im.save(ROOT/'artifacts/guitar/guitar-data-preview.png')
