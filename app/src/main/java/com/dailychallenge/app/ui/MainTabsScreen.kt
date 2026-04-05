package com.dailychallenge.app.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.StackedBarChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.dailychallenge.app.R
import com.dailychallenge.app.billing.BillingManager
import com.google.android.play.core.review.ReviewManagerFactory
import kotlinx.coroutines.launch

@Composable
fun MainTabsScreen(viewModel: AppViewModel, initialTab: Int? = null) {
    var selectedTab by remember { mutableIntStateOf(initialTab ?: 0) }
    var showPremiumOffer by remember { mutableStateOf(false) }
    val repo = viewModel.getRepository()
    val prefs by repo.preferences.collectAsState(initial = null)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val billingManager = remember {
        BillingManager(context) {
            repo.prefs.setPremium(true)
            showPremiumOffer = false
        }
    }
    var billingProductAvailable by remember { mutableStateOf(false) }
    var billingPrice by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        val details = billingManager.queryProduct()
        billingProductAvailable = details != null
        billingPrice = details?.oneTimePurchaseOfferDetails?.formattedPrice
    }

    DisposableEffect(Unit) {
        onDispose { billingManager.destroy() }
    }

    val records by repo.records.collectAsState(initial = emptyList())
    val currentStreak = remember(records) { repo.getCurrentStreak(records) }
    LaunchedEffect(currentStreak) {
        if (currentStreak >= 7) {
            val sp = context.getSharedPreferences("review_prefs", android.content.Context.MODE_PRIVATE)
            if (!sp.getBoolean("asked", false)) {
                sp.edit().putBoolean("asked", true).apply()
                val activity = context as? android.app.Activity ?: return@LaunchedEffect
                try {
                    val reviewManager = ReviewManagerFactory.create(context)
                    val request = reviewManager.requestReviewFlow()
                    request.addOnSuccessListener { reviewInfo ->
                        reviewManager.launchReviewFlow(activity, reviewInfo)
                    }
                } catch (_: Exception) { }
            }
        }
    }

    val onRequestPremium: () -> Unit = { showPremiumOffer = true }

    Scaffold(
        bottomBar = {
            NavigationBar(modifier = Modifier.fillMaxWidth()) {
                listOf(
                    R.string.tab_home to Icons.Default.Home,
                    R.string.tab_calendar to Icons.Default.CalendarMonth,
                    R.string.tab_stats to Icons.Default.StackedBarChart,
                    R.string.tab_settings to Icons.Default.Settings,
                ).forEachIndexed { index, (labelRes, icon) ->
                    NavigationBarItem(
                        modifier = Modifier.weight(1f),
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = { Icon(icon, contentDescription = stringResource(labelRes)) },
                        label = { Text(stringResource(labelRes)) }
                    )
                }
            }
        }
    ) { padding ->
        val tabSpec = tween<Float>(durationMillis = 300, easing = FastOutSlowInEasing)
        AnimatedContent(
            targetState = selectedTab,
            transitionSpec = {
                fadeIn(animationSpec = tabSpec) togetherWith fadeOut(animationSpec = tabSpec)
            },
            modifier = Modifier.fillMaxSize(),
            label = "tabs"
        ) { tab ->
            when (tab) {
                0 -> HomeScreen(repository = repo, onRequestPremium = onRequestPremium, modifier = Modifier.padding(padding))
                1 -> CalendarScreen(repository = repo, onRequestPremium = onRequestPremium, modifier = Modifier.padding(padding))
                2 -> StatsScreen(repository = repo, modifier = Modifier.padding(padding))
                3 -> SettingsScreen(viewModel = viewModel, onRequestPremium = onRequestPremium, modifier = Modifier.padding(padding))
                else -> HomeScreen(repository = repo, onRequestPremium = onRequestPremium, modifier = Modifier.padding(padding))
            }
        }
    }

    if (showPremiumOffer && prefs != null) {
        AlertDialog(
            onDismissRequest = { showPremiumOffer = false },
            title = { Text(stringResource(R.string.premium_title)) },
            text = {
                Column {
                    PremiumScreen(
                        isPremium = prefs!!.isPremium,
                        onGetPremium = {
                            if (billingProductAvailable) {
                                (context as? android.app.Activity)?.let { billingManager.launchPurchase(it) }
                            } else {
                                scope.launch { repo.prefs.setPremium(true) }
                                showPremiumOffer = false
                            }
                        },
                        onDismiss = { showPremiumOffer = false },
                        onRedeemPromoCode = { code -> repo.prefs.redeemPromoCode(code) },
                        onRestorePurchases = {
                            scope.launch {
                                val restored = billingManager.restorePurchases()
                                if (restored) showPremiumOffer = false
                            }
                        },
                        billingPrice = billingPrice
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showPremiumOffer = false }) { Text(stringResource(R.string.premium_close)) }
            }
        )
    }
}
