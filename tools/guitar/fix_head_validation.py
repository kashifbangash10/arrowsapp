from pathlib import Path
p=Path('tools/guitar/refine_contour.py');s=p.read_text(encoding='utf-8-sig')
s=s.replace("old=a['cells'];a['cells']=newpath", "if (newpath[-1][0]-newpath[-2][0],newpath[-1][1]-newpath[-2][1])!=DIRS[a['exit']]: return False\n    old=a['cells'];a['cells']=newpath")
p.write_text(s,encoding='utf-8')
p=Path('tools/guitar/export_level.py');s=p.read_text(encoding='utf-8-sig');s=s[:s.index("p=root/'app/src/main/java/com/amowpuzzle/aitricker/data/level/LevelCatalog.kt'")]+"print('GuitarLevel.kt exported.')\n";p.write_text(s,encoding='utf-8')
p=Path('tools/guitar/audit_level.py');s=p.read_text(encoding='utf-8-sig');a=s.index('# Quarter-cell samples');b=s.index('for id in sequence:',a);s=s[:a]+s[b:];p.write_text(s,encoding='utf-8')
