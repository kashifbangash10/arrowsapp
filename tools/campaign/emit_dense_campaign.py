"""Emit validated fixed campaign JSON as editable Kotlin coordinates. No runtime generation."""
import importlib.util,json
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
OUT=ROOT/'artifacts/campaign/dense'
DEST=ROOT/'app/src/main/java/com/amowpuzzle/aitricker/data/level'
spec=importlib.util.spec_from_file_location('dense',Path(__file__).with_name('author_dense_campaign.py'));m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m)
SHAPES={15:'heart',20:'diamond',25:'lightning',30:'star',40:'butterfly',45:'rocket'}
levels={i:json.loads((OUT/f'level-{i:02}.json').read_text()) for i in range(1,51) if i!=6}
for l in levels.values():m.validate(l)
HELPER='''    private fun path(id: Int, exit: Direction, vararg xy: Int): ArrowPath {
        require(xy.size >= 4 && xy.size % 2 == 0)
        val corners = xy.toList().chunked(2).map { Cell(it[0], it[1]) }
        val cells = mutableListOf(corners.first())
        for ((from, to) in corners.zipWithNext()) {
            require((from.col == to.col) xor (from.row == to.row))
            val dx = (to.col - from.col).coerceIn(-1, 1)
            val dy = (to.row - from.row).coerceIn(-1, 1)
            var cursor = from
            while (cursor != to) {
                cursor = Cell(cursor.col + dx, cursor.row + dy)
                cells.add(cursor)
            }
        }
        return ArrowPath(id, cells, exit)
    }
'''

def declaration(l,name='level',is_property=False):
    metric=l['metrics']
    lines=[f"    // {metric['arrows']} arrows; {len(metric['openingMoves'])} openings; {metric['dependencyRounds']} dependency rounds."]
    if is_property:lines += ['    val level: Level by lazy {','        Level(']
    else:lines += [f'    private fun {name}() = Level(']
    ind='            ' if is_property else '        '
    lines += [f"{ind}id = {l['id']}, cols = {l['cols']}, rows = {l['rows']}, difficulty = Difficulty.{l['difficulty']},",f'{ind}presentation = LevelPresentation.DARK_WHITE,',f'{ind}movementRule = MovementRule.HEAD_CORRIDOR,',f'{ind}arrows = listOf(']
    for a in l['arrows']:
        coords=', '.join(str(n) for p in m.helper.corners(a['cells']) for n in p)
        lines += [f"{ind}    path({a['id']}, Direction.{a['exit']}, {coords}),"]
    lines += [ind+')',('        )' if is_property else '    )')]
    if is_property:lines+=['    }']
    return '\n'.join(lines)+'\n'

def header(name,internal=False):
    return ['package com.amowpuzzle.aitricker.data.level','','import com.amowpuzzle.aitricker.domain.model.*','',
            '/** Explicit tail-to-head paths, authored offline and independently replayed with HEAD_CORRIDOR.',
            ' * The runtime expands only straight segments; no random generation or fallback. */',
            ('internal ' if internal else '')+'object '+name+' {']

def emit_regular():
    ids=[i for i in levels if i not in SHAPES and i not in (49,50)]
    lines=header('FixedCampaignLevels',True)+['    fun level(id: Int): Level = when (id) {']
    lines += [f'        {i} -> level{i}()' for i in ids]
    lines += ['        else -> error("No fixed campaign level $id")','    }','','    fun solutionIds(id: Int): List<Int> = when (id) {']
    lines += [f"        {i} -> listOf("+', '.join(map(str,levels[i]['solution']))+')' for i in ids]
    lines += ['        else -> emptyList()','    }','']
    lines += [declaration(levels[i],f'level{i}') for i in ids]
    lines += [HELPER,'}','']
    (DEST/'FixedCampaignLevels.kt').write_text('\n'.join(lines),encoding='utf-8')

def emit_shapes():
    lines=header('ShapeLevels')+['    val ids: Set<Int> = setOf('+', '.join(map(str,SHAPES))+')','','    fun level(id: Int): Level? = when (id) {']
    lines += [f'        {i} -> {name}()' for i,name in SHAPES.items()]
    lines += ['        else -> null','    }','','    fun solutionIds(id: Int): List<Int> = when (id) {']
    lines += [f"        {i} -> listOf("+', '.join(map(str,levels[i]['solution']))+')' for i in SHAPES]
    lines += ['        else -> emptyList()','    }','']
    lines += [declaration(levels[i],name) for i,name in SHAPES.items()]
    lines += [HELPER,'}','']
    (DEST/'ShapeLevels.kt').write_text('\n'.join(lines),encoding='utf-8')

for name,lid in [('DogLevel',49),('GuitarLevel',50)]:
    l=levels[lid]
    lines=header(name)+[f"    const val COLS = {l['cols']}",f"    const val ROWS = {l['rows']}",'',declaration(l,is_property=True),'',
          '    /** Complete valid removal certificate for path-following withdrawal. */',
          '    val solutionIds: List<Int> = listOf('+', '.join(map(str,l['solution']))+')','',HELPER,'}','']
    (DEST/f'{name}.kt').write_text('\n'.join(lines),encoding='utf-8')
emit_regular();emit_shapes()
(DEST/'LevelCatalog.kt').write_text('''package com.amowpuzzle.aitricker.data.level

import com.amowpuzzle.aitricker.domain.model.Difficulty
import com.amowpuzzle.aitricker.domain.model.Level
import java.util.concurrent.ConcurrentHashMap

/** 500 explicit puzzles: five Easy, twenty Hard, seventy-five Expert, 150 Master and 250 Grandmaster levels. */
object LevelCatalog {
    const val TOTAL_LEVELS = 500
    private val cache = ConcurrentHashMap<Int, Level>()
    val levelIds: IntRange get() = 1..TOTAL_LEVELS

    fun difficultyOf(id: Int): Difficulty = when (id.coerceIn(1, TOTAL_LEVELS)) {
        in 1..5 -> Difficulty.EASY
        in 6..25 -> Difficulty.HARD
        in 26..100 -> Difficulty.EXPERT
        in 101..250 -> Difficulty.MASTER
        else -> Difficulty.GRANDMASTER
    }

    fun level(id: Int): Level {
        val key = id.coerceIn(1, TOTAL_LEVELS)
        cache[key]?.let { return it }
        val built = when (key) {
            6 -> ReferenceMazeLevel.level
            49 -> DogLevel.level
            50 -> GuitarLevel.level
            in 51..100 -> requireNotNull(ExpertExpansionLevels.level(key))
            in 101..250 -> requireNotNull(MasterCampaignLevels.level(key))
            in 251..TOTAL_LEVELS -> requireNotNull(GrandmasterCampaignLevels.level(key))
            else -> ShapeLevels.level(key) ?: FixedCampaignLevels.level(key)
        }
        check(built.id == key && built.difficulty == difficultyOf(key))
        return cache.putIfAbsent(key, built) ?: built
    }

    fun preload(range: Iterable<Int> = levelIds) = range.forEach { level(it) }
    fun isCached(id: Int) = cache.containsKey(id)
}
''',encoding='utf-8')
print('PRODUCTION DATA EMITTED: all49validated levels plus root ReferenceMazeLevel at6; only Easy1-5/Hard6-25/Expert26-50, allHEAD.')
