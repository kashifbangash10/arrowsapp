package com.amowpuzzle.aitricker.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.amowpuzzle.aitricker.R

/**
 * Tabular (monospaced) figures. Applied to every style that shows a live count - the
 * arrow tally, hearts, coins, move totals - so a digit changing width never nudges the
 * layout. Nunito ships with the `tnum` feature.
 */
private const val TabularFigures = "tnum"

/** Nunito, bundled with the app so the game looks the same everywhere and offline. */
val AppFontFamily = FontFamily(
    Font(R.font.nunito_semibold, FontWeight.SemiBold),
    Font(R.font.nunito_bold, FontWeight.Bold),
    Font(R.font.nunito_extrabold, FontWeight.ExtraBold)
)

object AppText {
    /** The heavy rounded display face: splash wordmark, big level numbers on the map. */
    val Display = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 58.sp,
        lineHeight = 62.sp,
        letterSpacing = (-1.5).sp
    )
    val ScreenTitle = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 34.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.5).sp
    )
    val MenuTitle = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 46.sp,
        lineHeight = 50.sp,
        letterSpacing = (-1).sp
    )
    val CardTitle = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 24.sp,
        lineHeight = 30.sp
    )
    val LevelLabel = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 19.sp
    )
    val Pill = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 15.sp,
        fontFeatureSettings = TabularFigures
    )
    /** A live count on a pill or dock badge - coins, arrows left - that must not twitch. */
    val Counter = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 17.sp,
        fontFeatureSettings = TabularFigures
    )
    val Body = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 22.sp
    )
    val Button = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 18.sp,
        letterSpacing = 0.2.sp
    )
    val Badge = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 12.sp
    )
    val Caption = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp
    )
}

val AppTypography = Typography(
    bodyLarge = AppText.Body,
    bodyMedium = AppText.Body,
    labelLarge = AppText.Button,
    titleLarge = AppText.CardTitle
)
