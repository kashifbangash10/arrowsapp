package com.amowpuzzle.aitricker.presentation.levels

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.amowpuzzle.aitricker.R
import com.amowpuzzle.aitricker.data.level.AdvancedCampaignLevels
import com.amowpuzzle.aitricker.data.level.ApexCampaignLevels
import com.amowpuzzle.aitricker.data.level.CampaignChapters
import com.amowpuzzle.aitricker.data.level.ExpertExpansionLevels
import com.amowpuzzle.aitricker.data.level.GrandmasterCampaignLevels
import com.amowpuzzle.aitricker.data.level.LevelCatalog
import com.amowpuzzle.aitricker.data.level.MasterCampaignLevels
import com.amowpuzzle.aitricker.data.prefs.GamePreferences
import com.amowpuzzle.aitricker.ui.components.CheckIcon
import com.amowpuzzle.aitricker.ui.components.ChevronLeftIcon
import kotlin.math.sqrt

private val RoadGold = Color(0xFFFFD36D)
private val RoadWhite = Color(0xFFF3F5FC)
private val RoadMuted = Color(0xFF9EAED0)
private val RoadNavy = Color(0xFF101B36)
private val RoadCardShape = RoundedCornerShape(22.dp)

private data class RoadmapBand(val label: String, val range: IntRange)

private val roadmapBands = listOf(
    RoadmapBand("ALL", 1..LevelCatalog.TOTAL_LEVELS),
    RoadmapBand("ORIGINS", 1..100),
    RoadmapBand("MASTER", 101..250),
    RoadmapBand("GRANDMASTER", 251..500),
    RoadmapBand("LEGEND", 501..750),
    RoadmapBand("EXTREME", 751..1000),
    RoadmapBand("APEX", 1001..1500),
)

