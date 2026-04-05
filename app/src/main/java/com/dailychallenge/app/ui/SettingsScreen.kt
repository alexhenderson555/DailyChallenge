@file:OptIn(ExperimentalMaterial3Api::class)
package com.dailychallenge.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import android.content.Intent
import android.net.Uri
import android.text.format.DateFormat
import androidx.compose.ui.unit.dp
import com.dailychallenge.app.BuildConfig
import com.dailychallenge.app.R
import com.dailychallenge.app.data.Challenges
import com.dailychallenge.app.reminder.ReminderReceiver
import com.dailychallenge.app.util.SoundHelper
import kotlinx.coroutines.launch
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.core.content.FileProvider
import com.google.gson.GsonBuilder
import java.io.File

@Composable
fun SettingsScreen(
    viewModel: AppViewModel,
    onRequestPremium: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repo = viewModel.getRepository()
    val prefs by repo.preferences.collectAsState(initial = null)
    var showTimePicker by remember { mutableStateOf(false) }
    var showThemePicker by remember { mutableStateOf(false) }
    var showSecondReminderPicker by remember { mutableStateOf(false) }
    var showLanguagePicker by remember { mutableStateOf(false) }
    var showAddCustomGoal by remember { mutableStateOf(false) }
    var showEditCustomGoal by remember { mutableStateOf(false) }
    var editGoalIndex by remember { mutableStateOf(-1) }
    var editGoalText by remember { mutableStateOf("") }
    var showDifficultyPicker by remember { mutableStateOf(false) }
    var customGoals by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    var addGoalCategoryId by remember { mutableStateOf<String?>(null) }
    var addGoalText by remember { mutableStateOf("") }
    var cloudSignedIn by remember { mutableStateOf(false) }
    var cloudMessage by remember { mutableStateOf<String?>(null) }
    var cloudLoading by remember { mutableStateOf(false) }
    var showCloudLoadConfirm by remember { mutableStateOf(false) }
    val activity = context as? android.app.Activity
    val sync = remember(repo, activity) { activity?.let { repo.createPlayGamesSync(it) } }

    LaunchedEffect(sync) {
        cloudSignedIn = sync?.isSignedIn() == true
    }

    LaunchedEffect(prefs?.isPremium) {
        customGoals = if (prefs?.isPremium == true) repo.prefs.getCustomGoals() else emptyList()
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(20.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            stringResource(R.string.settings_title),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )

        prefs?.let { p ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showTimePicker = true }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Notifications, null, tint = MaterialTheme.colorScheme.primary)
                Text(
                    stringResource(R.string.settings_reminder_at, formatTime(context, p.reminderHour, p.reminderMinute)),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(start = 16.dp)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth().clickable { onRequestPremium() }.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Star, null, tint = MaterialTheme.colorScheme.primary)
                Text(if (p.isPremium) stringResource(R.string.settings_premium_active) else stringResource(R.string.settings_go_premium), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = 16.dp))
            }

            if (p.isPremium) {
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { showSecondReminderPicker = true }.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Notifications, null, tint = MaterialTheme.colorScheme.primary)
                    Text(
                        if (p.secondReminderHour != null && p.secondReminderMinute != null)
                            stringResource(R.string.settings_second_reminder, formatTime(context, p.secondReminderHour!!, p.secondReminderMinute!!))
                        else stringResource(R.string.settings_second_reminder_off),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(start = 16.dp)
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { onRequestPremium() }.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Notifications, null, tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
                    Text(
                        stringResource(R.string.time_second_title) + " " + stringResource(R.string.settings_tap_for_premium),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(start = 16.dp)
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(stringResource(R.string.settings_weekdays_only), style = MaterialTheme.typography.bodyLarge)
                Switch(checked = p.remindWeekdaysOnly, onCheckedChange = { scope.launch { repo.prefs.setRemindWeekdaysOnly(it); ReminderReceiver.scheduleFromContext(context) } })
            }

            Row(
                modifier = Modifier.fillMaxWidth().clickable { showLanguagePicker = true }.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Language, null, tint = MaterialTheme.colorScheme.primary)
                Text(stringResource(R.string.settings_language) + ": " + languageLabel(p.languageCode), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = 16.dp))
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showThemePicker = true }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Palette, null, tint = MaterialTheme.colorScheme.primary)
                Text(stringResource(R.string.settings_theme, themeName(p.theme)), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = 16.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(stringResource(R.string.settings_sound), style = MaterialTheme.typography.bodyLarge)
                Switch(
                    checked = p.soundEnabled,
                    onCheckedChange = { scope.launch { repo.prefs.setSoundEnabled(it) } }
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(stringResource(R.string.settings_vibration), style = MaterialTheme.typography.bodyLarge)
                Switch(
                    checked = p.vibrationEnabled,
                    onCheckedChange = { scope.launch { repo.prefs.setVibrationEnabled(it) } }
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth().clickable { SoundHelper.playSuccessSound(context) }.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.settings_sound_test), style = MaterialTheme.typography.bodyLarge)
            }

            Row(
                modifier = Modifier.fillMaxWidth().clickable { showDifficultyPicker = true }.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(R.string.settings_difficulty) + ": " + difficultyLabel(p.difficulty),
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            Text(
                stringResource(R.string.settings_categories),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = 8.dp)
            )
            Challenges.categories.forEach { cat ->
                val selected = cat.id in p.selectedCategoryIds
                val canSelectPremium = p.isPremium && cat.isPremium
                val isLockedPremium = cat.isPremium && !p.isPremium
                val canToggle = (p.isPremium || (cat.id in p.selectedCategoryIds || p.selectedCategoryIds.size < 2)) && (!cat.isPremium || canSelectPremium)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (isLockedPremium) {
                                onRequestPremium()
                                return@clickable
                            }
                            if (!canToggle) return@clickable
                            val newSet = if (selected) p.selectedCategoryIds - cat.id else p.selectedCategoryIds + cat.id
                            if (newSet.isNotEmpty()) scope.launch { repo.prefs.setSelectedCategories(newSet) }
                        }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (p.isPremium || !cat.isPremium) {
                        Checkbox(
                            checked = selected,
                            onCheckedChange = { newChecked ->
                                if (isLockedPremium) return@Checkbox
                                if (!canToggle && newChecked) return@Checkbox
                                val newSet = if (newChecked) p.selectedCategoryIds + cat.id else p.selectedCategoryIds - cat.id
                                if (newSet.isNotEmpty() && (p.isPremium || newSet.size <= 2))
                                    scope.launch { repo.prefs.setSelectedCategories(newSet) }
                            },
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    } else {
                        Spacer(modifier = Modifier.padding(end = 8.dp).size(24.dp))
                    }
                    Text(
                        categoryDisplayName(cat.id) + if (isLockedPremium) " " + stringResource(R.string.settings_tap_for_premium) else "",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                }
            }
            Text(
                stringResource(R.string.settings_custom_goals),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = 8.dp)
            )
            if (p.isPremium) {
                customGoals.forEachIndexed { index, (catId, text) ->
                    val catName = categoryDisplayName(catId)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text, style = MaterialTheme.typography.bodyMedium)
                            Text(catName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = stringResource(R.string.settings_edit),
                            modifier = Modifier.clickable {
                                editGoalIndex = index
                                editGoalText = text
                                showEditCustomGoal = true
                            }.padding(end = 8.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = stringResource(R.string.a11y_delete),
                            modifier = Modifier.clickable {
                                scope.launch {
                                    val newList = customGoals.toMutableList().apply { removeAt(index) }
                                    repo.prefs.setCustomGoals(newList)
                                    customGoals = newList
                                }
                            },
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                        showAddCustomGoal = true
                        addGoalCategoryId = p.selectedCategoryIds.firstOrNull()
                        addGoalText = ""
                    }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Add, null, tint = MaterialTheme.colorScheme.primary)
                    Text(stringResource(R.string.settings_custom_goals_add), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(start = 8.dp))
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onRequestPremium() }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Add, null, tint = MaterialTheme.colorScheme.primary)
                    Text(
                        stringResource(R.string.settings_custom_goals_add) + " " + stringResource(R.string.settings_tap_for_premium),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }

        Text(
            stringResource(R.string.settings_cloud),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(top = 20.dp)
        )
        if (cloudSignedIn) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Cloud, null, tint = MaterialTheme.colorScheme.primary)
                OutlinedButton(
                    onClick = {
                        cloudLoading = true
                        cloudMessage = null
                        scope.launch {
                            sync?.saveProgress()
                                ?.onSuccess { cloudMessage = context.getString(R.string.settings_cloud_saved) }
                                ?.onFailure { cloudMessage = context.getString(R.string.settings_cloud_error, it.message ?: "") }
                            cloudLoading = false
                        }
                    },
                    enabled = !cloudLoading
                ) { Text(stringResource(R.string.settings_save_to_cloud)) }
                OutlinedButton(
                    onClick = { showCloudLoadConfirm = true },
                    enabled = !cloudLoading
                ) { Text(stringResource(R.string.settings_load_from_cloud)) }
            }
        } else {
            Button(
                onClick = {
                    cloudLoading = true
                    cloudMessage = null
                    scope.launch {
                        val ok = sync?.signIn() == true
                        cloudSignedIn = ok
                        if (!ok) cloudMessage = context.getString(R.string.settings_cloud_error, "Sign-in failed")
                        cloudLoading = false
                    }
                },
                enabled = !cloudLoading
            ) {
                if (cloudLoading) Text(stringResource(R.string.settings_cloud_signing_in))
                else Text(stringResource(R.string.settings_sign_in_play))
            }
        }
        cloudMessage?.let { msg ->
            Text(msg, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Text(
            stringResource(R.string.settings_widget_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 16.dp)
        )

        Text(
            stringResource(R.string.settings_about),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(top = 24.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    val url = context.getString(R.string.settings_policy_url)
                    if (url.isNotBlank()) {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    }
                }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(stringResource(R.string.settings_policy), style = MaterialTheme.typography.bodyLarge)
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    val url = context.getString(R.string.settings_contacts_url)
                    if (url.isNotBlank()) {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    }
                }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(stringResource(R.string.settings_contacts), style = MaterialTheme.typography.bodyLarge)
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onRequestPremium() }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(stringResource(R.string.settings_restore_purchases), style = MaterialTheme.typography.bodyLarge)
        }
        Text(
            stringResource(R.string.settings_export_data),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(start = 16.dp, top = 8.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(onClick = {
                scope.launch {
                    val records = repo.history.getRecords()
                    val json = GsonBuilder().setPrettyPrinting().create().toJson(records)
                    shareTextFile(context, "daily_challenge_export.json", json, "application/json")
                }
            }) { Text(stringResource(R.string.settings_export_json)) }
            OutlinedButton(onClick = {
                scope.launch {
                    val records = repo.history.getRecords()
                    val csv = buildString {
                        appendLine("date,challenge,category,completed,frozen,note")
                        records.forEach { r ->
                            appendLine("${r.date},\"${r.challengeText.replace("\"", "\"\"")}\",${r.categoryId},${r.completed},${r.usedFreeze},\"${r.note.replace("\"", "\"\"")}\"")
                        }
                    }
                    shareTextFile(context, "daily_challenge_export.csv", csv, "text/csv")
                }
            }) { Text(stringResource(R.string.settings_export_csv)) }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Info, null, Modifier.padding(end = 8.dp))
            Text(stringResource(R.string.settings_version, BuildConfig.VERSION_NAME), style = MaterialTheme.typography.bodySmall)
        }
    }

    if (showTimePicker && prefs != null) {
        val is24h = DateFormat.is24HourFormat(context)
        val timeState = rememberTimePickerState(
            initialHour = prefs!!.reminderHour,
            initialMinute = prefs!!.reminderMinute,
            is24Hour = is24h
        )
        TimePickerDialog(
            onDismiss = { showTimePicker = false },
            onConfirm = {
                scope.launch {
                    repo.prefs.setReminderTime(timeState.hour, timeState.minute)
                    ReminderReceiver.scheduleFromContext(context)
                }
                showTimePicker = false
            },
            title = stringResource(R.string.time_title)
        ) {
            TimePicker(state = timeState)
        }
    }
    if (showThemePicker) {
        AlertDialog(
            onDismissRequest = { showThemePicker = false },
            title = { Text(stringResource(R.string.settings_theme_dialog_title)) },
            text = {
                Box(Modifier.fillMaxWidth()) {
                    Column(Modifier.align(Alignment.CenterStart)) {
                        listOf(
                            "system" to R.string.settings_theme_system,
                            "light" to R.string.settings_theme_light,
                            "dark" to R.string.settings_theme_dark,
                            "amoled" to R.string.settings_theme_amoled,
                            "spring" to R.string.settings_theme_spring,
                            "summer" to R.string.settings_theme_summer,
                            "autumn" to R.string.settings_theme_autumn,
                            "winter" to R.string.settings_theme_winter
                        ).forEach { (id, resId) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        scope.launch { repo.prefs.setTheme(id) }
                                        showThemePicker = false
                                    }
                                    .padding(vertical = 8.dp, horizontal = 4.dp)
                            ) {
                                Text(stringResource(resId), style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemePicker = false }) { Text(stringResource(R.string.close)) }
            }
        )
    }
    if (showLanguagePicker) {
        AlertDialog(
            onDismissRequest = { showLanguagePicker = false },
            title = { Text(stringResource(R.string.settings_language)) },
            text = {
                Box(Modifier.fillMaxWidth()) {
                    Column(Modifier.align(Alignment.CenterStart)) {
                        listOf(
                            null to R.string.settings_language_system,
                            "ru" to R.string.settings_language_russian,
                            "en" to R.string.settings_language_english
                        ).forEach { (code, resId) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        scope.launch {
                                            repo.prefs.setLanguage(code)
                                            val resolved = code ?: java.util.Locale.getDefault().language
                                            repo.retranslateOrRepickPending(resolved)
                                            (context as? android.app.Activity)?.recreate()
                                        }
                                        showLanguagePicker = false
                                    }
                                    .padding(vertical = 8.dp, horizontal = 4.dp)
                            ) {
                                Text(stringResource(resId), style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLanguagePicker = false }) { Text(stringResource(R.string.close)) }
            }
        )
    }
    if (showSecondReminderPicker && prefs != null) {
        val is24h2 = DateFormat.is24HourFormat(context)
        var enable by remember(prefs) { mutableStateOf(prefs!!.secondReminderHour != null) }
        val secondTimeState = rememberTimePickerState(
            initialHour = prefs!!.secondReminderHour ?: 20,
            initialMinute = prefs!!.secondReminderMinute ?: 0,
            is24Hour = is24h2
        )
        TimePickerDialog(
            onDismiss = { showSecondReminderPicker = false },
            onConfirm = {
                scope.launch {
                    repo.prefs.setSecondReminder(
                        if (enable) secondTimeState.hour else null,
                        if (enable) secondTimeState.minute else null
                    )
                    ReminderReceiver.scheduleFromContext(context)
                }
                showSecondReminderPicker = false
            },
            title = stringResource(R.string.time_second_title)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(checked = enable, onCheckedChange = { enable = it })
                    Text(stringResource(R.string.time_second_enable), Modifier.padding(start = 8.dp))
                }
                if (enable) TimePicker(state = secondTimeState)
            }
        }
    }
    if (showDifficultyPicker) {
        AlertDialog(
            onDismissRequest = { showDifficultyPicker = false },
            title = { Text(stringResource(R.string.settings_difficulty)) },
            text = {
                Box(Modifier.fillMaxWidth()) {
                    Column(Modifier.align(Alignment.CenterStart)) {
                        listOf(
                            null to R.string.settings_difficulty_any,
                            "easy" to R.string.settings_difficulty_easy,
                            "medium" to R.string.settings_difficulty_medium,
                            "hard" to R.string.settings_difficulty_hard
                        ).forEach { (id, resId) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        scope.launch { repo.prefs.setDifficulty(id) }
                                        showDifficultyPicker = false
                                    }
                                    .padding(vertical = 8.dp, horizontal = 4.dp)
                            ) {
                                Text(stringResource(resId), style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDifficultyPicker = false }) { Text(stringResource(R.string.close)) }
            }
        )
    }
    if (showAddCustomGoal && prefs != null) {
        val availableCategories = Challenges.categories.filter { it.id in prefs!!.selectedCategoryIds }
        AlertDialog(
            onDismissRequest = { showAddCustomGoal = false },
            title = { Text(stringResource(R.string.settings_custom_goal_dialog_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.settings_custom_goal_category), style = MaterialTheme.typography.labelMedium)
                    availableCategories.forEach { cat ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { addGoalCategoryId = cat.id }
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                categoryDisplayName(cat.id),
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (addGoalCategoryId == cat.id) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    Text(stringResource(R.string.settings_custom_goal_text), style = MaterialTheme.typography.labelMedium)
                    OutlinedTextField(
                        value = addGoalText,
                        onValueChange = { addGoalText = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = false,
                        maxLines = 3
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val catId = addGoalCategoryId
                        val text = addGoalText.trim()
                        if (catId != null && text.isNotBlank()) {
                            scope.launch {
                                val list = repo.prefs.getCustomGoals() + (catId to text)
                                repo.prefs.setCustomGoals(list)
                                customGoals = list
                            }
                            showAddCustomGoal = false
                            addGoalText = ""
                        }
                    }
                ) { Text(stringResource(R.string.settings_custom_goal_add_btn)) }
            },
            dismissButton = {
                TextButton(onClick = { showAddCustomGoal = false }) { Text(stringResource(R.string.close)) }
            }
        )
    }
    if (showEditCustomGoal && editGoalIndex >= 0) {
        AlertDialog(
            onDismissRequest = { showEditCustomGoal = false },
            title = { Text(stringResource(R.string.settings_edit_goal)) },
            text = {
                OutlinedTextField(
                    value = editGoalText,
                    onValueChange = { editGoalText = it },
                    label = { Text(stringResource(R.string.settings_custom_goal_text)) },
                    singleLine = false,
                    maxLines = 3
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (editGoalText.isNotBlank()) {
                        scope.launch {
                            val list = customGoals.toMutableList()
                            val (catId, _) = list[editGoalIndex]
                            list[editGoalIndex] = catId to editGoalText.trim()
                            repo.prefs.setCustomGoals(list)
                            customGoals = list
                        }
                        showEditCustomGoal = false
                    }
                }) { Text(stringResource(R.string.settings_save)) }
            },
            dismissButton = {
                TextButton(onClick = { showEditCustomGoal = false }) { Text(stringResource(R.string.settings_cancel)) }
            }
        )
    }
    if (showCloudLoadConfirm) {
        AlertDialog(
            onDismissRequest = { showCloudLoadConfirm = false },
            title = { Text(stringResource(R.string.settings_load_from_cloud)) },
            text = { Text(stringResource(R.string.settings_cloud_load_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    showCloudLoadConfirm = false
                    cloudLoading = true
                    cloudMessage = null
                    scope.launch {
                        sync?.loadProgress()
                            ?.onSuccess { cloudMessage = context.getString(R.string.settings_cloud_loaded) }
                            ?.onFailure { cloudMessage = context.getString(R.string.settings_cloud_error, it.message ?: "") }
                        cloudLoading = false
                    }
                }) { Text(stringResource(R.string.settings_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showCloudLoadConfirm = false }) { Text(stringResource(R.string.settings_cancel)) }
            }
        )
    }
    }

@Composable
private fun themeName(theme: String): String = when (theme) {
    "system" -> stringResource(R.string.settings_theme_system)
    "dark" -> stringResource(R.string.settings_theme_dark)
    "amoled" -> stringResource(R.string.settings_theme_amoled)
    "spring" -> stringResource(R.string.settings_theme_spring)
    "summer" -> stringResource(R.string.settings_theme_summer)
    "autumn" -> stringResource(R.string.settings_theme_autumn)
    "winter" -> stringResource(R.string.settings_theme_winter)
    else -> stringResource(R.string.settings_theme_light)
}

@Composable
private fun languageLabel(code: String?): String = when (code) {
    "ru" -> stringResource(R.string.settings_language_russian)
    "en" -> stringResource(R.string.settings_language_english)
    else -> stringResource(R.string.settings_language_system)
}

@Composable
private fun difficultyLabel(diff: String?): String = when (diff) {
    "easy" -> stringResource(R.string.settings_difficulty_easy)
    "medium" -> stringResource(R.string.settings_difficulty_medium)
    "hard" -> stringResource(R.string.settings_difficulty_hard)
    else -> stringResource(R.string.settings_difficulty_any)
}

@Composable
private fun TimePickerDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    title: String,
    content: @Composable () -> Unit
) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        androidx.compose.material3.Surface(
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.padding(top = 20.dp))
                content()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.close))
                    }
                    TextButton(onClick = onConfirm) {
                        Text(stringResource(R.string.time_done))
                    }
                }
            }
        }
    }
}

private fun formatTime(context: android.content.Context, hour: Int, minute: Int): String {
    val cal = java.util.Calendar.getInstance().apply {
        set(java.util.Calendar.HOUR_OF_DAY, hour)
        set(java.util.Calendar.MINUTE, minute)
    }
    return DateFormat.getTimeFormat(context).format(cal.time)
}

private fun shareTextFile(context: android.content.Context, filename: String, content: String, mimeType: String) {
    try {
        val dir = File(context.cacheDir, "export")
        dir.mkdirs()
        val file = File(dir, filename)
        file.writeText(content, Charsets.UTF_8)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, null))
    } catch (_: Exception) { }
}
