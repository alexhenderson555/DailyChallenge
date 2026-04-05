package com.dailychallenge.app.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.dailychallenge.app.data.AppRepository
import com.dailychallenge.app.data.HistoryDataSource
import com.dailychallenge.app.data.PreferencesDataSource
import com.dailychallenge.app.data.UserPreferences
import com.dailychallenge.app.reminder.ReminderReceiver
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AppViewModel(
    private val repository: AppRepository
) : ViewModel() {

    val preferences = repository.preferences.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        UserPreferences()
    )

    fun finishOnboarding(reminderHour: Int, reminderMinute: Int, categoryIds: Set<String>) {
        viewModelScope.launch {
            repository.prefs.setReminderTime(reminderHour, reminderMinute)
            repository.prefs.setSelectedCategories(categoryIds)
            repository.prefs.setOnboardingDone(true)
        }
    }

    fun getRepository(): AppRepository = repository

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val prefs = PreferencesDataSource(context)
            val history = HistoryDataSource(context)
            val repo = AppRepository(prefs, history)
            return AppViewModel(repo) as T
        }
    }
}
