package com.amowpuzzle.aitricker.presentation.complete

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.amowpuzzle.aitricker.R
import com.amowpuzzle.aitricker.data.level.LevelCatalog
import com.amowpuzzle.aitricker.domain.model.Level
import com.amowpuzzle.aitricker.ui.components.ChevronLeftIcon
import com.amowpuzzle.aitricker.ui.components.MiniBoard
import com.amowpuzzle.aitricker.ui.theme.AppColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/**
 * Celebration Screen matching Image 2 reference:
 * - Radiant celebratory sky-blue gradient background
 * - Spinning sunburst rays
 * - Colorful celebratory confetti tumbling across the screen
 * - "Level Completed!" clean bold white title
 * - Pure white rounded center card with thumbnail preview of cleared level
 * - Big rounded white pill button: "Next Game" (bold blue) + "Level X" (soft blue)
 * - "Main" navigation action with icon below
 */
@Composable
fun LevelCompleteScreen(
    levelId: Int,
    moves: Int,
    livesLeft: Int,
    onNext: () -> Unit,
    onReplay: () -> Unit,
    onMenu: () -> Unit,
    modifier: Modifier = Modifier
) {
    val level by produceState<Level?>(initialValue = null, levelId) {
        value = withContext(Dispatchers.Default) { LevelCatalog.level(levelId) }
    }
    val hasNext = levelId < LevelCatalog.TOTAL_LEVELS
    val confetti = remember(levelId) { confettiFor(levelId) }
    val transition = rememberInfiniteTransition(label = "celebration")

    // Sunburst rotation
    val spin by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(32_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "spin"
    )

    // Confetti falling
    val fall by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(5200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "fall"
    )

    // Gentle pulse animation for the "Next Game" button
    val buttonPulse by transition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.025f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "buttonPulse"
    )

    // Entrance animation states
    var appeared by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        appeared = true
    }

    val cardScale by animateFloatAsState(
        targetValue = if (appeared) 1f else 0.85f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = Spring.StiffnessMediumLow),
        label = "cardScale"
    )

    val contentAlpha by animateFloatAsState(
        targetValue = if (appeared) 1f else 0f,
        animationSpec = tween(400, easing = FastOutSlowInEasing),
        label = "contentAlpha"
    )

    val bgGradient = remember {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF29B6F6),
                Color(0xFF1E88E5),
                Color(0xFF1565C0)
            )
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bgGradient)
    ) {
        // Rotating sunburst rays behind card and buttons
        Canvas(modifier = Modifier.fillMaxSize()) { drawSunburst(spin) }

        // Continuous falling colorful celebratory confetti
        Canvas(modifier = Modifier.fillMaxSize()) { drawConfetti(confetti, fall) }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(36.dp))

            // ── TITLE: "Level Completed!" ──────────────────────────────────
            Text(
                text = "Level Completed!",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.graphicsLayer {
                    alpha = contentAlpha
                    translationY = (1f - contentAlpha) * -20f
                }
            )

            Spacer(Modifier.weight(1f))

            // ── CENTER CARD: White rounded card with completed level preview ──
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.78f)
                    .aspectRatio(1f)
                    .graphicsLayer {
                        scaleX = cardScale
                        scaleY = cardScale
                        alpha = contentAlpha
                    }
                    .shadow(
                        elevation = 16.dp,
                        shape = RoundedCornerShape(28.dp),
                        spotColor = Color(0x33000000),
                        ambientColor = Color(0x1F000000)
                    )
                    .clip(RoundedCornerShape(28.dp))
                    .background(Color.White)
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                if (level != null) {
                    MiniBoard(
                        level = level!!,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Spacer(Modifier.weight(1.2f))

            // ── ACTION BUTTON: "Next Game" pill button ──────────────────────
            Surface(
                onClick = if (hasNext) onNext else onReplay,
                shape = RoundedCornerShape(32.dp),
                color = Color.White,
                shadowElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth(0.86f)
                    .height(64.dp)
                    .graphicsLayer {
                        scaleX = buttonPulse
                        scaleY = buttonPulse
                        alpha = contentAlpha
                    }
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = if (hasNext) "Next Game" else "Replay Game",
                        color = Color(0xFF1E88E5),
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.2.sp
                    )
                    Text(
                        text = if (hasNext) "Level ${levelId + 1}" else "Level $levelId",
                        color = Color(0xFF64B5F6),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // ── SECONDARY ACTION: "Main" button with icon ──────────────────
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onMenu)
                    .padding(horizontal = 20.dp, vertical = 10.dp)
                    .graphicsLayer { alpha = contentAlpha },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                ChevronLeftIcon(color = Color.White, size = 18.dp)
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "Main",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.2.sp
                )
            }

            Spacer(Modifier.height(18.dp))
        }
    }
}

