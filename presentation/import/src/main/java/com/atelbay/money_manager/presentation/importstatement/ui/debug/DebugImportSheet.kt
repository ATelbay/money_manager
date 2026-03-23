package com.atelbay.money_manager.presentation.importstatement.ui.debug

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.atelbay.money_manager.domain.importstatement.usecase.ImportStepEvent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugImportSheet(
    events: List<ImportStepEvent>,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val listState = rememberLazyListState()

    // Auto-scroll to latest event
    LaunchedEffect(events.size) {
        if (events.isNotEmpty()) {
            listState.animateScrollToItem(events.lastIndex)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp),
        ) {
            Text(
                text = "Import Debug Log",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 12.dp),
            )
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items(events) { event ->
                    val (icon, color, title, detail) = event.toDisplayInfo()
                    StepRow(icon, color, title, detail)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun StepRow(
    icon: ImageVector,
    color: Color,
    title: String,
    detail: String?,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = color,
            )
            if (detail != null) {
                Text(
                    text = detail,
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                )
            }
        }
    }
}

private data class DisplayInfo(
    val icon: ImageVector,
    val color: Color,
    val title: String,
    val detail: String?,
)

private val SuccessGreen = Color(0xFF4CAF50)
private val ErrorRed = Color(0xFFF44336)
private val InfoGray = Color(0xFF9E9E9E)
private val WarningAmber = Color(0xFFFFC107)

private fun ImportStepEvent.toDisplayInfo(): DisplayInfo = when (this) {
    is ImportStepEvent.PdfExtracted -> DisplayInfo(
        Icons.Default.Info, InfoGray,
        "PDF extracted", "$lineCount lines",
    )
    is ImportStepEvent.RegexConfigAttempt -> DisplayInfo(
        Icons.Default.Info, InfoGray,
        "Trying $source regex", bankId?.let { "Bank: $it" },
    )
    is ImportStepEvent.RegexConfigResult -> DisplayInfo(
        if (txCount > 0) Icons.Default.Check else Icons.Default.Close,
        if (txCount > 0) SuccessGreen else InfoGray,
        "$source: $txCount transactions", null,
    )
    is ImportStepEvent.AiConfigRequest -> DisplayInfo(
        Icons.Default.Info, WarningAmber,
        "AI config generation (attempt $attempt)", null,
    )
    is ImportStepEvent.AiConfigResponse -> DisplayInfo(
        Icons.Default.Check, SuccessGreen,
        "AI response (attempt $attempt)", "Bank: $bankId",
    )
    is ImportStepEvent.ValidationResult -> DisplayInfo(
        if (passed) Icons.Default.Check else Icons.Default.Close,
        if (passed) SuccessGreen else ErrorRed,
        "$check: ${if (passed) "PASS" else "FAIL"} (#$attempt)",
        detail,
    )
    is ImportStepEvent.AiConfigParseResult -> DisplayInfo(
        if (txCount > 0) Icons.Default.Check else Icons.Default.Close,
        if (txCount > 0) SuccessGreen else ErrorRed,
        "AI config parsed $txCount tx (attempt $attempt)", null,
    )
    is ImportStepEvent.FullAiParse -> DisplayInfo(
        Icons.Default.Info, if (enabled) WarningAmber else ErrorRed,
        if (enabled) "Falling back to full AI parse" else "Full AI parse disabled",
        null,
    )
    is ImportStepEvent.CategoryAssignment -> DisplayInfo(
        Icons.Default.Check, SuccessGreen,
        "Categories assigned", "$count transactions",
    )
    is ImportStepEvent.Deduplication -> DisplayInfo(
        Icons.Default.Info, InfoGray,
        "Deduplication", "$before total → $after new",
    )
    is ImportStepEvent.Complete -> DisplayInfo(
        Icons.Default.Check, SuccessGreen,
        "Complete ($method)", "$txCount new transactions",
    )
    is ImportStepEvent.Error -> DisplayInfo(
        Icons.Default.Close, ErrorRed,
        "Error", message,
    )
    is ImportStepEvent.TableExtracted -> DisplayInfo(
        Icons.Default.Info, InfoGray,
        "Table extracted", "$rowCount rows × $columnCount columns",
    )
    is ImportStepEvent.TableConfigAttempt -> DisplayInfo(
        Icons.Default.Info, InfoGray,
        "Trying $source table config", bankId?.let { "Bank: $it" },
    )
    is ImportStepEvent.TableConfigResult -> DisplayInfo(
        if (txCount > 0) Icons.Default.Check else Icons.Default.Close,
        if (txCount > 0) SuccessGreen else InfoGray,
        "$source table: $txCount transactions", null,
    )
    is ImportStepEvent.AiTableConfigRequest -> DisplayInfo(
        Icons.Default.Info, WarningAmber,
        "AI table config generation (attempt $attempt)", null,
    )
    is ImportStepEvent.AiTableConfigResponse -> DisplayInfo(
        Icons.Default.Check, SuccessGreen,
        "AI table response (attempt $attempt)", "Bank: $bankId",
    )
    is ImportStepEvent.AiTableConfigParseResult -> DisplayInfo(
        if (txCount > 0) Icons.Default.Check else Icons.Default.Close,
        if (txCount > 0) SuccessGreen else ErrorRed,
        "AI table config parsed $txCount tx (attempt $attempt)", null,
    )
}
