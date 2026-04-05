package com.dailychallenge.app.ui

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.dailychallenge.app.R
import com.dailychallenge.app.data.AppRepository
import com.dailychallenge.app.data.Challenges
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate

@Composable
fun StatsScreen(
    repository: AppRepository,
    modifier: Modifier = Modifier
) {
    val records by repository.records.collectAsState(initial = emptyList())
    val streak = repository.getCurrentStreak(records)
    val best = repository.getBestStreak(records)
    val (completed, total) = repository.monthStats(records)
    val (weekDone, weekTotal) = repository.weekStats(records)
    val totalCompleted = records.count { it.completed }
    val byCategory = remember(records) { repository.completedByCategory(records) }
    val yearMap = remember(records) { repository.yearCompletionMap(records) }
    val byDayOfWeek = remember(records) { repository.completionByDayOfWeek(records) }
    val weeklyProgress = remember(records) { repository.weeklyProgress(records) }
    val year = LocalDate.now().year
    val yearCellsByMonth = remember(year) {
        (1..12).map { month ->
            val start = LocalDate.of(year, month, 1)
            val length = start.lengthOfMonth()
            (0 until length).map { start.plusDays(it.toLong()) } + List(31 - length) { null as LocalDate? }
        }
    }

    val context = LocalContext.current
    val primaryArgb = MaterialTheme.colorScheme.primary.toArgb()
    val onBgArgb = MaterialTheme.colorScheme.onBackground.toArgb()
    val surfaceArgb = MaterialTheme.colorScheme.surface.toArgb()
    val appName = stringResource(R.string.app_name)
    val streakLabel = stringResource(R.string.stats_current_streak)
    val bestLabel = stringResource(R.string.stats_best_streak)
    val daysLabel = stringResource(R.string.stats_days)
    val completedLabel = stringResource(R.string.stats_this_month)
    val ofLabel = stringResource(R.string.stats_of, completed, total)
    val totalLabel = stringResource(R.string.stats_total)

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(20.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                stringResource(R.string.stats_title),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            if (records.isNotEmpty()) {
                IconButton(onClick = {
                    shareStats(
                        context, appName, streak, best, totalCompleted,
                        completed, total, daysLabel, streakLabel, bestLabel,
                        completedLabel, ofLabel, totalLabel, primaryArgb, onBgArgb, surfaceArgb
                    )
                }) {
                    Icon(Icons.Default.Share, contentDescription = stringResource(R.string.stats_share), tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
        if (records.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_illustration_stats_empty),
                    contentDescription = null,
                    modifier = Modifier.size(96.dp),
                    colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                )
                Text(
                    stringResource(R.string.stats_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.weight(1f)) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            stringResource(R.string.stats_current_streak),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            "$streak " + stringResource(R.string.stats_days),
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            stringResource(R.string.stats_this_month),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            stringResource(R.string.stats_of, completed, total),
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.weight(1f)) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            stringResource(R.string.stats_best_streak),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            "$best " + stringResource(R.string.stats_days),
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(stringResource(R.string.stats_this_week), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                        Text(stringResource(R.string.stats_of, weekDone, weekTotal), style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }
        Text(
            stringResource(R.string.stats_year_pixels),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(top = 8.dp)
        )
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Box(modifier = Modifier.height(200.dp)) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        yearCellsByMonth.forEach { monthDates ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                monthDates.forEach { date ->
                                    val color = when {
                                        date == null -> MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)
                                        else -> {
                                            val dateStr = date.format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE)
                                            val completed = yearMap[dateStr] == true
                                            val hasRecord = yearMap.containsKey(dateStr)
                                            when {
                                                completed -> MaterialTheme.colorScheme.primary
                                                hasRecord -> MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
                                                else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                                            }
                                        }
                                    }
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(1f)
                                            .clip(CircleShape)
                                            .background(color)
                                    )
                                }
                            }
                        }
                    }
                }
                Row(
                    modifier = Modifier.padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                        )
                        Text(
                            stringResource(R.string.stats_year_legend_done),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                        )
                        Text(
                            stringResource(R.string.stats_year_legend_rest),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        Text(
            stringResource(R.string.stats_by_weekday),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(top = 20.dp)
        )
        WeekdayBarChart(byDayOfWeek)

        Text(
            stringResource(R.string.stats_weekly_progress),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(top = 20.dp)
        )
        WeeklyLineChart(weeklyProgress)

        Text(
            stringResource(R.string.stats_by_categories),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(top = 20.dp)
        )
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Challenges.categories.forEach { cat ->
                    val count = byCategory[cat.id] ?: 0
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(categoryDisplayName(cat.id), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                        Text("$count", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
        Text(
            stringResource(R.string.stats_achievements),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(top = 8.dp)
        )
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(
                Triple(7, best, R.string.stats_streak_7),
                Triple(30, best, R.string.stats_streak_30),
                Triple(10, totalCompleted, R.string.stats_goals_10),
                Triple(25, totalCompleted, R.string.stats_goals_25),
                Triple(50, totalCompleted, R.string.stats_goals_50),
                Triple(100, totalCompleted, R.string.stats_goals_100),
                Triple(200, totalCompleted, R.string.stats_goals_200),
                Triple(500, totalCompleted, R.string.stats_goals_500)
            ).chunked(2).forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowItems.forEach { (target, current, labelRes) ->
                        val unlocked = current >= target
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = if (unlocked) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(stringResource(labelRes), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                                Text(if (unlocked) stringResource(R.string.stats_unlocked) else stringResource(R.string.stats_progress, current, target), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    if (rowItems.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
        Text(
            stringResource(R.string.stats_achievements_by_category),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(top = 20.dp)
        )
        val categoryTarget = 5
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Challenges.categories.chunked(2).forEach { rowCats ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowCats.forEach { cat ->
                        val count = byCategory[cat.id] ?: 0
                        val unlocked = count >= categoryTarget
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = if (unlocked) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    stringResource(R.string.stats_category_goals, categoryDisplayName(cat.id), categoryTarget),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(if (unlocked) stringResource(R.string.stats_unlocked) else stringResource(R.string.stats_progress, count, categoryTarget), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    if (rowCats.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
        }
    }
}

private fun shareStats(
    context: android.content.Context,
    appName: String,
    streak: Int,
    best: Int,
    totalCompleted: Int,
    monthDone: Int,
    monthTotal: Int,
    daysLabel: String,
    streakLabel: String,
    bestLabel: String,
    completedLabel: String,
    ofLabel: String,
    totalLabel: String,
    primaryArgb: Int,
    onBgArgb: Int,
    surfaceArgb: Int
) {
    try {
        val w = 600
        val h = 400
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = AndroidCanvas(bmp)
        canvas.drawColor(surfaceArgb)

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 32f; typeface = Typeface.DEFAULT_BOLD; color = primaryArgb; textAlign = Paint.Align.CENTER
        }
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 20f; color = onBgArgb; textAlign = Paint.Align.CENTER
        }
        val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 48f; typeface = Typeface.DEFAULT_BOLD; color = primaryArgb; textAlign = Paint.Align.CENTER
        }
        canvas.drawText(appName, w / 2f, 50f, titlePaint)

        canvas.drawText(streakLabel, w / 4f, 110f, labelPaint)
        canvas.drawText("$streak $daysLabel", w / 4f, 165f, valuePaint)

        canvas.drawText(bestLabel, 3 * w / 4f, 110f, labelPaint)
        canvas.drawText("$best $daysLabel", 3 * w / 4f, 165f, valuePaint)

        canvas.drawText(completedLabel, w / 4f, 230f, labelPaint)
        canvas.drawText(ofLabel, w / 4f, 280f, valuePaint)

        canvas.drawText(totalLabel, 3 * w / 4f, 230f, labelPaint)
        canvas.drawText("$totalCompleted", 3 * w / 4f, 280f, valuePaint)

        val bottomPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 14f; color = (onBgArgb and 0x00FFFFFF) or 0x99000000.toInt(); textAlign = Paint.Align.CENTER
        }
        canvas.drawText("dailychallenge.app", w / 2f, h - 20f, bottomPaint)

        val dir = File(context.cacheDir, "share")
        dir.mkdirs()
        val file = File(dir, "stats.png")
        FileOutputStream(file).use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bmp.recycle()

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, null))
    } catch (_: Exception) {
    }
}

@Composable
private fun WeekdayBarChart(byDayOfWeek: Map<Int, Int>) {
    val primary = MaterialTheme.colorScheme.primary
    val outline = MaterialTheme.colorScheme.outlineVariant
    val onSurfaceArgb = MaterialTheme.colorScheme.onSurfaceVariant.toArgb()
    val maxVal = (byDayOfWeek.values.maxOrNull() ?: 1).coerceAtLeast(1)
    val density = LocalDensity.current
    val weekdayLabels = listOf(
        stringResource(R.string.calendar_weekday_mon),
        stringResource(R.string.calendar_weekday_tue),
        stringResource(R.string.calendar_weekday_wed),
        stringResource(R.string.calendar_weekday_thu),
        stringResource(R.string.calendar_weekday_fri),
        stringResource(R.string.calendar_weekday_sat),
        stringResource(R.string.calendar_weekday_sun),
    )

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Canvas(modifier = Modifier.fillMaxWidth().height(140.dp)) {
                val barCount = 7
                val gap = 8.dp.toPx()
                val barWidth = (size.width - gap * (barCount + 1)) / barCount
                val labelSpace = 20.dp.toPx()
                val chartHeight = size.height - labelSpace

                for (day in 1..7) {
                    val count = byDayOfWeek[day] ?: 0
                    val barHeight = if (maxVal > 0) (count.toFloat() / maxVal) * chartHeight else 0f
                    val x = gap + (day - 1) * (barWidth + gap)

                    drawRoundRect(
                        color = outline,
                        topLeft = Offset(x, 0f),
                        size = Size(barWidth, chartHeight),
                        cornerRadius = CornerRadius(4.dp.toPx())
                    )
                    if (barHeight > 0) {
                        drawRoundRect(
                            color = primary,
                            topLeft = Offset(x, chartHeight - barHeight),
                            size = Size(barWidth, barHeight),
                            cornerRadius = CornerRadius(4.dp.toPx())
                        )
                    }

                    val paint = android.graphics.Paint().apply {
                        textSize = with(density) { 10.dp.toPx() }
                        textAlign = android.graphics.Paint.Align.CENTER
                        color = onSurfaceArgb
                    }
                    drawContext.canvas.nativeCanvas.drawText(
                        weekdayLabels[day - 1],
                        x + barWidth / 2,
                        size.height,
                        paint
                    )
                }
            }
        }
    }
}

@Composable
private fun WeeklyLineChart(weeklyProgress: List<Pair<String, Int>>) {
    val primary = MaterialTheme.colorScheme.primary
    val outline = MaterialTheme.colorScheme.outlineVariant
    val onSurfaceArgb = MaterialTheme.colorScheme.onSurfaceVariant.toArgb()
    val maxVal = (weeklyProgress.maxOfOrNull { it.second } ?: 1).coerceAtLeast(1)
    val density = LocalDensity.current

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Canvas(modifier = Modifier.fillMaxWidth().height(140.dp)) {
                val labelSpace = 20.dp.toPx()
                val chartHeight = size.height - labelSpace
                val pointCount = weeklyProgress.size
                if (pointCount < 2) return@Canvas
                val stepX = size.width / (pointCount - 1).toFloat()

                val gridLines = 3
                for (i in 0..gridLines) {
                    val y = chartHeight * i / gridLines
                    drawLine(outline, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
                }

                val path = Path()
                weeklyProgress.forEachIndexed { index, (_, count) ->
                    val x = index * stepX
                    val y = chartHeight - (count.toFloat() / maxVal) * chartHeight
                    if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                drawPath(path, primary, style = Stroke(width = 2.5.dp.toPx()))

                weeklyProgress.forEachIndexed { index, (_, count) ->
                    val x = index * stepX
                    val y = chartHeight - (count.toFloat() / maxVal) * chartHeight
                    drawCircle(primary, radius = 3.dp.toPx(), center = Offset(x, y))
                }

                val paint = android.graphics.Paint().apply {
                    textSize = with(density) { 8.dp.toPx() }
                    textAlign = android.graphics.Paint.Align.CENTER
                    color = onSurfaceArgb
                }
                weeklyProgress.forEachIndexed { index, (label, _) ->
                    if (index % 3 == 0 || index == pointCount - 1) {
                        val x = index * stepX
                        drawContext.canvas.nativeCanvas.drawText(label, x, size.height, paint)
                    }
                }
            }
        }
    }
}
