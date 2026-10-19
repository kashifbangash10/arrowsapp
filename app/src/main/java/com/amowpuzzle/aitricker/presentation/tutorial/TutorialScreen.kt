package com.amowpuzzle.aitricker.presentation.tutorial

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.amowpuzzle.aitricker.R
import com.amowpuzzle.aitricker.domain.model.ArrowPath
import com.amowpuzzle.aitricker.domain.model.Cell
import com.amowpuzzle.aitricker.domain.model.Difficulty
import com.amowpuzzle.aitricker.domain.model.Direction
import com.amowpuzzle.aitricker.domain.model.Level
import com.amowpuzzle.aitricker.ui.components.MiniBoard
import com.amowpuzzle.aitricker.ui.components.PlainTextButton
import com.amowpuzzle.aitricker.ui.components.PrimaryPillButton
import com.amowpuzzle.aitricker.ui.theme.AppColors
import com.amowpuzzle.aitricker.ui.theme.AppText
import kotlinx.coroutines.launch

/**
 * The five-card walkthrough shown once on first run, and any time from the menu.
 *
 * Each card illustrates its point with a real board drawn by the same code that draws
 * the game, so the tutorial cannot drift out of step with how the game actually looks.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TutorialScreen(onFinish: () -> Unit, modifier: Modifier = Modifier) {
    val pages = remember { tutorialPages() }
    val pager = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()
    val last = pager.currentPage == pages.lastIndex

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.BackgroundTint)
            .safeDrawingPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.End
        ) {
            if (!last) {
                PlainTextButton(
                    text = stringResource(R.string.tutorial_skip),
                    onClick = onFinish,
                    color = AppColors.Slate,
                    style = AppText.Body
                )
            }
        }

        HorizontalPager(
            state = pager,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) { index ->
            TutorialCard(page = pages[index])
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            pages.indices.forEach { index ->
                val current = index == pager.currentPage
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .size(if (current) 10.dp else 7.dp)
                        .clip(CircleShape)
                        .background(if (current) AppColors.Accent else AppColors.SurfaceStrong)
                )
            }
        }

        Box(modifier = Modifier.padding(horizontal = 30.dp)) {
            PrimaryPillButton(
                text = if (last) {
                    stringResource(R.string.tutorial_start)
                } else {
                    stringResource(R.string.tutorial_next)
                },
                onClick = {
                    if (last) {
                        onFinish()
                    } else {
                        scope.launch { pager.animateScrollToPage(pager.currentPage + 1) }
                    }
                }
            )
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun TutorialCard(page: TutorialPage) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 30.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.78f)
                .aspectRatio(1f)
                .clip(RoundedCornerShape(30.dp))
                .background(AppColors.Background)
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            MiniBoard(
                level = page.level,
                modifier = Modifier.fillMaxSize(),
                highlighted = page.highlighted,
                highlightColor = page.highlightColor
            )
        }
        Spacer(Modifier.height(28.dp))
        Text(
            text = stringResource(page.titleRes),
            style = AppText.CardTitle,
            color = AppColors.Navy,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = stringResource(page.bodyRes),
            style = AppText.Body,
            color = AppColors.Slate,
            textAlign = TextAlign.Center
        )
    }
}

private class TutorialPage(
    val titleRes: Int,
    val bodyRes: Int,
    val level: Level,
    val highlighted: Set<Int> = emptySet(),
    val highlightColor: Color = AppColors.HintArrow
)

/** Hand-built five-by-five boards, each arranged to make exactly one point. */
private fun tutorialPages(): List<TutorialPage> {
    fun board(vararg arrows: ArrowPath) = Level(
        id = 0,
        cols = 5,
        rows = 5,
        difficulty = Difficulty.EASY,
        arrows = arrows.toList()
    )

    fun straight(id: Int, from: Cell, to: Cell, exit: Direction) =
        ArrowPath(id, listOf(from, to), exit)

    return listOf(
        TutorialPage(
            titleRes = R.string.tutorial_1_title,
            bodyRes = R.string.tutorial_1_body,
            level = board(
                straight(1, Cell(1, 1), Cell(2, 1), Direction.RIGHT),
                ArrowPath(2, listOf(Cell(1, 3), Cell(2, 3), Cell(2, 2)), Direction.UP)
            )
        ),
        TutorialPage(
            titleRes = R.string.tutorial_2_title,
            bodyRes = R.string.tutorial_2_body,
            level = board(
                straight(1, Cell(0, 2), Cell(1, 2), Direction.RIGHT),
                straight(2, Cell(3, 1), Cell(3, 2), Direction.DOWN)
            ),
            highlighted = setOf(2)
        ),
        TutorialPage(
            titleRes = R.string.tutorial_3_title,
            bodyRes = R.string.tutorial_3_body,
            level = board(
                straight(1, Cell(1, 2), Cell(2, 2), Direction.RIGHT),
                straight(2, Cell(4, 1), Cell(4, 2), Direction.DOWN)
            ),
            highlighted = setOf(1),
            highlightColor = AppColors.Blocked
        ),
        TutorialPage(
            titleRes = R.string.tutorial_4_title,
            bodyRes = R.string.tutorial_4_body,
            level = board(
                straight(1, Cell(1, 1), Cell(1, 2), Direction.DOWN),
                straight(2, Cell(3, 3), Cell(2, 3), Direction.LEFT),
                straight(3, Cell(3, 0), Cell(3, 1), Direction.DOWN)
            ),
            highlighted = setOf(1)
        ),
        TutorialPage(
            titleRes = R.string.tutorial_5_title,
            bodyRes = R.string.tutorial_5_body,
            level = board(straight(1, Cell(2, 3), Cell(2, 2), Direction.UP))
        )
    )
}
