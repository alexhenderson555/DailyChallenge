package com.dailychallenge.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.dailychallenge.app.R
import kotlinx.coroutines.launch

@Composable
fun PremiumScreen(
    isPremium: Boolean,
    onGetPremium: () -> Unit,
    onDismiss: () -> Unit,
    onRedeemPromoCode: suspend (String) -> Boolean = { false },
    onRestorePurchases: (() -> Unit)? = null,
    billingPrice: String? = null,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var promoCode by remember { mutableStateOf("") }
    var promoError by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.Star,
            contentDescription = null,
            modifier = Modifier.padding(8.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            if (isPremium) stringResource(R.string.premium_you_have) else stringResource(R.string.premium_title),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))
        if (!isPremium) {
            val benefits = listOf(
                R.string.premium_benefit_1,
                R.string.premium_benefit_2,
                R.string.premium_benefit_3,
                R.string.premium_benefit_4,
                R.string.premium_benefit_5,
                R.string.premium_benefit_6,
                R.string.premium_benefit_7,
                R.string.premium_benefit_8,
            )
            benefits.forEach { resId ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start
                ) {
                    Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.padding(8.dp))
                    Text(stringResource(resId), style = MaterialTheme.typography.bodyLarge)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = promoCode,
                onValueChange = { promoCode = it; promoError = null },
                label = { Text(stringResource(R.string.premium_promo_hint)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = promoError != null,
                supportingText = promoError?.let { { Text(it) } },
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = {
                    promoError = null
                    scope.launch {
                        val ok = onRedeemPromoCode(promoCode)
                        if (ok) {
                            onDismiss()
                        } else {
                            promoError = context.getString(R.string.premium_promo_invalid)
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = promoCode.isNotBlank()
            ) {
                Text(stringResource(R.string.premium_promo_activate))
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onGetPremium,
                modifier = Modifier.fillMaxWidth()
            ) {
                val label = if (billingPrice != null) {
                    stringResource(R.string.premium_get) + " — $billingPrice"
                } else {
                    stringResource(R.string.premium_get)
                }
                Text(label)
            }
            if (billingPrice == null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    stringResource(R.string.premium_test_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (onRestorePurchases != null) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onRestorePurchases,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.settings_restore_purchases))
                }
            }
        } else {
            Text(
                stringResource(R.string.premium_thanks),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.premium_close))
        }
    }
}
