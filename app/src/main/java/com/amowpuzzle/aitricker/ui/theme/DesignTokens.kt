package com.amowpuzzle.aitricker.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/**
 * Every measurement in the app comes from this file.
 *
 * The redesign asks for one thing above all else: that a small card feel as considered as
 * a large one. That only holds if they are cut from the same ruler, so no screen is allowed
 * to invent a padding, a radius, or a shadow depth of its own - it picks one from here.
 *
 * Nothing in this file is a composable and nothing reads state, so the tokens cannot drift
 * apart from each other the way scattered dp literals always do.
 */

/** Spacing, on a 4dp grid. Names describe weight, not pixel counts. */
object Space {
    val hair = 2.dp
    val xs = 4.dp
    val s = 8.dp
    val m = 12.dp
    val l = 16.dp
    val xl = 20.dp
    val xxl = 24.dp
    val huge = 32.dp
    val giant = 40.dp
}

/**
 * Corner radii. The scale is deliberately short: five steps and a pill. A sixth step would
 * only ever be used once and then copied wrongly.
 */
object Radius {
    val chip = 12.dp
    val small = 18.dp
    val card = 24.dp

    /** The floating header and the bottom dock. */
    val panel = 28.dp

    /** Bottom sheets and the level-preview surface. */
    val sheet = 34.dp

    val chipShape = RoundedCornerShape(chip)
    val smallShape = RoundedCornerShape(small)
    val cardShape = RoundedCornerShape(card)
    val panelShape = RoundedCornerShape(panel)
    val sheetShape = RoundedCornerShape(topStart = sheet, topEnd = sheet)
    val pillShape = RoundedCornerShape(percent = 50)
}

/**
 * The clay recipe.
 *
 * A claymorphic surface is two shadows, not one: a wide soft [ambient] pass that seats the
 * card on the page, and a tight [direct] pass just below it that gives the card thickness.
 * Compose's [androidx.compose.ui.draw.shadow] takes a single elevation, so a clay surface
 * stacks two of them - which is why both numbers live here together rather than being
 * guessed per call site.
 */
object Clay {
    /** Wide, faint, no visible edge. Seats the surface. */
    val ambient = 18.dp

    /** Tight and darker, offset down. Reads as the thickness of the card. */
    val direct = 6.dp
    val directOffsetY = 3.dp

    /** Small cards and pills get the same recipe at half strength. */
    val ambientSmall = 9.dp
    val directSmall = 3.dp

    /**
     * A 3D button rests on a coloured riser. Pressing travels the top face down onto the
     * riser by [pressTravel], leaving [riserHeight] - [pressTravel] still showing, so the
     * button compresses instead of merely tinting.
     */
    val riserHeight = 6.dp
    val pressTravel = 4.dp

    /** The light inner edge along the top of a surface, which is what sells the material. */
    val innerHighlight = 1.dp

    /** Multipliers applied to a base colour to derive its extrusion and its gloss. */
    const val SHADE_FACTOR = 0.72f
    const val TINT_FACTOR = 1.14f
    const val HIGHLIGHT_ALPHA = 0.28f
    const val AMBIENT_ALPHA = 0.10f
}

/**
 * Motion. Durations only - easings live with the animations that use them.
 *
 * The ceiling is deliberate: nothing that stands between a tap and the board may exceed
 * [screenEnterMs], because a transition the player has to wait through stops being
 * atmosphere and becomes a toll.
 */
object Motion {
    const val screenEnterMs = 220
    const val screenExitMs = 180

    /** The board earns a little longer, because arriving at a puzzle should settle. */
    const val boardEnterMs = 260

    const val pressMs = 90
    const val pulseMs = 760
    const val countUpMs = 620

    const val ENTER_SCALE = 0.96f
    const val POP_ENTER_SCALE = 1.04f
    const val EXIT_SCALE = 1.02f
}
