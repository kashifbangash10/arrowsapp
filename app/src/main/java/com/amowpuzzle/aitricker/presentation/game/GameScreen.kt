package com.amowpuzzle.aitricker.presentation.game

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.amowpuzzle.aitricker.BuildConfig
import com.amowpuzzle.aitricker.R
import com.amowpuzzle.aitricker.domain.model.ArrowPath
import com.amowpuzzle.aitricker.ui.components.ArrowBoard
import com.amowpuzzle.aitricker.ui.components.ChevronLeftIcon
import com.amowpuzzle.aitricker.ui.components.ClayBand
import com.amowpuzzle.aitricker.ui.components.ClayCard
import com.amowpuzzle.aitricker.ui.components.ClayCircleButton
import com.amowpuzzle.aitricker.ui.components.GridHashIcon
import com.amowpuzzle.aitricker.ui.components.HeartIcon
import com.amowpuzzle.aitricker.ui.components.HeartsRow
import com.amowpuzzle.aitricker.ui.components.LightbulbIcon
import com.amowpuzzle.aitricker.ui.components.Pill
import com.amowpuzzle.aitricker.ui.components.PlainTextButton
import com.amowpuzzle.aitricker.ui.components.PointerArrowIcon
import com.amowpuzzle.aitricker.ui.components.PrimaryPillButton
import com.amowpuzzle.aitricker.ui.theme.AppColors
import com.amowpuzzle.aitricker.ui.theme.AppText
import com.amowpuzzle.aitricker.ui.theme.Radius
import com.amowpuzzle.aitricker.ui.theme.Space

/**
 * The play screen. It renders [state] and forwards taps; it holds no game state of
 * its own, which is why a configuration change or a recomposition mid-animation
 * cannot desynchronise it from the view model.
 *
 * Layout: floating clay header card (level + hearts) above the board, then a
 * chapter-tinted clay dock below with hint and grid buttons.  Both panels use the
 * chapter-colour of the current level, so the UI shifts hue as the player climbs.
 */
