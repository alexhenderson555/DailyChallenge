package com.dailychallenge.app.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.dailychallenge.app.R
import com.dailychallenge.app.data.AppRepository
import com.dailychallenge.app.data.Challenges
import com.dailychallenge.app.data.DayRecord
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

private val WEEKDAY_LABELS = listOf(
    R.string.calendar_weekday_mon,
    R.string.calendar_weekday_tue,
    R.string.calendar_weekday_wed,
    R.string.calendar_weekday_thu,
    R.string.calendar_weekday_fri,
    R.string.calendar_weekday_sat,
    R.string.calendar_weekday_sun,
)

@Composable
fun CalendarScreen(
    repository: AppRepository,
    onRequestPremium: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val records by repository.records.collectAsState(initial = emptyList())
    val prefs by repository.preferences.collectAsState(initial = null)
    val scope = rememberCoroutineScope()
    val isPremium = prefs?.isPremium == true
    val config = LocalConfiguration.current
    val appLocale = remember(config, prefs?.languageCode) {
        when (prefs?.languageCode) {
            "ru" -> Locale("ru")
            "en" -> Locale.ENGLISH
            else -> config.locales[0]
        }
    }
    var isWeekView by remember { mutableStateOf(true) }
    var currentMonth by remember { mutableStateOf(YearMonth.now()) }
    var monthDirection by remember { mutableStateOf(1) }
    var searchQuery by remember { mutableStateOf("") }
    val filteredRecords = remember(records, searchQuery) {
        if (searchQuery.isBlank()) null
        else records.filter {
            it.challengeText.contains(searchQuery, ignoreCase = true) ||
                    it.note.contains(searchQuery, ignoreCase = true)
        }
    }
    var selectedDate by remember { mutableStateOf<String?>(null) }
    val monthTitle = remember(currentMonth, appLocale) {
        currentMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy", appLocale)).replaceFirstChar { it.uppercase() }
    }
    val today = LocalDate.now()
    val weekMonday = today.minusDays(today.dayOfWeek.value - 1L)
    val weekDates = (0..6).map { weekMonday.plusDays(it.toLong()) }
    val weekMonthTitle = remember(weekMonday, appLocale) {
        YearMonth.from(weekMonday).format(DateTimeFormatter.ofPattern("MMMM yyyy", appLocale)).replaceFirstChar { it.uppercase() }
    }

    Column(
        modifier = modifier.padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        androidx.compose.material3.OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("🔍 ${stringResource(R.string.stats_title)}…") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = null,
                        modifier = Modifier.clickable { searchQuery = "" }
                    )
                }
            }
        )
        if (filteredRecords != null) {
            Spacer(modifier = Modifier.height(8.dp))
            if (filteredRecords.isEmpty()) {
                Text(
                    stringResource(R.string.calendar_no_record),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp)
                )
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth().height(400.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    filteredRecords.sortedByDescending { it.date }.forEach { r ->
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable { selectedDate = r.date },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(r.date, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(r.challengeText, style = MaterialTheme.typography.bodyMedium)
                                if (r.note.isNotBlank()) {
                                    Text(r.note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .clickable { isWeekView = true }
                        .background(
                            if (isWeekView) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                            RoundedCornerShape(6.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(stringResource(R.string.calendar_week), style = MaterialTheme.typography.labelLarge)
                }
                Row(
                    modifier = Modifier
                        .clickable {
                            if (isPremium) {
                                isWeekView = false
                            } else {
                                onRequestPremium()
                            }
                        }
                        .background(
                            if (!isWeekView) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                            RoundedCornerShape(6.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        if (isPremium) stringResource(R.string.calendar_month) else stringResource(R.string.calendar_month_premium),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        if (isWeekView) {
            Text(
                weekMonthTitle,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally)
            ) {
                weekDates.forEach { date ->
                    val dateStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
                    val record = records.find { it.date == dateStr }
                    val dayNum = date.dayOfMonth
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            stringResource(WEEKDAY_LABELS[date.dayOfWeek.value - 1]),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        DayCell(
                            day = dayNum,
                            record = record,
                            isToday = dateStr == repository.todayKey(),
                            onClick = { selectedDate = dateStr }
                        )
                    }
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = stringResource(R.string.a11y_prev_month),
                    modifier = Modifier.clickable { monthDirection = -1; currentMonth = currentMonth.minusMonths(1) },
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    monthTitle,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Icon(
                    Icons.Default.ArrowForward,
                    contentDescription = stringResource(R.string.a11y_next_month),
                    modifier = Modifier.clickable { monthDirection = 1; currentMonth = currentMonth.plusMonths(1) },
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            AnimatedContent(
                targetState = currentMonth,
                transitionSpec = {
                    val dir = monthDirection
                    slideInHorizontally { fullWidth -> dir * fullWidth } togetherWith
                            slideOutHorizontally { fullWidth -> -dir * fullWidth }
                },
                label = "calendarMonth"
            ) { animMonth ->
                val animDaysInMonth = animMonth.lengthOfMonth()
                val animFirstDay = animMonth.atDay(1)
                val animStartOffset = animFirstDay.dayOfWeek.value - 1
                LazyVerticalGrid(
                    columns = GridCells.Fixed(7),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(7) { i ->
                        Text(
                            stringResource(WEEKDAY_LABELS[i]),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    items(animStartOffset) { Box(Modifier.size(56.dp)) }
                    items(animDaysInMonth) { dayIndex ->
                        val day = dayIndex + 1
                        val dateStr = animMonth.atDay(day).format(DateTimeFormatter.ISO_LOCAL_DATE)
                        val record = records.find { it.date == dateStr }
                        DayCell(
                            day = day,
                            record = record,
                            isToday = dateStr == repository.todayKey(),
                            onClick = { selectedDate = dateStr }
                        )
                    }
                }
            }
        }
    }

    selectedDate?.let { dateStr ->
        val record = records.find { it.date == dateStr }
        val locale = remember(config, prefs?.languageCode) {
            when (prefs?.languageCode) {
                "ru" -> Locale("ru")
                "en" -> Locale.ENGLISH
                else -> config.locales[0]
            }
        }
        val isFuture = try { LocalDate.parse(dateStr).isAfter(LocalDate.now()) } catch (_: Exception) { false }
        DayDetailBottomSheet(
            dateStr = dateStr,
            record = record,
            locale = locale,
            isFuture = isFuture,
            onDismiss = { selectedDate = null },
            onSaveNote = { note ->
                scope.launch { repository.addNoteToRecord(dateStr, note) }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DayDetailBottomSheet(
    dateStr: String,
    record: DayRecord?,
    locale: Locale,
    isFuture: Boolean = false,
    onDismiss: () -> Unit,
    onSaveNote: (String) -> Unit = {}
) {
    val formatted = remember(dateStr, locale) {
        try {
            LocalDate.parse(dateStr).format(DateTimeFormatter.ofPattern("EEEE, d MMMM", locale)).replaceFirstChar { it.uppercase() }
        } catch (_: Exception) { dateStr }
    }
    var editingNote by remember { mutableStateOf(false) }
    var noteText by remember(record) { mutableStateOf(record?.note ?: "") }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(formatted, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
            Spacer(modifier = Modifier.height(12.dp))
            if (record != null) {
                Text(record.challengeText, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    stringResource(R.string.calendar_category, categoryDisplayName(record.categoryId)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    when {
                        record.completed -> stringResource(R.string.calendar_completed)
                        record.usedFreeze -> stringResource(R.string.calendar_frozen)
                        else -> stringResource(R.string.calendar_not_completed)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                if (editingNote) {
                    androidx.compose.material3.OutlinedTextField(
                        value = noteText,
                        onValueChange = { noteText = it },
                        label = { Text(stringResource(R.string.home_note_hint)) },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 4
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        androidx.compose.material3.Button(onClick = {
                            onSaveNote(noteText.trim())
                            editingNote = false
                        }) { Text(stringResource(R.string.home_note_save)) }
                        androidx.compose.material3.TextButton(onClick = {
                            noteText = record.note
                            editingNote = false
                        }) { Text(stringResource(R.string.settings_cancel)) }
                    }
                } else {
                    if (record.note.isNotBlank()) {
                        Text(
                            stringResource(R.string.calendar_note, record.note),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    androidx.compose.material3.TextButton(onClick = { editingNote = true }) {
                        Text(stringResource(R.string.calendar_edit_note))
                    }
                }
            } else if (isFuture) {
                Text(stringResource(R.string.calendar_future), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Text(stringResource(R.string.calendar_no_record), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun DayCell(
    day: Int,
    record: DayRecord?,
    isToday: Boolean,
    onClick: () -> Unit = {}
) {
    val (bg, icon) = when {
        record == null -> MaterialTheme.colorScheme.surfaceVariant to null
        record.completed -> MaterialTheme.colorScheme.primaryContainer to Icons.Default.Check
        else -> MaterialTheme.colorScheme.surfaceVariant to Icons.Default.Close
    }
    val cellSize = 56.dp
    Card(
        modifier = Modifier
            .size(cellSize)
            .then(if (isToday) Modifier.padding(1.dp) else Modifier)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = bg)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxWidth().height(cellSize)
        ) {
            if (icon != null) {
                Icon(
                    icon,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            } else {
                Text(
                    "$day",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
