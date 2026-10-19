from pathlib import Path
p=Path('tools/guitar/author_hooks.py');s=p.read_text(encoding='utf-8-sig')
s=s.replace("palette=[(3,4),(4,3),(2,5),(5,2),(3,3),(2,4),(4,2),(2,3),(3,2)]", "palette=[(2,2)]")
s=s.replace("+[(2,2),(1,4),(4,1),(1,3),(3,1),(1,2),(2,1),(1,1)]", "+[(1,1)]")
s=s.replace("for iteration in range(3):", "for iteration in range(0):")
s=s.replace("hooks_layout.json", "squares_layout.json").replace("hooks-draft.png", "squares-draft.png")
Path('tools/guitar/author_squares.py').write_text(s,encoding='utf-8')
