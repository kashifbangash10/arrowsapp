package com.amowpuzzle.aitricker.redesign

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.amowpuzzle.aitricker.ui.components.*
import com.amowpuzzle.aitricker.ui.theme.AppColors
import com.amowpuzzle.aitricker.ui.theme.Radius
import com.amowpuzzle.aitricker.ui.theme.Space

enum class PreviewFixture(val label: String) {
    STRAIGHT("Straight"),
    L_BEND("L-Bend"),
    U_BEND("U-Bend"),
    ZIGZAG("Zigzag"),
    GUITAR("Guitar (L50)"),
    DOG("Dog (L49)"),
    LEVEL_50("Level 50")
}

data class PreviewStats(
    val remaining: Int = 0,
    val total: Int = 0,
    val lives: Int = 3,
    val inFlight: Boolean = false,
    val completed: Boolean = false,
    val message: String = "",
    val busy: Boolean = false
)

@Composable
fun RedesignLabScreen(
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var fixture by remember { mutableStateOf(PreviewFixture.STRAIGHT) }
    var showGrid by remember { mutableStateOf(true) }
    var slowMotion by remember { mutableStateOf(false) }
    var hintRequest by remember { mutableIntStateOf(0) }
    var fitRequest by remember { mutableIntStateOf(0) }
    var resetRequest by remember { mutableIntStateOf(0) }
    var stats by remember { mutableStateOf(PreviewStats()) }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = AppColors.BackgroundTint
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = Space.l, vertical = Space.s)
        ) {
            // Header bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = Space.s),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClose) {
                    CloseIcon(color = AppColors.Navy)
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Motion Study Lab",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.Navy
                    )
                    Text(
                        text = fixture.label,
                        fontSize = 13.sp,
                        color = AppColors.Slate
                    )
                }

                IconButton(onClick = { resetRequest++ }) {
                    RestartIcon(color = AppColors.Navy)
                }
            }

            // Fixture selection chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(vertical = Space.xs),
                horizontalArrangement = Arrangement.spacedBy(Space.s)
            ) {
                PreviewFixture.entries.forEach { item ->
                    val isSelected = item == fixture
                    FilterChip(
                        selected = isSelected,
                        onClick = { fixture = item },
                        label = {
                            Text(
                                text = item.label,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color.White else AppColors.Navy
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AppColors.Accent,
                            containerColor = AppColors.Surface
                        ),
                        shape = Radius.chipShape
                    )
                }
            }

            // Status chip row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = Space.s),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = Radius.chipShape,
                    color = AppColors.Surface,
                    modifier = Modifier.padding(end = Space.s)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = Space.m, vertical = Space.xs),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PointerArrowIcon(color = AppColors.NavySoft)
                        Spacer(Modifier.width(Space.xs))
                        Text(
                            text = "${stats.remaining} / ${stats.total}",
                            fontWeight = FontWeight.SemiBold,
                            color = AppColors.Navy,
                            fontSize = 13.sp
                        )
                    }
                }

                Surface(
                    shape = Radius.chipShape,
                    color = AppColors.Surface
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = Space.m, vertical = Space.xs),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HeartIcon(
                            color = if (stats.lives > 0) AppColors.Heart else AppColors.HeartEmpty,
                            size = 18.dp
                        )
                        Spacer(Modifier.width(Space.xs))
                        Text(
                            text = "${stats.lives} hearts",
                            fontWeight = FontWeight.SemiBold,
                            color = if (stats.lives > 0) AppColors.Navy else AppColors.Blocked,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            // Instruction / status message banner
            if (stats.message.isNotBlank()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = Space.s),
                    shape = RoundedCornerShape(8.dp),
                    color = if (stats.completed) AppColors.ChapterTints[1] else AppColors.SurfaceStrong
                ) {
                    Text(
                        text = stats.message,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        color = AppColors.Navy,
                        modifier = Modifier.padding(horizontal = Space.m, vertical = Space.xs)
                    )
                }
            }

            // Withdrawal board area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(AppColors.Board, Radius.cardShape)
                    .padding(Space.s),
                contentAlignment = Alignment.Center
            ) {
                WithdrawalPreviewBoard(
                    fixture = fixture,
                    showGrid = showGrid,
                    hintRequest = hintRequest,
                    fitRequest = fitRequest,
                    resetRequest = resetRequest,
                    slowMotion = slowMotion,
                    modifier = Modifier.fillMaxSize(),
                    onStats = { stats = it }
                )
            }

            // Bottom control action dock
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Space.s),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Hint
                Button(
                    onClick = { hintRequest++ },
                    shape = Radius.chipShape,
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.Surface, contentColor = AppColors.Navy),
                    contentPadding = PaddingValues(horizontal = Space.m, vertical = Space.xs)
                ) {
                    LightbulbIcon(color = AppColors.Accent, size = 20.dp)
                    Spacer(Modifier.width(Space.xs))
                    Text("Hint", fontSize = 13.sp)
                }

                // Grid toggle
                FilledIconToggleButton(
                    checked = showGrid,
                    onCheckedChange = { showGrid = it },
                    shape = Radius.chipShape,
                    colors = IconButtonDefaults.filledIconToggleButtonColors(
                        containerColor = AppColors.Surface,
                        contentColor = AppColors.Navy,
                        checkedContainerColor = AppColors.AccentSoft,
                        checkedContentColor = AppColors.Accent
                    )
                ) {
                    GridHashIcon(color = if (showGrid) AppColors.Accent else AppColors.Navy, size = 20.dp)
                }

                // Slow-mo toggle
                FilterChip(
                    selected = slowMotion,
                    onClick = { slowMotion = !slowMotion },
                    label = { Text("Slow-mo", fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AppColors.ChapterTints[2],
                        containerColor = AppColors.Surface
                    ),
                    shape = Radius.chipShape
                )

                // Fit screen
                Button(
                    onClick = { fitRequest++ },
                    shape = Radius.chipShape,
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.Surface, contentColor = AppColors.Navy),
                    contentPadding = PaddingValues(horizontal = Space.m, vertical = Space.xs)
                ) {
                    Text("Fit", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
