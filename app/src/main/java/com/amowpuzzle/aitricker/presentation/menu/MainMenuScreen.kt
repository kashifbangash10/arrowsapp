package com.amowpuzzle.aitricker.presentation.menu

import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.amowpuzzle.aitricker.R
import com.amowpuzzle.aitricker.data.level.LevelCatalog
import com.amowpuzzle.aitricker.data.prefs.GamePreferences
import com.amowpuzzle.aitricker.ui.components.BrandArrowIcon
import com.amowpuzzle.aitricker.ui.components.ClayButton
import com.amowpuzzle.aitricker.ui.components.ClayCard
import com.amowpuzzle.aitricker.ui.components.ClayCircleButton
import com.amowpuzzle.aitricker.ui.components.ClayPill
import com.amowpuzzle.aitricker.ui.components.GearIcon
import com.amowpuzzle.aitricker.ui.components.PlainTextButton
import com.amowpuzzle.aitricker.ui.theme.AppColors
import com.amowpuzzle.aitricker.ui.theme.AppText
import com.amowpuzzle.aitricker.ui.theme.Radius
import com.amowpuzzle.aitricker.ui.theme.Space

/**
 * Home.
 *
 * Level-based mobile puzzle games open the same way, and for good reason: the player is
 * told where they are, given one obvious way forward, and shown everything else second.
 * So this screen is a hero tile with the mark, a progress card that names the chapter the
 * player is in, one full-width Continue action, and quieter routes to Levels and the
 * tutorial underneath.
 *
 * The page carries the tint of the chapter the player is currently in, so returning to
 * Home after clearing ten levels feels like the game has moved with them.
 */
@Composable
fun MainMenuScreen(
    snapshot: GamePreferences.Snapshot,
    onPlay: (Int) -> Unit,
    onLevels: () -> Unit,
    onSettings: () -> Unit,
    onTutorial: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cleared = snapshot.completed.size
    val resume = snapshot.resumeLevel
    val total = LevelCatalog.TOTAL_LEVELS
    val tint = AppColors.chapterTint(resume)
    val ink = AppColors.chapterInk(resume)
    val settingsLabel = stringResource(R.string.settings)

    // The mark breathes so Home never looks like a screenshot.
    val breathe = rememberInfiniteTransition(label = "homeBreathe")
    val lift by breathe.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2600, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "markLift"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(tint, AppColors.BackgroundTint))
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = Space.xxl),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = Space.xs),
                horizontalArrangement = Arrangement.End
            ) {
                ClayCircleButton(
                    onClick = onSettings,
                    contentDescription = settingsLabel,
                    size = 48.dp
                ) { iconTint ->
                    GearIcon(color = iconTint, size = 24.dp)
                }
            }

            Spacer(Modifier.weight(1f))

            // The hero: the mark on a clay tile in the chapter's deeper ink.
            ClayCard(
                modifier = Modifier
                    .size(126.dp)
                    .graphicsLayer { translationY = -6.dp.toPx() * lift },
                tint = ink,
                shape = Radius.cardShape
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    BrandArrowIcon(color = AppColors.Background, size = 72.dp)
                }
            }

            Spacer(Modifier.height(Space.xxl))
            Text(
                text = stringResource(R.string.app_name),
                style = AppText.MenuTitle,
                color = AppColors.Navy,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(Space.s))
            Text(
                text = stringResource(R.string.game_subtitle),
                style = AppText.Body,
                color = AppColors.Slate,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(Space.xl))

            ProgressCard(cleared = cleared, total = total, ink = ink)

            Spacer(Modifier.weight(1f))

            ClayButton(
                text = if (cleared == 0) {
                    stringResource(R.string.menu_play)
                } else {
                    stringResource(R.string.menu_continue_level, resume)
                },
                onClick = { onPlay(resume) }
            )
            Spacer(Modifier.height(Space.m))
            ClayButton(
                text = stringResource(R.string.menu_levels),
                onClick = onLevels,
                container = AppColors.Background,
                contentColor = AppColors.Navy
            )
            Spacer(Modifier.height(Space.xs))
            PlainTextButton(
                text = stringResource(R.string.menu_how_to_play),
                onClick = onTutorial,
                color = AppColors.Slate,
                style = AppText.Body
            )

            Text(
                text = stringResource(R.string.developer_attribution),
                style = AppText.Caption,
                color = AppColors.SlateMuted
            )
            Spacer(Modifier.height(Space.l))
        }
    }
}

/**
 * How far the player has come: a filled track plus the count. The bar is the honest
 * signal here - "12 of 50" is a fact, but a bar that is a quarter full is a feeling.
 */
@Composable
private fun ProgressCard(cleared: Int, total: Int, ink: Color) {
    val fraction = if (total <= 0) 0f else (cleared.toFloat() / total).coerceIn(0f, 1f)
    ClayCard(
        modifier = Modifier.fillMaxWidth(),
        tint = AppColors.Background,
        shape = Radius.cardShape,
        contentPadding = PaddingValues(horizontal = Space.l, vertical = Space.m)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.menu_progress, cleared, total),
                    style = AppText.Caption,
                    color = AppColors.Slate
                )
                ClayPill(
                    text = "$cleared",
                    container = AppColors.BackgroundTint,
                    contentColor = ink
                )
            }
            Spacer(Modifier.height(Space.s))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(Radius.pillShape)
                    .background(AppColors.Surface)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction)
                        .fillMaxHeight()
                        .clip(Radius.pillShape)
                        .background(Brush.horizontalGradient(listOf(ink, AppColors.Accent)))
                )
            }
        }
    }
}
