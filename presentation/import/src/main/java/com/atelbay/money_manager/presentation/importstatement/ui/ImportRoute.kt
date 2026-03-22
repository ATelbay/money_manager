package com.atelbay.money_manager.presentation.importstatement.ui

import android.content.pm.ApplicationInfo
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.atelbay.money_manager.core.model.ImportState
import com.atelbay.money_manager.core.ui.theme.MoneyManagerTheme
import com.atelbay.money_manager.presentation.importstatement.ui.debug.DebugImportSheet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.io.ByteArrayOutputStream

@Composable
fun ImportRoute(
    onBack: () -> Unit,
    initialPdfUri: String? = null,
    modifier: Modifier = Modifier,
    viewModel: ImportViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val selectedAccountId by viewModel.selectedAccountId.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val strings = MoneyManagerTheme.strings

    val isDebug = remember {
        0 != (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE)
    }

    // Debug: toast for AI events
    LaunchedEffect(isDebug) {
        if (isDebug) {
            viewModel.debugAiEvent.collect { message ->
                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            }
        }
    }

    // Debug: bottom sheet with step-by-step progress
    var showDebugSheet by remember { mutableStateOf(false) }
    val debugEvents by viewModel.debugCollector.eventsFlow.collectAsStateWithLifecycle()

    // Auto-show debug sheet when parsing starts
    LaunchedEffect(state) {
        if (isDebug && state is ImportState.Parsing) {
            showDebugSheet = true
        }
    }

    if (isDebug && showDebugSheet && debugEvents.isNotEmpty()) {
        DebugImportSheet(
            events = debugEvents,
            onDismiss = { showDebugSheet = false },
        )
    }

    LaunchedEffect(initialPdfUri) {
        if (initialPdfUri != null) {
            val uri = Uri.parse(initialPdfUri)
            val bytes = withContext(Dispatchers.IO) {
                try {
                    context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                } catch (e: SecurityException) {
                    null // permission expired; user can pick manually
                }
            }
            if (bytes != null && bytes.isNotEmpty()) {
                viewModel.onPdfSelected(bytes, strings)
            }
        }
    }

    val coroutineScope = rememberCoroutineScope()

    val pdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                val bytes = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                }
                if (bytes != null && bytes.isNotEmpty()) {
                    viewModel.onPdfSelected(bytes, strings)
                }
            }
        }
    }

    val photoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview(),
    ) { bitmap ->
        if (bitmap != null) {
            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, stream)
            viewModel.onPhotoTaken(stream.toByteArray(), strings)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        ImportScreen(
            state = state,
            categories = categories,
            accounts = accounts,
            selectedAccountId = selectedAccountId,
            onAccountSelected = viewModel::selectAccount,
            onSelectPdf = { pdfLauncher.launch(arrayOf("application/pdf")) },
            onTakePhoto = { photoLauncher.launch(null) },
            onAmountChange = viewModel::updateAmount,
            onTypeChange = viewModel::updateType,
            onDetailsChange = viewModel::updateDetails,
            onDateChange = viewModel::updateDate,
            onCategoryChange = viewModel::updateCategory,
            onImport = { viewModel.importTransactions(strings) },
            onReset = viewModel::reset,
            onBack = onBack,
        )

        // Debug FAB to reopen the import log sheet
        if (isDebug && !showDebugSheet && debugEvents.isNotEmpty()) {
            FloatingActionButton(
                onClick = { showDebugSheet = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
                    .size(40.dp),
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                elevation = FloatingActionButtonDefaults.elevation(2.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.BugReport,
                    contentDescription = "Debug log",
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}
