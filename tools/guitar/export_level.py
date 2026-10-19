import json
from pathlib import Path
root=Path(__file__).resolve().parents[2]
d=json.loads((root/'tools/guitar/guitar-layout.json').read_text())
lines=[]
for a in d['arrows']:
    pts=a['cells'];corners=[pts[0]]
    for p,q,r in zip(pts,pts[1:],pts[2:]):
        if (q[0]-p[0],q[1]-p[1])!=(r[0]-q[0],r[1]-q[1]):corners.append(q)
    corners.append(pts[-1])
    coords=', '.join(str(n) for p in corners for n in p)
    lines.append(f"            arrow({a['id']}, Direction.{a['exit']}, {coords}),")
seq=', '.join(map(str,d['solution']))
source='''package com.amowpuzzle.aitricker.data.level

import com.amowpuzzle.aitricker.domain.model.ArrowPath
import com.amowpuzzle.aitricker.domain.model.Cell
import com.amowpuzzle.aitricker.domain.model.Difficulty
import com.amowpuzzle.aitricker.domain.model.Direction
import com.amowpuzzle.aitricker.domain.model.Level
import com.amowpuzzle.aitricker.domain.model.LevelPresentation
import com.amowpuzzle.aitricker.domain.model.MovementRule
import kotlin.math.sign

/**
 * Level 1 only. Fixed grid coordinates traced from the supplied diagonal guitar.
 * Each row below is one editable tail-to-head orthogonal polyline (corner pairs).
 * Expansion only fills straight segments; there is no generation or randomness.
 * The playable paths themselves form the silhouette, with no image or outline layer.
 *
 * Verified: 115 arrows, one opening (110), 62 dependency rounds, 1,465 occupied cells.
 * Reference trace and reproducible previews/solution are in tools/guitar and artifacts/guitar.
 */
object GuitarLevel {
    const val COLS = 69
    const val ROWS = 69

    val level: Level by lazy {
        Level(
            id = 1,
            cols = COLS,
            rows = ROWS,
            difficulty = Difficulty.EXPERT,
            presentation = LevelPresentation.GUITAR_WHITE,
            movementRule = MovementRule.SWEPT_TRANSLATION,
            arrows = listOf(
'''+ '\n'.join(lines)+'''
            )
        )
    }

    /** A complete removal certificate, also replayed by GuitarLevelTest. */
    val solutionIds: List<Int> = listOf(
        '''+seq+'''
    )

    private fun arrow(id: Int, exit: Direction, vararg coordinates: Int): ArrowPath {
        require(coordinates.size >= 4 && coordinates.size % 2 == 0)
        val corners = coordinates.toList().chunked(2).map { Cell(it[0], it[1]) }
        val cells = mutableListOf(corners.first())
        for ((from, to) in corners.zipWithNext()) {
            require((from.col == to.col) xor (from.row == to.row)) {
                "Guitar arrow $id has a diagonal or empty segment"
            }
            val dc = (to.col - from.col).sign
            val dr = (to.row - from.row).sign
            var cell = from
            while (cell != to) {
                cell = Cell(cell.col + dc, cell.row + dr)
                cells.add(cell)
            }
        }
        return ArrowPath(id, cells, exit).also {
            require(it.isWellFormed(COLS, ROWS)) { "Malformed guitar arrow $id" }
        }
    }
}
'''
p=root/'app/src/main/java/com/amowpuzzle/aitricker/data/level/GuitarLevel.kt';p.write_text(source,encoding='utf-8')
print('GuitarLevel.kt exported.')
