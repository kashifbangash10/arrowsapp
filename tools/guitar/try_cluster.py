from pathlib import Path
s=Path('tools/guitar/author_hooks.py').read_text(encoding='utf-8-sig')
s=s.replace("palette=[(3,4),(4,3),(2,5),(5,2),(3,3),(2,4),(4,2),(2,3),(3,2)]", "palette=[(2,2)]")
s=s.replace("+[(2,2),(1,4),(4,1),(1,3),(3,1),(1,2),(2,1),(1,1)]", "+[(1,1)]")
marker='for iteration in range(3):'
insert='''# Compose the small authored blocks into winding, asymmetric compact paths.
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
'''
s=s.replace(marker,insert+marker)
s=s.replace("len(good)>=3 or (len(good)>=2 and len(xs)*len(ys)==1)", "len(good)==4")
s=s.replace("hooks_layout.json", "cluster_layout.json").replace("hooks-draft.png", "cluster-draft.png")
Path('tools/guitar/author_cluster.py').write_text(s,encoding='utf-8')
