package com.dailychallenge.app

import android.app.AlarmManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.dailychallenge.app.ui.MainTabsScreen
import com.dailychallenge.app.ui.OnboardingScreen
import com.dailychallenge.app.ui.theme.DailyChallengeTheme
import com.dailychallenge.app.ui.AppViewModel
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        applySavedLocale()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestNotificationPermissionIfNeeded()
        requestExactAlarmPermissionIfNeeded()
        val initialTab = intent?.getIntExtra("open_tab", -1)?.takeIf { it >= 0 }
        setContent {
            val viewModel: AppViewModel = viewModel(
                factory = AppViewModel.Factory(applicationContext)
            )
            val prefs by viewModel.preferences.collectAsState(initial = null)
            val theme = prefs?.theme ?: "system"
            DailyChallengeTheme(theme = theme) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNav(
                        onboardingDone = prefs?.onboardingDone == true,
                        viewModel = viewModel,
                        initialTab = initialTab
                    )
                }
            }
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!NotificationManagerCompat.from(this).areNotificationsEnabled()) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 0)
            }
        }
    }

    private fun requestExactAlarmPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(AlarmManager::class.java)
            if (!alarmManager.canScheduleExactAlarms()) {
                try {
                    startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
                } catch (_: Exception) { }
            }
        }
    }

    private fun applySavedLocale() {
        val lang = applicationContext
            .getSharedPreferences("locale_prefs", android.content.Context.MODE_PRIVATE)
            .getString("lang", null) ?: return
        val locale = when (lang) {
            "ru" -> Locale("ru")
            "en" -> Locale.ENGLISH
            else -> return
        }
        Locale.setDefault(locale)
        val config = resources.configuration
        config.setLocale(locale)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocales(android.os.LocaleList(locale))
        }
        @Suppress("DEPRECATION")
        resources.updateConfiguration(config, resources.displayMetrics)
    }
}

@Composable
private fun AppNav(
    onboardingDone: Boolean,
    viewModel: AppViewModel,
    initialTab: Int? = null
) {
    val navController = rememberNavController()
    val startDestination = if (onboardingDone) "main" else "onboarding"

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable("onboarding") {
            OnboardingScreen(
                viewModel = viewModel,
                onFinish = {
                    navController.navigate("main") { popUpTo("onboarding") { inclusive = true } }
                }
            )
        }
        composable("main") {
            MainTabsScreen(viewModel = viewModel, initialTab = initialTab)
        }
    }
}
