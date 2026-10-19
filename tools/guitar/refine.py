from pathlib import Path
p=Path('tools/guitar/author_layout.py')
s=p.read_text(encoding='utf-8-sig')
s=s.replace("left=set(range(len(tiles))); removed=set(); depth={}; order=[]; selected={}; roots=[]", """def closure(seed):
    done=set(seed)
    while True:
        more={i for i,t in enumerate(tiles) if i not in done and any(bs and bs<=done for vs in t['choices'].values() for _,bs in vs)}
        if not more: return done
        done|=more
root_candidates=[i for i,t in enumerate(tiles) if any(not bs for vs in t['choices'].values() for _,bs in vs)]
seeds=set(); covered=set()
while len(covered)<len(tiles):
    options=[(len(closure(covered|{i})),i) for i in root_candidates if i not in covered]
    if not options: break
    _,i=max(options); seeds.add(i); covered=closure(covered|{i})
print('SEEDS',len(seeds),[i+1 for i in seeds], 'CLOSURE',len(covered))
left=set(range(len(tiles))); removed=set(); depth={}; order=[]; selected={}; roots=[]""")
s=s.replace("if blockers & left: continue", "if blockers & left or (not blockers and i not in seeds): continue")
p.write_text(s,encoding='utf-8')
