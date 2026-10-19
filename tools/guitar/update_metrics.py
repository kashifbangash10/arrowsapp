from pathlib import Path
for name in ['tools/guitar/audit_level.py','tools/guitar/export_level.py','app/src/main/java/com/amowpuzzle/aitricker/data/level/GuitarLevel.kt']:
    p=Path(name);s=p.read_text(encoding='utf-8-sig').replace('len(rounds)==61','len(rounds)==62').replace('61 dependency rounds, 1,469','62 dependency rounds, 1,465');p.write_text(s,encoding='utf-8')