@Composable
fun GameScreen(
    state: GameUiState,
    showGrid: Boolean,
    boardDark: Boolean = true,
    onTap: (ArrowPath) -> Unit,
    onDepartureFinished: (Long) -> Unit,
    onHint: () -> Unit,
    onToggleGrid: () -> Unit,
    onRetry: () -> Unit,
    onBack: () -> Unit,
    onSettings: () -> Unit,
    onSolved: (levelId: Int, moves: Int, lives: Int) -> Unit,
    onPlaySolution: () -> Unit,
    modifier: Modifier = Modifier
) {
    val level = state.level
    val isLevel1 = state.levelId == 1
    val darkBoard = if (isLevel1) false else boardDark
    val boardBg = if (isLevel1) Color.White else if (darkBoard) Color(0xFF101318) else AppColors.Board
    val chapterTint = AppColors.chapterTint(state.levelId.coerceAtLeast(1))
    val chapterInk = AppColors.chapterInk(state.levelId.coerceAtLeast(1))
    val tierAccent = level?.difficulty?.let(AppColors::difficultyAccent) ?: AppColors.StarGold
    val headerFace = if (darkBoard) lerp(Color(0xFF141D31), tierAccent, 0.16f)
        else lerp(Color.White, tierAccent, 0.55f)
    val controlInk = if (darkBoard) tierAccent else AppColors.Navy

    LaunchedEffect(state.status, state.levelId) {
        if (state.status == GameStatus.VICTORY && state.levelId > 0) {
            onSolved(state.levelId, state.movesMade, state.lives)
        }
    }

    Box(modifier = modifier.fillMaxSize().background(boardBg)) {

        // ── BOARD (fills everything; header+dock float above) ─────────────────
        if (level != null) {
            ArrowBoard(
                level = level,
                activeArrows = state.activeArrows,
                departing = state.departing,
                blocked = state.blocked,
                hintArrowId = state.hintArrowId,
                showGrid = if (isLevel1) false else showGrid,
                enabled = state.acceptsInput,
                boardDark = darkBoard,
                onArrowTap = onTap,
                onDepartureFinished = onDepartureFinished,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        top = if (isLevel1) 72.dp else 112.dp,
                        bottom = if (isLevel1) 24.dp else 100.dp
                    ),
                contentDescription = stringResource(
                    R.string.board_description,
                    state.remainingArrows
                )
            )
        }

        // ── HEADER CARD (minimal on Level 1 matching Image 1; clay card on later levels) ──
        if (isLevel1) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(top = 18.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Level 1",
                    fontSize = 24.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    color = Color(0xFF111827),
                    textAlign = TextAlign.Center
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
            ) {
                ClayBand(
                    tint = headerFace,
                    shape = Radius.panelShape,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Space.m, vertical = Space.s)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Space.l, vertical = Space.m),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Back button
                        ClayCircleButton(
                            onClick = onBack,
                            contentDescription = stringResource(R.string.back),
                            size = 38.dp,
                            face = if (darkBoard) Color(0xFF2A3040) else AppColors.Background
                        ) { _ -> ChevronLeftIcon(color = controlInk, size = 20.dp) }

                        // Level number + difficulty
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = stringResource(R.string.level_number, state.levelId.coerceAtLeast(1)),
                                style = AppText.CardTitle,
                                color = if (darkBoard) tierAccent else chapterInk
                            )
                            if (level != null) {
                                Text(
                                    text = level.difficulty.label,
                                    style = AppText.Caption,
                                    color = if (darkBoard) tierAccent.copy(alpha = 0.85f) else chapterInk.copy(alpha = 0.8f)
                                )
                            }
                        }

                        // Hearts
                        if (level != null) {
                            val livesLabel = stringResource(R.string.lives_remaining, state.lives, level.startingLives)
                            Box(modifier = Modifier.clearAndSetSemantics { contentDescription = livesLabel }) {
                                HeartsRow(lives = state.lives, total = level.startingLives, heartSize = 22.dp)
                            }
                        } else {
                            Spacer(Modifier.width(80.dp))
                        }
                    }
                }
            }
        }

        // ── DOCK (floating bottom clay bar, shown on Level 2+) ────────────────
        if (level != null && !isLevel1) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
            ) {
                ClayBand(
                    tint = headerFace,
                    shape = Radius.panelShape,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Space.m, vertical = Space.s)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Space.xl, vertical = Space.s),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Arrows remaining pill
                        Pill(
                            text = state.remainingArrows.toString(),
                            container = if (darkBoard) Color(0xFF2A3040) else AppColors.Background,
                            contentColor = if (darkBoard) Color.White else AppColors.Navy,
                            contentDescription = stringResource(R.string.arrows_remaining, state.remainingArrows),
                            leading = { PointerArrowIcon(color = AppColors.Accent, size = 14.dp) }
                        )

                        // Hint button
                        ClayCircleButton(
                            onClick = onHint,
                            contentDescription = if (state.hintsRemaining > 0) {
                                stringResource(R.string.hint_button, state.hintsRemaining)
                            } else {
                                stringResource(R.string.hint_button_empty)
                            },
                            active = state.hintArrowId != null,
                            enabled = state.canUseHint,
                            badge = state.hintsRemaining,
                            face = if (darkBoard) Color(0xFF2A3040) else AppColors.Background
                        ) { _ -> LightbulbIcon(color = controlInk, size = 28.dp) }

                        // Grid toggle
                        ClayCircleButton(
                            onClick = onToggleGrid,
                            contentDescription = if (showGrid) {
                                stringResource(R.string.grid_hide)
                            } else {
                                stringResource(R.string.grid_show)
                            },
                            active = showGrid,
                            face = if (darkBoard) Color(0xFF2A3040) else AppColors.Background
                        ) { _ -> GridHashIcon(color = controlInk, size = 26.dp) }

                        // Debug solution button (release builds compile this out)
                        if (BuildConfig.SHOW_DEBUG_TOOLS) {
                            ClayCircleButton(
                                onClick = onPlaySolution,
                                contentDescription = "Play solution",
                                size = 44.dp,
                                face = if (darkBoard) Color(0xFF2A3040) else AppColors.Background
                            ) { _ -> PointerArrowIcon(color = controlInk, size = 20.dp) }
                        }
                    }
                }
            }
        }

        // ── LOADING PLACEHOLDER ────────────────────────────────────────────────
        if (level == null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.app_name),
                    style = AppText.Body,
                    color = AppColors.SlateMuted
                )
            }
        }

        // ── GAME OVER OVERLAY ──────────────────────────────────────────────────
        if (state.status == GameStatus.GAME_OVER) {
            GameOverOverlay(onRetry = onRetry, onMenu = onBack)
        }
    }
}

/**
 * Out of hearts. Deliberately not a dead end: the level restarts with a full set and
 * nothing that was already unlocked is taken away.
 */
@Composable
private fun GameOverOverlay(onRetry: () -> Unit, onMenu: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Navy.copy(alpha = 0.58f))
            .padding(horizontal = 30.dp),
        contentAlignment = Alignment.Center
    ) {
        ClayCard(
            modifier = Modifier.fillMaxWidth(),
            tint = AppColors.Background,
            shape = Radius.sheetShape
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 26.dp, vertical = 30.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(AppColors.Blocked.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    HeartIcon(color = AppColors.Blocked, size = 38.dp)
                }
                Spacer(Modifier.height(18.dp))
                Text(
                    text = stringResource(R.string.game_over_title),
                    style = AppText.CardTitle,
                    color = AppColors.Navy
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = stringResource(R.string.game_over_body),
                    style = AppText.Body,
                    color = AppColors.Slate,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(24.dp))
                PrimaryPillButton(
                    text = stringResource(R.string.game_over_retry),
                    onClick = onRetry
                )
                Spacer(Modifier.height(4.dp))
                PlainTextButton(
                    text = stringResource(R.string.game_over_menu),
                    onClick = onMenu,
                    color = AppColors.Slate,
                    style = AppText.Body
                )
            }
        }
    }
}
