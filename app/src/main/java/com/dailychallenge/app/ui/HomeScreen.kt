package com.dailychallenge.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Assignment
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import kotlinx.coroutines.launch
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.dailychallenge.app.R
import com.dailychallenge.app.data.AppRepository
import com.dailychallenge.app.data.Challenges
import com.dailychallenge.app.data.PendingChallenge
import com.dailychallenge.app.util.SoundHelper
import com.dailychallenge.app.widget.DailyGoalWidget
import com.dailychallenge.app.widget.WidgetHelper
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    repository: AppRepository,
    onRequestPremium: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val prefs by repository.preferences.collectAsState(initial = null)
    val records by repository.records.collectAsState(initial = emptyList())
    val pending by repository.pendingChallenges.collectAsState(initial = emptyList())
    var replaceRemaining by remember { mutableStateOf(1) }
    var canReplace by remember { mutableStateOf(true) }
    var showChoiceModal by remember { mutableStateOf(false) }
    var choiceAlternatives by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    var replaceTargetDate by remember { mutableStateOf<String?>(null) }
    var freezeCount by remember { mutableStateOf(0) }
    var canUseFreeze by remember { mutableStateOf(false) }
    var showFreezeConfirm by remember { mutableStateOf(false) }
    var showSuccessAnimation by remember { mutableStateOf(false) }
    var achievementUnlockedRes by remember { mutableStateOf<Int?>(null) }
    var achievementUnlockedCategoryId by remember { mutableStateOf<String?>(null) }
    val successScale = remember { Animatable(0f) }
    val successAlpha = remember { Animatable(0f) }
    val snackbarHostState = remember { SnackbarHostState() }
    var showNoteDialog by remember { mutableStateOf(false) }
    var noteDialogDate by remember { mutableStateOf("") }
    var noteText by remember { mutableStateOf("") }
    val context = LocalContext.current
    val todayKey = repository.todayKey()
    val todayRecord = records.find { it.date == todayKey }
    val undoLabel = stringResource(R.string.home_undo)
    val markedDoneLabel = stringResource(R.string.home_marked_done)
    val markedNotDoneLabel = stringResource(R.string.home_marked_not_done)

    LaunchedEffect(showSuccessAnimation) {
        if (showSuccessAnimation) {
            val smoothShort = tween<Float>(durationMillis = 220, easing = FastOutSlowInEasing)
            val smoothMedium = tween<Float>(durationMillis = 280, easing = FastOutSlowInEasing)
            val smoothFadeOut = tween<Float>(durationMillis = 400, easing = FastOutSlowInEasing)
            successScale.snapTo(0f)
            successAlpha.snapTo(0f)
            successScale.animateTo(1.2f, smoothShort)
            successAlpha.animateTo(1f, smoothShort)
            successScale.animateTo(1f, smoothMedium)
            kotlinx.coroutines.delay(800)
            successAlpha.animateTo(0f, smoothFadeOut)
            showSuccessAnimation = false
        }
    }

    LaunchedEffect(achievementUnlockedRes, achievementUnlockedCategoryId) {
        if (achievementUnlockedRes != null || achievementUnlockedCategoryId != null) {
            kotlinx.coroutines.delay(3000)
            achievementUnlockedRes = null
            achievementUnlockedCategoryId = null
        }
    }

    LaunchedEffect(prefs?.isPremium, records) {
        canReplace = repository.canReplaceToday()
        replaceRemaining = repository.getReplaceOrChoiceRemainingToday()
        freezeCount = repository.getFreezeCount()
        canUseFreeze = repository.canUseFreezeToday()
    }

    val resolvedLanguageCode = prefs?.languageCode ?: LocalConfiguration.current.locales[0].language
    LaunchedEffect(Unit, resolvedLanguageCode) {
        repository.ensureTodayInPending(resolvedLanguageCode)
    }

    LaunchedEffect(pending, records, todayKey, prefs?.theme) {
        val goalText = pending.find { it.date == todayKey }?.challengeText
            ?: todayRecord?.challengeText
            ?: ""
        val isDone = todayRecord?.completed ?: false
        val streak = repository.getCurrentStreak(records)
        val theme = prefs?.theme ?: "system"
        WidgetHelper.save(context.applicationContext, goalText, todayKey, isDone, streak, theme)
        DailyGoalWidget.updateAll(context.applicationContext)
    }

    val config = LocalConfiguration.current
    val appLocale = remember(prefs?.languageCode) {
        when (prefs?.languageCode) {
            "ru" -> Locale("ru")
            "en" -> Locale.ENGLISH
            else -> config.locales[0]
        }
    }
    val todayFormatted = remember(appLocale) {
        LocalDate.now().format(
            DateTimeFormatter.ofPattern("EEEE, d MMMM", appLocale)
        ).replaceFirstChar { it.uppercase() }
    }
    val dateFormatter = remember(appLocale) {
        DateTimeFormatter.ofPattern("d MMM", appLocale)
    }
    val todayLabel = stringResource(R.string.home_today)
    val streak = remember(records) { repository.getCurrentStreak(records) }
    val (monthDone, monthTotal) = remember(records) { repository.monthStats(records) }

    Box(modifier = modifier.fillMaxSize()) {
        val sortedPending = pending.sortedBy { it.date }
        val isSingleTodayGoal = sortedPending.size == 1 && sortedPending[0].date == todayKey
        if (isSingleTodayGoal) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 40.dp, y = 80.dp)
                    .size(220.dp)
                    .background(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.03f),
                        CircleShape
                    )
            )
        }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        Text(
            todayFormatted,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (streak > 0) {
            Text(
                "🔥 " + pluralStringResource(R.plurals.home_streak_days, streak, streak),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(modifier = Modifier.height(24.dp))

        if (prefs?.selectedCategoryIds?.isEmpty() != false) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_illustration_empty_categories),
                    contentDescription = null,
                    modifier = Modifier.size(96.dp)
                )
                Text(
                    stringResource(R.string.home_choose_categories),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else if (pending.isEmpty() && todayRecord == null) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                androidx.compose.material3.CircularProgressIndicator(
                    modifier = Modifier.size(48.dp),
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    stringResource(R.string.home_loading),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (isSingleTodayGoal) Modifier.weight(1f)
                        else Modifier.verticalScroll(rememberScrollState())
                    ),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (isSingleTodayGoal) Spacer(modifier = Modifier.weight(1f))
                for (entry in sortedPending) {
                    val dateLabel = if (entry.date == todayKey) todayLabel else LocalDate.parse(entry.date).format(dateFormatter)
                    val isToday = entry.date == todayKey
                    val isHero = isSingleTodayGoal && isToday
                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = { direction ->
                            val completed = direction == SwipeToDismissBoxValue.StartToEnd
                            val savedEntry = PendingChallenge(entry.date, entry.categoryId, entry.challengeText)
                            if (completed && prefs?.vibrationEnabled != false) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                            if (completed && prefs?.soundEnabled == true) {
                                SoundHelper.playSuccessSound(context)
                            }
                            if (completed) showSuccessAnimation = true
                            scope.launch {
                                repository.markResultForDate(entry.date, completed)
                                if (completed) {
                                    noteDialogDate = entry.date
                                    noteText = ""
                                    showNoteDialog = true
                                }
                                if (isToday && !completed) {
                                    canUseFreeze = repository.canUseFreezeToday()
                                    freezeCount = repository.getFreezeCount()
                                }
                                val msg = if (completed) markedDoneLabel else markedNotDoneLabel
                                val result = snackbarHostState.showSnackbar(msg, undoLabel, duration = SnackbarDuration.Short)
                                if (result == SnackbarResult.ActionPerformed) {
                                    repository.undoMarkResult(savedEntry.date, savedEntry)
                                }
                            }
                            true
                        }
                    )
                    SwipeToDismissBox(
                        state = dismissState,
                        backgroundContent = {
                            val direction = dismissState.dismissDirection
                            val bgColor = when (direction) {
                                SwipeToDismissBoxValue.StartToEnd -> MaterialTheme.colorScheme.primaryContainer
                                SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.errorContainer
                                else -> MaterialTheme.colorScheme.surface
                            }
                            val icon = when (direction) {
                                SwipeToDismissBoxValue.StartToEnd -> Icons.Default.Check
                                SwipeToDismissBoxValue.EndToStart -> Icons.Default.Close
                                else -> Icons.Default.Check
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(bgColor, RoundedCornerShape(16.dp))
                                    .padding(horizontal = 24.dp),
                                contentAlignment = if (direction == SwipeToDismissBoxValue.EndToStart) Alignment.CenterEnd else Alignment.CenterStart
                            ) {
                                val desc = if (direction == SwipeToDismissBoxValue.StartToEnd) stringResource(R.string.a11y_mark_done) else stringResource(R.string.a11y_mark_not_done)
                                Icon(icon, contentDescription = desc, tint = MaterialTheme.colorScheme.onSurface)
                            }
                        },
                        enableDismissFromStartToEnd = true,
                        enableDismissFromEndToStart = true
                    ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(if (isHero) 28.dp else 20.dp)) {
                            Text(
                                dateLabel,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (isHero) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(modifier = Modifier.fillMaxWidth()) {
                                    Box(
                                        modifier = Modifier
                                            .wrapContentWidth(Alignment.Start)
                                            .background(
                                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                                                RoundedCornerShape(8.dp)
                                            )
                                            .padding(start = 0.dp, end = 12.dp, top = 6.dp, bottom = 6.dp)
                                    ) {
                                        Text(
                                            categoryDisplayName(entry.categoryId),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(if (isHero) 12.dp else 4.dp))
                            Text(
                                entry.challengeText,
                                style = if (isHero) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Button(
                                    onClick = {
                                        if (prefs?.vibrationEnabled != false) {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        }
                                        if (prefs?.soundEnabled == true) {
                                            SoundHelper.playSuccessSound(context)
                                        }
                                        showSuccessAnimation = true
                                        val prevStreak = repository.getCurrentStreak(records)
                                        val prevTotal = records.count { it.completed }
                                        val prevByCat = repository.completedByCategory(records)
                                        val savedEntry = PendingChallenge(entry.date, entry.categoryId, entry.challengeText)
                                        scope.launch {
                                            repository.markResultForDate(entry.date, true)
                                            noteDialogDate = entry.date
                                            noteText = ""
                                            showNoteDialog = true
                                            val newRecords = repository.getRecordsList()
                                            val newStreak = repository.getCurrentStreak(newRecords)
                                            val newTotal = newRecords.count { it.completed }
                                            val newByCat = repository.completedByCategory(newRecords)
                                            when {
                                                prevStreak < 7 && newStreak >= 7 -> achievementUnlockedRes = R.string.stats_streak_7
                                                prevStreak < 30 && newStreak >= 30 -> achievementUnlockedRes = R.string.stats_streak_30
                                                prevTotal < 10 && newTotal >= 10 -> achievementUnlockedRes = R.string.stats_goals_10
                                                prevTotal < 25 && newTotal >= 25 -> achievementUnlockedRes = R.string.stats_goals_25
                                                prevTotal < 50 && newTotal >= 50 -> achievementUnlockedRes = R.string.stats_goals_50
                                                prevTotal < 100 && newTotal >= 100 -> achievementUnlockedRes = R.string.stats_goals_100
                                                prevTotal < 200 && newTotal >= 200 -> achievementUnlockedRes = R.string.stats_goals_200
                                                prevTotal < 500 && newTotal >= 500 -> achievementUnlockedRes = R.string.stats_goals_500
                                                else -> {
                                                    val catId = Challenges.categories.firstOrNull { cat ->
                                                        (prevByCat[cat.id] ?: 0) < 5 && (newByCat[cat.id] ?: 0) >= 5
                                                    }?.id
                                                    if (catId != null) achievementUnlockedCategoryId = catId
                                                }
                                            }
                                            val result = snackbarHostState.showSnackbar(
                                                message = markedDoneLabel,
                                                actionLabel = undoLabel,
                                                duration = SnackbarDuration.Short
                                            )
                                            if (result == SnackbarResult.ActionPerformed) {
                                                repository.undoMarkResult(savedEntry.date, savedEntry)
                                            }
                                        }
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Check, null, Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.size(8.dp))
                                    Text(stringResource(R.string.home_done))
                                }
                                OutlinedButton(
                                    onClick = {
                                        val savedEntry = PendingChallenge(entry.date, entry.categoryId, entry.challengeText)
                                        scope.launch {
                                            repository.markResultForDate(entry.date, false)
                                            if (isToday) {
                                                canUseFreeze = repository.canUseFreezeToday()
                                                freezeCount = repository.getFreezeCount()
                                            }
                                            val result = snackbarHostState.showSnackbar(
                                                message = markedNotDoneLabel,
                                                actionLabel = undoLabel,
                                                duration = SnackbarDuration.Short
                                            )
                                            if (result == SnackbarResult.ActionPerformed) {
                                                repository.undoMarkResult(savedEntry.date, savedEntry)
                                            }
                                        }
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Close, null, Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.size(8.dp))
                                    Text(stringResource(R.string.home_not_done))
                                }
                            }
                            val isPremium = prefs?.isPremium == true
                            val showReplaceForThisCard = canReplace && (isPremium || isToday)
                            if (showReplaceForThisCard) {
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedButton(
                                    onClick = {
                                        if (!canReplace) return@OutlinedButton
                                        if (isPremium) {
                                            replaceTargetDate = entry.date
                                            scope.launch {
                                                choiceAlternatives = repository.pickAlternativeChallenges(4, resolvedLanguageCode)
                                                showChoiceModal = true
                                            }
                                        } else {
                                            scope.launch {
                                                repository.replacePendingChallenge(entry.date, resolvedLanguageCode)
                                                canReplace = repository.canReplaceToday()
                                                replaceRemaining = repository.getReplaceOrChoiceRemainingToday()
                                            }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = canReplace
                                ) {
                                    Icon(Icons.Default.Refresh, null, Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.size(8.dp))
                                    Text(
                                        when {
                                            !canReplace -> stringResource(R.string.home_used_today)
                                            isPremium -> stringResource(R.string.home_choose_from_4)
                                            else -> stringResource(R.string.home_other_goal)
                                        }
                                    )
                                }
                            }
                        }
                    }
                    } // SwipeToDismissBox
                }
                if (prefs?.isPremium == true) {
                    val lang = prefs?.languageCode
                    val weeklyChallenge = remember(lang) { Challenges.getWeeklyChallenge(lang) }
                    val monthlyChallenge = remember(lang) { Challenges.getMonthlyChallenge(lang) }
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(stringResource(R.string.home_weekly_challenge), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onTertiaryContainer)
                            Text(weeklyChallenge, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onTertiaryContainer)
                        }
                    }
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(stringResource(R.string.home_monthly_challenge), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onTertiaryContainer)
                            Text(monthlyChallenge, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onTertiaryContainer)
                        }
                    }
                }
                if (isSingleTodayGoal) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        stringResource(R.string.stats_this_month) + " " + stringResource(R.string.stats_of, monthDone, monthTotal),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.weight(1f))
                }
                if (todayRecord != null && pending.none { it.date == todayKey }) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text(
                                todayLabel,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                todayRecord.challengeText,
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                when {
                                    todayRecord.completed -> stringResource(R.string.home_completed)
                                    todayRecord.usedFreeze -> stringResource(R.string.home_frozen)
                                    else -> stringResource(R.string.home_not_completed)
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            if (todayRecord.completed) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    stringResource(R.string.home_no_more_goals_today),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f)
                                )
                            }
                            if (!todayRecord.completed && !todayRecord.usedFreeze && canUseFreeze) {
                                Spacer(modifier = Modifier.height(12.dp))
                                OutlinedButton(
                                    onClick = { showFreezeConfirm = true },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(stringResource(R.string.home_use_freeze, freezeCount))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
        if (achievementUnlockedRes != null || achievementUnlockedCategoryId != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .align(Alignment.TopCenter),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        stringResource(R.string.achievement_unlocked),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        if (achievementUnlockedCategoryId != null) {
                            stringResource(R.string.stats_category_goals, categoryDisplayName(achievementUnlockedCategoryId!!), 5)
                        } else {
                            stringResource(achievementUnlockedRes!!)
                        },
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
        if (showSuccessAnimation) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier
                        .size(120.dp)
                        .scale(successScale.value)
                        .alpha(successAlpha.value),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }

    if (showFreezeConfirm) {
        AlertDialog(
            onDismissRequest = { showFreezeConfirm = false },
            title = { Text(stringResource(R.string.home_use_freeze, freezeCount)) },
            text = { Text(stringResource(R.string.home_freeze_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        if (repository.useFreezeForToday()) {
                            freezeCount = repository.getFreezeCount()
                            canUseFreeze = repository.canUseFreezeToday()
                        }
                        showFreezeConfirm = false
                    }
                }) { Text(stringResource(R.string.home_done)) }
            },
            dismissButton = {
                TextButton(onClick = { showFreezeConfirm = false }) {
                    Text(stringResource(R.string.close))
                }
            }
        )
    }
    if (showChoiceModal && choiceAlternatives.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { showChoiceModal = false; replaceTargetDate = null },
            title = { Text(stringResource(R.string.home_choose_goal_title)) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    choiceAlternatives.forEach { (catId, text) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val targetDate = replaceTargetDate
                                    scope.launch {
                                        if (targetDate != null) {
                                            repository.choosePendingChallenge(targetDate, catId, text)
                                            replaceTargetDate = null
                                        }
                                        canReplace = repository.canReplaceToday()
                                        replaceRemaining = repository.getReplaceOrChoiceRemainingToday()
                                        showChoiceModal = false
                                    }
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showChoiceModal = false; replaceTargetDate = null }) {
                    Text(stringResource(R.string.close))
                }
            }
        )
    }
    if (showNoteDialog) {
        NoteDialog(
            noteText = noteText,
            onNoteTextChange = { noteText = it },
            onSave = {
                if (noteText.isNotBlank()) {
                    scope.launch { repository.addNoteToRecord(noteDialogDate, noteText.trim()) }
                }
                showNoteDialog = false
            },
            onSkip = { showNoteDialog = false }
        )
    }
}

