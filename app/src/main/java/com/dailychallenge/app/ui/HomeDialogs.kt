package com.dailychallenge.app.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.dailychallenge.app.R

@Composable
fun NoteDialog(
    noteText: String,
    onNoteTextChange: (String) -> Unit,
    onSave: () -> Unit,
    onSkip: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onSkip,
        title = { Text(stringResource(R.string.home_add_note)) },
        text = {
            OutlinedTextField(
                value = noteText,
                onValueChange = onNoteTextChange,
                label = { Text(stringResource(R.string.home_note_hint)) },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 4
            )
        },
        confirmButton = {
            TextButton(onClick = onSave) { Text(stringResource(R.string.home_note_save)) }
        },
        dismissButton = {
            TextButton(onClick = onSkip) { Text(stringResource(R.string.home_note_skip)) }
        }
    )
}

@Composable
fun FreezeConfirmDialog(
    freezeCount: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.home_use_freeze, freezeCount)) },
        text = { Text(stringResource(R.string.home_freeze_confirm)) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(stringResource(R.string.home_done)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) }
        }
    )
}