/** Lightweight chapter metadata only: scrolling never generates a puzzle board. */
@Composable
fun LevelSelectScreen(
    snapshot: GamePreferences.Snapshot,
    onLevel: (Int) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedChapter by rememberSaveable { mutableStateOf<Int?>(null) }
    var selectedBand by rememberSaveable { mutableStateOf(0) }
    val resume = snapshot.resumeLevel
    val chapterIndex = selectedChapter?.takeIf { it in CampaignChapters.all.indices }

    BackHandler(enabled = chapterIndex != null) { selectedChapter = null }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF22355D), RoadNavy, Color(0xFF080E20))))
            .drawBehind {
                drawRect(
                    brush = Brush.radialGradient(
                        listOf(Color(0xFF7186B1).copy(alpha = 0.13f), Color.Transparent),
                        center = Offset(size.width * 0.85f, size.height * 0.12f),
                        radius = size.width * 0.9f
                    )
                )
                val horizon = size.height * 0.46f
                val grid = Color(0xFF7A94C7).copy(alpha = 0.08f)
                for (ray in -5..5) {
                    drawLine(
                        grid,
                        Offset(size.width * 0.5f + ray * size.width * 0.025f, horizon),
                        Offset(size.width * 0.5f + ray * size.width * 0.25f, size.height),
                        1.dp.toPx()
                    )
                }
                for (line in 1..9) {
                    val fraction = line / 9f
                    val y = horizon + (size.height - horizon) * fraction * fraction
                    drawLine(grid, Offset(0f, y), Offset(size.width, y), 1.dp.toPx())
                }
                for (star in 0..18) {
                    drawCircle(
                        RoadWhite.copy(alpha = 0.1f),
                        radius = if (star % 4 == 0) 1.4.dp.toPx() else 0.8.dp.toPx(),
                        center = Offset(size.width * ((star * 37 % 97) / 97f), size.height * ((star * 23 % 89) / 100f))
                    )
                }
            }
    ) {
        Column(Modifier.fillMaxSize().safeDrawingPadding()) {
            RoadmapHeader(
                title = if (chapterIndex == null) "Roadmap" else "Chapter ${chapterIndex + 1}",
                subtitle = if (chapterIndex == null) {
                    "${snapshot.completed.size} / ${LevelCatalog.TOTAL_LEVELS} cleared"
                } else {
                    val chapter = CampaignChapters.all[chapterIndex]
                    "Levels ${chapter.levels.first}\u2013${chapter.levels.last}"
                },
                onBack = { if (chapterIndex == null) onBack() else selectedChapter = null }
            )
            if (chapterIndex == null) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                ) {
                    items(roadmapBands.indices.toList(), key = { it }) { index ->
                        val band = roadmapBands[index]
                        val selected = selectedBand == index
                        Column(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (selected) RoadGold.copy(alpha = 0.13f) else RoadNavy.copy(alpha = 0.7f))
                                .border(1.dp, if (selected) RoadGold.copy(alpha = 0.85f) else RoadWhite.copy(alpha = 0.1f), RoundedCornerShape(14.dp))
                                .clickable { selectedBand = index }
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(band.label, color = if (selected) RoadGold else RoadWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp)
                            Text("${band.range.first}\u2013${band.range.last}", color = RoadMuted, fontSize = 10.sp, modifier = Modifier.padding(top = 3.dp))
                        }
                    }
                }
                val chapters = remember(selectedBand) {
                    CampaignChapters.all.indices.filter { CampaignChapters.all[it].levels.first in roadmapBands[selectedBand].range }
                }
                key(selectedBand) {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 28.dp)
                    ) {
                        item {
                            RoadmapHero(
                                title = if (selectedBand == 0) "${LevelCatalog.TOTAL_LEVELS} LEVELS" else roadmapBands[selectedBand].label,
                                subtitle = "Choose your next challenge",
                                steps = roadmapWindow(chapters, CampaignChapters.indexOf(resume)),
                                currentStep = CampaignChapters.indexOf(resume),
                                completed = { index -> CampaignChapters.all[index].levels.all(snapshot::isCompleted) },
                                onStep = { selectedChapter = it }
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 22.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(RoadGold.copy(alpha = 0.1f))
                                    .border(1.dp, RoadGold.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
                                    .clickable { onLevel(resume) }
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text("CONTINUE YOUR JOURNEY", color = RoadGold, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                                    Text("Level $resume", color = RoadWhite, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                                }
                                Text("PLAY  \u2192", color = RoadGold, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        items((chapters.size + 1) / 2, key = { row -> "chapter_${chapters[row * 2]}" }) { row ->
                            val indices = chapters.drop(row * 2).take(2)
                            RoadmapRow(
                                ids = indices,
                                isCurrent = { resume in CampaignChapters.all[it].levels },
                                isCompleted = { CampaignChapters.all[it].levels.all(snapshot::isCompleted) }
                            ) { index, cardModifier ->
                                val chapter = CampaignChapters.all[index]
                                val cleared = chapter.levels.count(snapshot::isCompleted)
                                RoadmapCard(
                                    number = (index + 1).toString().padStart(2, '0'),
                                    title = chapter.name.uppercase(),
                                    subtitle = "LEVELS ${chapter.levels.first}\u2013${chapter.levels.last}",
                                    footer = "$cleared / ${chapter.count} CLEARED",
                                    isCurrent = resume in chapter.levels,
                                    isCompleted = cleared == chapter.count,
                                    artSeed = index,
                                    description = "Chapter ${index + 1}, ${chapter.name}, levels ${chapter.levels.first} to ${chapter.levels.last}, $cleared cleared",
                                    onClick = { selectedChapter = index },
                                    modifier = cardModifier
                                )
                            }
                        }
                    }
                }
            } else {
                val chapter = CampaignChapters.all[chapterIndex]
                val levelIds = remember(chapterIndex) { chapter.levels.toList() }
                key(chapterIndex) {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 28.dp)
                    ) {
                        item {
                            RoadmapHero(
                                title = chapter.name.uppercase(),
                                subtitle = "${chapter.count} puzzles \u00b7 ${chapter.levels.count(snapshot::isCompleted)} cleared",
                                steps = roadmapWindow(levelIds, resume),
                                currentStep = resume,
                                completed = snapshot::isCompleted,
                                onStep = onLevel,
                                stepOffset = 0
                            )
                        }
                        items((levelIds.size + 1) / 2, key = { row -> "level_${levelIds[row * 2]}" }) { row ->
                            val ids = levelIds.drop(row * 2).take(2)
                            RoadmapRow(ids = ids, isCurrent = { it == resume }, isCompleted = snapshot::isCompleted) { id, cardModifier ->
                                val completed = snapshot.isCompleted(id)
                                RoadmapCard(
                                    number = id.toString().padStart(2, '0'),
                                    title = levelName(id)?.uppercase() ?: "LEVEL $id",
                                    subtitle = LevelCatalog.difficultyOf(id).label.uppercase(),
                                    footer = when {
                                        completed -> "\u2605  CLEARED"
                                        id == resume -> "CONTINUE  \u2192"
                                        else -> "PLAY  \u2192"
                                    },
                                    isCurrent = id == resume,
                                    isCompleted = completed,
                                    artSeed = id,
                                    description = "Level $id, ${LevelCatalog.difficultyOf(id).label}${levelName(id)?.let { ", $it" }.orEmpty()}, ${if (completed) "completed" else "ready to play"}",
                                    onClick = { onLevel(id) },
                                    modifier = cardModifier
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun roadmapWindow(ids: List<Int>, current: Int): List<Int> {
    val start = (ids.indexOf(current) - 1).coerceIn(0, (ids.size - 5).coerceAtLeast(0))
    return ids.drop(start).take(5)
}

private fun levelName(id: Int): String? =
    ApexCampaignLevels.name(id) ?: AdvancedCampaignLevels.name(id) ?: GrandmasterCampaignLevels.name(id) ?: MasterCampaignLevels.name(id) ?: ExpertExpansionLevels.name(id)

@Composable
private fun RoadmapHeader(title: String, subtitle: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val backLabel = stringResource(R.string.back)
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(RoadNavy.copy(alpha = 0.8f))
                .border(1.dp, RoadWhite.copy(alpha = 0.13f), CircleShape)
                .semantics { contentDescription = backLabel }
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center
        ) { ChevronLeftIcon(color = RoadWhite, size = 18.dp) }
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(title, color = RoadWhite, fontSize = 23.sp, fontWeight = FontWeight.ExtraBold)
            Text(subtitle, color = RoadMuted, fontSize = 11.sp, modifier = Modifier.padding(top = 2.dp))
        }
        Text(
            "ALL OPEN",
            color = RoadGold,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.7.sp,
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(RoadGold.copy(alpha = 0.1f))
                .border(1.dp, RoadGold.copy(alpha = 0.3f), RoundedCornerShape(50))
                .padding(horizontal = 10.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun RoadmapHero(
    title: String,
    subtitle: String,
    steps: List<Int>,
    currentStep: Int,
    completed: (Int) -> Boolean,
    onStep: (Int) -> Unit,
    stepOffset: Int = 1
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .widthIn(max = 340.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(50))
                .background(Brush.horizontalGradient(listOf(Color(0xFF283D66), RoadNavy)))
                .border(1.dp, RoadWhite.copy(alpha = 0.12f), RoundedCornerShape(50))
                .padding(start = 14.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("ROADMAP", color = RoadGold, fontSize = 10.sp, letterSpacing = 1.3.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f).padding(end = 4.dp))
            steps.forEach { step ->
                val done = completed(step)
                val current = step == currentStep
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(if (done) RoadGold else RoadNavy.copy(alpha = 0.8f))
                        .border(if (current) 1.5.dp else 1.dp, if (current) RoadGold else RoadWhite.copy(alpha = 0.12f), CircleShape)
                        .semantics { contentDescription = "${if (stepOffset == 0) "Level" else "Chapter"} ${step + stepOffset}${if (done) ", completed" else ""}" }
                        .clickable { onStep(step) },
                    contentAlignment = Alignment.Center
                ) {
                    if (done) CheckIcon(color = RoadNavy, size = 13.dp)
                    else Text("${step + stepOffset}", color = if (current) RoadGold else RoadWhite, fontWeight = FontWeight.Bold, fontSize = if (step + stepOffset >= 100) 10.sp else 12.sp)
                }
            }
        }
        Text(
            title,
            color = RoadWhite,
            fontSize = if (title.length > 16) 27.sp else 32.sp,
            lineHeight = 35.sp,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center,
            letterSpacing = 0.8.sp,
            modifier = Modifier.padding(top = 20.dp)
        )
        Box(
            Modifier.padding(top = 10.dp, bottom = 12.dp).width(108.dp).height(4.dp)
                .shadow(5.dp, RoundedCornerShape(50), spotColor = RoadGold)
                .background(Brush.horizontalGradient(listOf(Color(0xFFE6AF49), RoadGold)), RoundedCornerShape(50))
        )
        Text(subtitle, color = RoadMuted, fontSize = 12.sp, textAlign = TextAlign.Center)
    }
}

@Composable
private fun RoadmapRow(
    ids: List<Int>,
    isCurrent: (Int) -> Boolean,
    isCompleted: (Int) -> Boolean,
    card: @Composable (Int, Modifier) -> Unit
) {
    Column(Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            ids.forEachIndexed { position, id ->
                Column(Modifier.weight(1f)) {
                    Spacer(Modifier.height(if (position % 2 == 1) 24.dp else 0.dp))
                    card(id, Modifier.fillMaxWidth())
                    Spacer(Modifier.height(if (position % 2 == 0) 24.dp else 0.dp))
                }
            }
            if (ids.size == 1) Spacer(Modifier.weight(1f))
        }
        Canvas(Modifier.fillMaxWidth().height(44.dp)) {
            val gap = 14.dp.toPx()
            val cardWidth = (size.width - gap) / 2f
            val centers = ids.indices.map { cardWidth / 2f + it * (cardWidth + gap) }
            val timelineY = size.height * 0.72f
            drawLine(RoadMuted.copy(alpha = 0.22f), Offset(0f, timelineY), Offset(size.width, timelineY), 3.dp.toPx(), StrokeCap.Round)
            ids.forEachIndexed { index, id ->
                val active = isCurrent(id) || isCompleted(id)
                val center = Offset(centers[index], timelineY)
                drawLine(
                    RoadMuted.copy(alpha = 0.35f), Offset(center.x, 0f), Offset(center.x, timelineY - 9.dp.toPx()),
                    strokeWidth = 1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 4.dp.toPx()))
                )
                if (active) {
                    drawCircle(Brush.radialGradient(listOf(RoadGold.copy(alpha = 0.4f), Color.Transparent), center, 22.dp.toPx()), 22.dp.toPx(), center)
                }
                drawCircle(if (active) RoadGold else RoadNavy, 8.dp.toPx(), center)
                drawCircle(if (active) Color(0xFFFFE7A8) else RoadMuted.copy(alpha = 0.7f), 8.dp.toPx(), center, style = Stroke(2.dp.toPx()))
            }
            if (ids.size == 2 && ids.all(isCompleted)) {
                drawLine(RoadGold.copy(alpha = 0.85f), Offset(centers[0], timelineY), Offset(centers[1], timelineY), 3.dp.toPx(), StrokeCap.Round)
            }
        }
    }
}

@Composable
private fun RoadmapCard(
    number: String,
    title: String,
    subtitle: String,
    footer: String,
    isCurrent: Boolean,
    isCompleted: Boolean,
    artSeed: Int,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val emphasized = isCurrent || isCompleted
    // Keep both cards aligned while reserving room for the user's larger text setting.
    val cardHeight = (238f + 130f * (LocalDensity.current.fontScale - 1f).coerceAtLeast(0f)).dp
    Column(
        modifier = modifier
            .height(cardHeight)
            .shadow(if (isCurrent) 16.dp else 5.dp, RoadCardShape, spotColor = if (isCurrent) RoadGold else Color.Black, ambientColor = if (isCurrent) RoadGold.copy(alpha = 0.35f) else Color.Black)
            .clip(RoadCardShape)
            .background(Brush.linearGradient(listOf(if (isCurrent) Color(0xFF344363) else Color(0xFF1B2B4C), Color(0xFF111B35), Color(0xFF0B1329))))
            .drawBehind {
                val shine = Path().apply {
                    moveTo(0f, 0f)
                    lineTo(size.width * 0.32f, 0f)
                    lineTo(0f, size.height * 0.72f)
                    close()
                }
                drawPath(shine, RoadWhite.copy(alpha = if (isCurrent) 0.08f else 0.025f))
            }
            .border(if (isCurrent) 2.dp else 1.dp, when { isCurrent -> RoadGold; isCompleted -> RoadGold.copy(alpha = 0.5f); else -> RoadWhite.copy(alpha = 0.12f) }, RoadCardShape)
            .semantics(mergeDescendants = true) { contentDescription = description }
            .clickable(onClick = onClick)
            .padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(number, color = RoadGold.copy(alpha = if (emphasized) 1f else 0.75f), fontSize = 23.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.weight(1f))
            if (isCompleted) {
                Box(Modifier.size(25.dp).background(RoadGold, CircleShape), contentAlignment = Alignment.Center) {
                    CheckIcon(color = RoadNavy, size = 13.dp)
                }
            } else if (isCurrent) {
                Box(Modifier.size(8.dp).background(RoadGold, CircleShape))
            }
        }
        ArrowEmblem(seed = artSeed, color = if (emphasized) RoadGold else Color(0xFF8598BE), modifier = Modifier.padding(top = 8.dp, bottom = 8.dp).size(61.dp))
        Text(title, color = RoadWhite, fontSize = 16.sp, lineHeight = 19.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Text(subtitle, color = RoadMuted, fontSize = 9.sp, letterSpacing = 0.6.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 6.dp))
        Spacer(Modifier.weight(1f))
        Box(Modifier.fillMaxWidth().height(1.dp).background(RoadWhite.copy(alpha = 0.08f)))
        Text(footer, color = if (emphasized) RoadGold else RoadMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp, modifier = Modifier.padding(top = 10.dp))
    }
}

/** Decorative arrow artwork shares the board's clear, rounded line language. */
@Composable
private fun ArrowEmblem(seed: Int, color: Color, modifier: Modifier = Modifier) {
    val paths = remember(seed % 5) {
        when (seed % 5) {
            0 -> listOf(listOf(0.12f to 0.78f, 0.12f to 0.4f, 0.6f to 0.4f, 0.6f to 0.12f), listOf(0.38f to 0.86f, 0.84f to 0.86f, 0.84f to 0.32f))
            1 -> listOf(listOf(0.16f to 0.84f, 0.16f to 0.16f, 0.52f to 0.16f), listOf(0.4f to 0.84f, 0.84f to 0.84f, 0.84f to 0.42f, 0.4f to 0.42f))
            2 -> listOf(listOf(0.12f to 0.68f, 0.5f to 0.3f, 0.88f to 0.68f), listOf(0.24f to 0.88f, 0.5f to 0.62f, 0.76f to 0.88f), listOf(0.5f to 0.3f, 0.5f to 0.08f))
            3 -> listOf(listOf(0.1f to 0.8f, 0.1f to 0.5f, 0.42f to 0.5f, 0.42f to 0.16f, 0.8f to 0.16f), listOf(0.4f to 0.82f, 0.8f to 0.82f, 0.8f to 0.5f))
            else -> listOf(listOf(0.18f to 0.8f, 0.18f to 0.2f, 0.82f to 0.2f, 0.82f to 0.8f, 0.5f to 0.8f, 0.5f to 0.48f))
        }
    }
    Canvas(modifier) {
        paths.forEachIndexed { index, points ->
            val ink = color.copy(alpha = if (index == 0) 1f else 0.65f)
            val path = Path()
            points.forEachIndexed { pointIndex, (x, y) ->
                if (pointIndex == 0) path.moveTo(x * size.width, y * size.height)
                else path.lineTo(x * size.width, y * size.height)
            }
            drawPath(path, ink, style = Stroke(3.5.dp.toPx(), cap = StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round))
            val tip = Offset(points.last().first * size.width, points.last().second * size.height)
            val previous = Offset(points[points.lastIndex - 1].first * size.width, points[points.lastIndex - 1].second * size.height)
            val delta = tip - previous
            val length = sqrt(delta.x * delta.x + delta.y * delta.y).coerceAtLeast(1f)
            val direction = delta / length
            val normal = Offset(-direction.y, direction.x)
            val wing = 6.dp.toPx()
            drawLine(ink, tip - direction * wing + normal * wing * 0.75f, tip, 3.5.dp.toPx(), StrokeCap.Round)
            drawLine(ink, tip - direction * wing - normal * wing * 0.75f, tip, 3.5.dp.toPx(), StrokeCap.Round)
        }
    }
}
