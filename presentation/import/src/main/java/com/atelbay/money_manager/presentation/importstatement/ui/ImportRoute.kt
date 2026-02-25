package com.atelbay.money_manager.presentation.importstatement.ui

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.io.ByteArrayOutputStream

@Composable
fun ImportRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ImportViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val selectedAccountId by viewModel.selectedAccountId.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val pdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            if (bytes != null && bytes.isNotEmpty()) {
                viewModel.onPdfSelected(bytes)
            }
        }
    }

    val photoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview(),
    ) { bitmap ->
        if (bitmap != null) {
            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, stream)
            viewModel.onPhotoTaken(stream.toByteArray())
        }
    }

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
        onImport = viewModel::importTransactions,
        onReset = viewModel::reset,
        onBack = onBack,
        modifier = modifier,
    )
}
