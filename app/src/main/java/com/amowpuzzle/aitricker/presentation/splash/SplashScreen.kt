package com.amowpuzzle.aitricker.presentation.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Text
import com.amowpuzzle.aitricker.R
import com.amowpuzzle.aitricker.domain.model.Direction
import com.amowpuzzle.aitricker.ui.components.BrandArrowIcon
import com.amowpuzzle.aitricker.ui.components.ClayCard
import com.amowpuzzle.aitricker.ui.theme.AppColors
import com.amowpuzzle.aitricker.ui.theme.AppText
import com.amowpuzzle.aitricker.ui.theme.Radius
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * The opening beat.
 *
 * Rather than parking a static logo on screen, the splash performs the game's one verb:
 * the mark lands, and four arrows shoot out of it and clear the frame. A player who has
 * never seen the game has been shown the rule before the first level loads, and a player
 * who has seen it fifty times gets a half-second of motion instead of a wait.
 *
 * Every animated value is read inside a [graphicsLayer] block, so the whole sequence runs
 * on the render thread - nothing here recomposes per frame. [onFinished] fires once, after
 * the fade, and the caller decides where to go next.
 */
@Composable
fun SplashScreen(onFinished: () -> Unit, modifier: Modifier = Modifier) {
    // The mark's landing, with overshoot.
    val land = remember { Animatable(0f) }

    // The four arrows leaving, 0 at the mark and 1 well off screen.
    val fly = remember { Animatable(0f) }

    // The wordmark rising under the mark.
    val word = remember { Animatable(0f) }

    // The whole screen dimming out into the Home behind it.
    val exit = remember { Animatable(1f) }

    LaunchedEffect(Unit) {
        launch {
            land.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = 0.44f,
                    stiffness = Spring.StiffnessLow
                )
            )
        }
        launch {
            delay(190)
            fly.animateTo(1f, tween(560, easing = FastOutSlowInEasing))
        }
        launch {
            delay(300)
            word.animateTo(1f, tween(440, easing = LinearOutSlowInEasing))
        }
        delay(1450)
        exit.animateTo(0f, tween(280, easing = FastOutSlowInEasing))
        onFinished()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.BackgroundTint)
            .graphicsLayer { alpha = exit.value },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // The mark, and the four arrows leaving it. They share this Box so the arrows
            // launch from the mark's real centre rather than from the middle of the screen.
            Box(contentAlignment = Alignment.Center) {
                Direction.ALL.forEachIndexed { index, direction ->
                    val hue = AppColors.ArrowBases[index * 2 % AppColors.ArrowBases.size]
                    BrandArrowIcon(
                        color = hue,
                        size = 34.dp,
                        modifier = Modifier.graphicsLayer {
                            val t = fly.value
                            val reach = 190.dp.toPx() * t
                            translationX = direction.dCol * reach
                            translationY = direction.dRow * reach
                            rotationZ = direction.rotationDegrees - 90f
                            // Solid leaving the mark, gone by the time it clears the frame.
                            alpha = (1f - t) * 0.9f
                            val shrink = 1f - 0.25f * t
                            scaleX = shrink
                            scaleY = shrink
                        }
                    )
                }

                ClayCard(
                    modifier = Modifier
                        .size(118.dp)
                        .graphicsLayer {
                            scaleX = land.value
                            scaleY = land.value
                            alpha = land.value.coerceIn(0f, 1f)
                        },
                    tint = AppColors.Accent,
                    shape = Radius.cardShape
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        BrandArrowIcon(color = AppColors.Background, size = 68.dp)
                    }
                }
            }

            Spacer(Modifier.height(28.dp))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.graphicsLayer {
                    alpha = word.value
                    translationY = 18.dp.toPx() * (1f - word.value)
                }
            ) {
                Text(
                    text = stringResource(R.string.app_name),
                    style = AppText.MenuTitle,
                    color = AppColors.Navy,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.game_subtitle),
                    style = AppText.Body,
                    color = AppColors.Slate,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

