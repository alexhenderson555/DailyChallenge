package com.dailychallenge.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.dailychallenge.app.R

@Composable
fun categoryDisplayName(categoryId: String): String = stringResource(
    when (categoryId) {
        "health" -> R.string.category_health
        "productivity" -> R.string.category_productivity
        "relationships" -> R.string.category_relationships
        "creativity" -> R.string.category_creativity
        "order" -> R.string.category_order
        "finance" -> R.string.category_finance
        "learning" -> R.string.category_learning
        "sport" -> R.string.category_sport
        "mindfulness" -> R.string.category_mindfulness
        "cooking" -> R.string.category_cooking
        "nature" -> R.string.category_nature
        else -> R.string.category_health
    }
)
