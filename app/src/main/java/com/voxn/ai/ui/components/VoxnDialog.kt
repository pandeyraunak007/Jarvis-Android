package com.voxn.ai.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.voxn.ai.theme.VoxnColors
import com.voxn.ai.theme.VoxnFont

/**
 * Standard Voxn dialog with sticky header, scrollable body, and sticky action row.
 * Enforces consistent margins (20dp), max-height (640dp), and button styling (52dp / 14dp).
 */
@Composable
fun VoxnDialog(
    title: String,
    accent: Color = VoxnColors.cyan,
    onDismiss: () -> Unit,
    confirmLabel: String,
    onConfirm: () -> Unit,
    confirmEnabled: Boolean = true,
    cancelLabel: String = "CANCEL",
    maxHeight: androidx.compose.ui.unit.Dp = 640.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = VoxnColors.backgroundMid,
            border = BorderStroke(1.dp, accent.copy(alpha = 0.3f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 24.dp)
                .heightIn(max = maxHeight),
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(title, style = VoxnFont.mono(16, FontWeight.Bold), color = accent, letterSpacing = 2.sp)
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, null, tint = VoxnColors.textTertiary, modifier = Modifier.size(20.dp))
                    }
                }
                HorizontalDivider(color = VoxnColors.textTertiary.copy(alpha = 0.15f))

                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    content = content,
                )

                HorizontalDivider(color = VoxnColors.textTertiary.copy(alpha = 0.15f))
                VoxnDialogActions(
                    cancelLabel = cancelLabel,
                    onCancel = onDismiss,
                    confirmLabel = confirmLabel,
                    onConfirm = onConfirm,
                    confirmEnabled = confirmEnabled,
                    accent = accent,
                )
            }
        }
    }
}

@Composable
fun VoxnDialogActions(
    cancelLabel: String = "CANCEL",
    onCancel: () -> Unit,
    confirmLabel: String,
    onConfirm: () -> Unit,
    confirmEnabled: Boolean = true,
    accent: Color = VoxnColors.cyan,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        OutlinedButton(
            onClick = onCancel,
            modifier = Modifier.weight(1f).height(52.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = VoxnColors.textSecondary),
            border = BorderStroke(1.dp, VoxnColors.textTertiary.copy(alpha = 0.4f)),
            shape = RoundedCornerShape(14.dp),
        ) {
            Text(cancelLabel, style = VoxnFont.mono(13, FontWeight.Medium), letterSpacing = 1.sp, maxLines = 1)
        }
        Button(
            onClick = onConfirm,
            modifier = Modifier.weight(1f).height(52.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = accent,
                contentColor = VoxnColors.backgroundDark,
                disabledContainerColor = accent.copy(alpha = 0.3f),
                disabledContentColor = VoxnColors.backgroundDark.copy(alpha = 0.5f),
            ),
            enabled = confirmEnabled,
            shape = RoundedCornerShape(14.dp),
        ) {
            Text(confirmLabel, style = VoxnFont.mono(13, FontWeight.Bold), letterSpacing = 1.sp, maxLines = 1)
        }
    }
}
