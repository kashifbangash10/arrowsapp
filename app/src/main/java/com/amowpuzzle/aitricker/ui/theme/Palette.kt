package com.amowpuzzle.aitricker.ui.theme

import androidx.compose.ui.graphics.Color
import com.amowpuzzle.aitricker.domain.model.Difficulty

/**
 * The Soft-3D "Clay" palette.
 *
 * The page is a warm cream rather than pure white, because this whole language is
 * built out of soft shadows and pure white makes those shadows read as dirt. On top
 * of it sit pastel clay cards; each chapter carries its own tint so the game shifts
 * colour as the player climbs without ever losing its identity.
 *
 * Arrows are multi-coloured and drawn as extruded tubes - see [ArrowBases], which
 * gives the eight base hues the arrow skin derives its shade and specular from.
 * One rule is absolute: colour never encodes whether an arrow can move. Direction and
 * shape stay the only signal; colour is decoration and identity only.
 *
 * The semantic names the screens already reference are kept and simply retuned toward
 * the warmer page, so adopting this palette is not a rename of the whole app.
 */
object AppColors {
    // Ink and text.
    val Navy = Color(0xFF1B2547)
    val NavySoft = Color(0xFF2C3963)
    val Slate = Color(0xFF515C77)
    val SlateMuted = Color(0xFF8A93A8)

    // The one touchable blue, kept as the primary action colour.
    val Accent = Color(0xFF2E8FF0)
    val AccentPressed = Color(0xFF1974D6)
    val AccentSoft = Color(0xFFE4F0FD)

    // Hints glow in a friendly blue that reads clearly on any arrow colour.
    val HintArrow = Color(0xFF3DA5F5)
    val HintGlow = Color(0xFFB9DEFB)

    val Blocked = Color(0xFFEC5B67)

    val Heart = Color(0xFFFB2C46)
    val HeartEmpty = Color(0xFFE7E2D8)

    // The page is warm cream; cards are near-white or chapter-tinted clay.
    val Background = Color(0xFFFFFFFF)
    val BackgroundTint = Color(0xFFFBF6EE)
    val Board = Color(0xFFFFFDF9)
    val Surface = Color(0xFFF3EEE4)
    val SurfaceStrong = Color(0xFFE8E1D3)
    val GridDot = Color(0xFFD8D0C0)
    val Divider = Color(0xFFEDE7DB)

    // Clay shadow tints. The ambient shadow is warm, not grey, so it sits on cream.
    val ClayShadow = Color(0xFF9A8F76)
    val ClayHighlight = Color(0xFFFFFFFF)

    // Reward accents.
    val StarGold = Color(0xFFFFC531)
    val StarEmpty = Color(0xFFE3DCCC)
    val CoinGold = Color(0xFFFFB627)
    val CoinShade = Color(0xFFE38F12)
    val Flame = Color(0xFFFF7A3D)

    val CompleteTop = Color(0xFF35B0F6)
    val CompleteBottom = Color(0xFF0E7ADE)
    val CompleteRay = Color(0xFFFFFFFF)

    val Confetti = listOf(
        Color(0xFFFF9FB2),
        Color(0xFFFFD54A),
        Color(0xFF7BE0A3),
        Color(0xFF6FE3D2),
        Color(0xFFB9A7F5),
        Color(0xFF7FC4FF),
        Color(0xFFFFFFFF),
        Color(0xFFFF8A65),
        Color(0xFFC5E1A5)
    )

    val LockedCard = Color(0xFFEDE7DB)
    val LockedIcon = Color(0xFFB2A992)

    /**
     * Five chapter tints, one per band of ten levels. Each is the surface colour of
     * that chapter's cards, banners and board frame; the game changes colour as the
     * player progresses. Deliberately pastel so dark ink and 3D arrows read on top.
     */
    val ChapterTints = listOf(
        Color(0xFFDCEBFF), // 1-10  sky
        Color(0xFFD7F3E4), // 11-20 mint
        Color(0xFFFFE7D6), // 21-30 peach
        Color(0xFFEADEFB), // 31-40 lavender
        Color(0xFFFFF0CC)  // 41-50 amber
    )

    /** Deeper companion to each chapter tint, for banners and pressed states. */
    val ChapterInks = listOf(
        Color(0xFF2E6FC2),
        Color(0xFF1F9E6B),
        Color(0xFFDB7A3E),
        Color(0xFF7E52C8),
        Color(0xFFC9992A)
    )

    fun chapterTint(levelId: Int): Color =
        ChapterTints[((levelId - 1) / 10).coerceIn(0, ChapterTints.lastIndex)]

    fun chapterInk(levelId: Int): Color =
        ChapterInks[((levelId - 1) / 10).coerceIn(0, ChapterInks.lastIndex)]

    /** A distinct, readable header accent for each challenge tier. */
    fun difficultyAccent(difficulty: Difficulty): Color = when (difficulty) {
        Difficulty.EASY -> Color(0xFF8CD9B0)
        Difficulty.NORMAL -> Color(0xFF96C8FF)
        Difficulty.HARD -> Color(0xFF99B8FF)
        Difficulty.EXPERT -> Color(0xFFF2AB78)
        Difficulty.MASTER -> Color(0xFFF3D26A)
        Difficulty.GRANDMASTER -> Color(0xFFC3A1FF)
        Difficulty.LEGEND -> Color(0xFF7CE1EA)
        Difficulty.EXTREME -> Color(0xFFFF8D99)
        Difficulty.APEX -> Color(0xFFF2A4FF)
    }

    /**
     * The eight arrow base hues. Widely spaced around the wheel so touching arrows
     * stay distinct, and chosen to survive the shade/specular derivation in the arrow
     * skin without turning muddy. The flat [Navy] is still available as the
     * "classic arrows" fallback for the accessibility setting.
     */
    val ArrowBases = listOf(
        Color(0xFFFF6B6B), // coral
        Color(0xFFFF9F43), // orange
        Color(0xFFFEC531), // amber
        Color(0xFF23C87E), // green
        Color(0xFF20BFB0), // teal
        Color(0xFF3E93F7), // blue
        Color(0xFF6A6FF0), // indigo
        Color(0xFFA65EE8)  // violet
    )
}
