@file:OptIn(ExperimentalMaterial3Api::class)
package com.dailychallenge.app.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.dailychallenge.app.R
import com.dailychallenge.app.data.Challenges
import com.dailychallenge.app.reminder.ReminderReceiver
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope

private const val TOTAL_STEPS = 3

@Composable
fun OnboardingScreen(
    viewModel: AppViewModel,
    onFinish: () -> Unit
) {
    var step by remember { mutableIntStateOf(0) }
    var previousStep by remember { mutableIntStateOf(0) }
    var reminderHour by remember { mutableIntStateOf(9) }
    var reminderMinute by remember { mutableIntStateOf(0) }
    var selectedIds by remember { mutableStateOf(setOf("health", "productivity")) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val duration = 400
    val spec = tween<IntOffset>(durationMillis = duration, easing = FastOutSlowInEasing)

    Column(modifier = Modifier.fillMaxSize()) {
        StepIndicator(current = step)
        AnimatedContent(
            targetState = step,
            transitionSpec = {
                val goingForward = targetState > previousStep
                if (goingForward) {
                    slideInHorizontally(animationSpec = spec, initialOffsetX = { it }) togetherWith
                        slideOutHorizontally(animationSpec = spec, targetOffsetX = { -it })
                } else {
                    slideInHorizontally(animationSpec = spec, initialOffsetX = { -it }) togetherWith
                        slideOutHorizontally(animationSpec = spec, targetOffsetX = { it })
                }
            },
            modifier = Modifier.weight(1f),
            label = "onboarding"
        ) { currentStep ->
            when (currentStep) {
                0 -> OnboardingWelcome(onNext = { previousStep = 0; step = 1 })
                1 -> OnboardingTime(
                    initialHour = reminderHour,
                    initialMinute = reminderMinute,
                    onBack = { previousStep = 1; step = 0 },
                    onNext = { h, m -> reminderHour = h; reminderMinute = m; previousStep = 1; step = 2 }
                )
                2 -> OnboardingCategories(
                    selectedIds = selectedIds,
                    onSelectionChange = { selectedIds = it },
                    onBack = { previousStep = 2; step = 1 },
                    onStart = {
                        if (selectedIds.isEmpty()) return@OnboardingCategories
                        viewModel.finishOnboarding(reminderHour, reminderMinute, selectedIds)
                        scope.launch { ReminderReceiver.scheduleFromContext(context) }
                        onFinish()
                    }
                )
            }
        }
    }
}

@Composable
private fun StepIndicator(current: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 48.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(TOTAL_STEPS) { index ->
            val isActive = index <= current
            Box(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .size(if (index == current) 10.dp else 8.dp)
                    .clip(CircleShape)
                    .background(
                        if (isActive) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outlineVariant
                    )
            )
        }
    }
}

@Composable
private fun OnboardingWelcome(onNext: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 420.dp)
                .fillMaxWidth(0.92f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(R.drawable.ic_illustration_onboarding),
                contentDescription = null,
                modifier = Modifier.size(120.dp),
                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                stringResource(R.string.onboarding_title),
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                stringResource(R.string.onboarding_desc),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(32.dp))
            Button(onClick = onNext, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.onboarding_next))
            }
        }
    }
}

@Composable
private fun OnboardingTime(
    initialHour: Int,
    initialMinute: Int,
    onBack: () -> Unit,
    onNext: (hour: Int, minute: Int) -> Unit
) {
    val timeState = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = true
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 420.dp)
                .fillMaxWidth(0.92f)
                .fillMaxSize()
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.onboarding_back))
            }
            Text(
                stringResource(R.string.onboarding_reminder_title),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                stringResource(R.string.onboarding_reminder_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                TimePicker(state = timeState)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { onNext(timeState.hour, timeState.minute) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.onboarding_next))
            }
        }
    }
}

@Composable
private fun OnboardingCategories(
    selectedIds: Set<String>,
    onSelectionChange: (Set<String>) -> Unit,
    onBack: () -> Unit,
    onStart: () -> Unit
) {
    val freeCategories = Challenges.categories.filter { !it.isPremium }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 420.dp)
                .fillMaxWidth(0.92f)
                .fillMaxSize()
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.onboarding_back))
            }
            Text(
                stringResource(R.string.onboarding_categories_title),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                stringResource(R.string.onboarding_categories_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            freeCategories.forEach { cat ->
            val selected = cat.id in selectedIds
            val toggle: () -> Unit = {
                val newSet = if (selected) selectedIds - cat.id else selectedIds + cat.id
                if (newSet.size <= 2) onSelectionChange(newSet)
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .clickable(onClick = toggle)
                    .background(
                        if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                        RoundedCornerShape(12.dp)
                    )
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (selected) Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.size(8.dp))
                Text(categoryDisplayName(cat.id), style = MaterialTheme.typography.titleMedium)
            }
        }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                stringResource(R.string.onboarding_categories_footer),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.weight(1f))
            Button(
                onClick = onStart,
                modifier = Modifier.fillMaxWidth(),
                enabled = selectedIds.isNotEmpty()
            ) {
                Text(stringResource(R.string.onboarding_start))
            }
        }
    }
}
