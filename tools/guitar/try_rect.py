from pathlib import Path
s=Path('tools/guitar/author_hooks.py').read_text(encoding='utf-8-sig')
s=s.replace("len(good)>=3 or (len(good)>=2 and len(xs)*len(ys)==1)", "len(good)==4")
s=s.replace("tiles=[t for t in tiles if len(t)>=2];choices=[]", "tiles=[t for t in tiles if len({x for x,y in t})>=2 and len({y for x,y in t})>=2];choices=[]")
s=s.replace("hooks_layout.json", "rect_layout.json").replace("hooks-draft.png", "rect-draft.png")
Path('tools/guitar/author_rect.py').write_text(s,encoding='utf-8')