/**
 * The rays radiating from upper-center behind the card.
 */
private fun DrawScope.drawSunburst(rotation: Float) {
    val reach = size.width + size.height
    val centreX = size.width / 2f
    val centreY = size.height * 0.38f
    val topLeft = Offset(centreX - reach, centreY - reach)
    val arcSize = Size(reach * 2f, reach * 2f)
    for (i in 0 until 14) {
        drawArc(
            color = Color.White,
            startAngle = rotation + i * (360f / 14f),
            sweepAngle = 12f,
            useCenter = true,
            topLeft = topLeft,
            size = arcSize,
            alpha = 0.09f
        )
    }
}

/**
 * Falling confetti pieces with natural flutter and 3D rotation.
 */
private fun DrawScope.drawConfetti(pieces: List<ConfettiPiece>, progress: Float) {
    val travel = size.height + 160f
    for (piece in pieces) {
        val t = (progress + piece.phase) % 1f
        val y = t * travel - 80f
        val sway = sin((t * 2.2f + piece.phase) * 2f * PI.toFloat()) * piece.drift * size.width
        val x = piece.x * size.width + sway
        val extent = piece.size * size.width
        val alpha = if (t > 0.88f) ((1f - t) / 0.12f).coerceIn(0f, 1f) else 1f

        if (piece.round) {
            drawCircle(
                color = piece.color,
                radius = extent * 0.5f,
                center = Offset(x, y),
                alpha = alpha
            )
        } else {
            rotate(degrees = t * 360f * piece.spin, pivot = Offset(x, y)) {
                drawRoundRect(
                    color = piece.color,
                    topLeft = Offset(x - extent * 0.5f, y - extent * 0.32f),
                    size = Size(extent, extent * 0.64f),
                    cornerRadius = CornerRadius(extent * 0.2f, extent * 0.2f),
                    alpha = alpha
                )
            }
        }
    }
}

/**
 * Generates celebratory confetti layout in bright festive colors matching Image 2.
 */
private fun confettiFor(levelId: Int): List<ConfettiPiece> {
    val random = Random(levelId * 7919 + 13)
    val palette = listOf(
        Color(0xFFFFEB3B), // Yellow
        Color(0xFFFF4081), // Pink
        Color(0xFF00E5FF), // Cyan
        Color(0xFF76FF03), // Lime
        Color(0xFFFF9100), // Orange
        Color.White,       // White
        Color(0xFFE040FB), // Magenta
        Color(0xFF69F0AE), // Mint
        Color(0xFFFFD54F)  // Amber
    )
    return List(48) {
        ConfettiPiece(
            x = random.nextFloat(),
            size = 0.016f + random.nextFloat() * 0.024f,
            color = palette[random.nextInt(palette.size)],
            phase = random.nextFloat(),
            drift = 0.012f + random.nextFloat() * 0.055f,
            spin = 0.7f + random.nextFloat() * 2.0f,
            round = random.nextFloat() < 0.28f
        )
    }
}

private class ConfettiPiece(
    val x: Float,
    val size: Float,
    val color: Color,
    val phase: Float,
    val drift: Float,
    val spin: Float,
    val round: Boolean
)
