package com.atelbay.money_manager.presentation.importstatement.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.atelbay.money_manager.core.model.Account
import com.atelbay.money_manager.core.model.Category
import com.atelbay.money_manager.core.model.ImportState
import com.atelbay.money_manager.presentation.importstatement.ui.components.ImportPreview

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportScreen(
    state: ImportState,
    categories: List<Category>,
    accounts: List<Account>,
    selectedAccountId: Long?,
    onAccountSelected: (Long) -> Unit,
    onSelectPdf: () -> Unit,
    onTakePhoto: () -> Unit,
    onAmountChange: (Int, Double) -> Unit,
    onTypeChange: (Int, com.atelbay.money_manager.core.model.TransactionType) -> Unit,
    onDetailsChange: (Int, String) -> Unit,
    onDateChange: (Int, kotlinx.datetime.LocalDate) -> Unit,
    onCategoryChange: (Int, Long) -> Unit,
    onImport: () -> Unit,
    onReset: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.testTag("import:screen"),
        topBar = {
            TopAppBar(
                title = { Text("Импорт выписки") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Назад",
                        )
                    }
                },
            )
        },
    ) { padding ->
        when (state) {
            is ImportState.Idle,
            is ImportState.SelectingFile,
            -> {
                IdleContent(
                    onSelectPdf = onSelectPdf,
                    onTakePhoto = onTakePhoto,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                )
            }

            is ImportState.Parsing -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(
                            modifier = Modifier.testTag("import:loading"),
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Анализ выписки...",
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
            }

            is ImportState.Preview -> {
                ImportPreview(
                    result = state.result,
                    overrides = state.overrides,
                    categories = categories,
                    accounts = accounts,
                    selectedAccountId = selectedAccountId,
                    onAccountSelected = onAccountSelected,
                    onAmountChange = onAmountChange,
                    onTypeChange = onTypeChange,
                    onDetailsChange = onDetailsChange,
                    onDateChange = onDateChange,
                    onCategoryChange = onCategoryChange,
                    onImport = onImport,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                )
            }

            is ImportState.Importing -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Импорт транзакций...",
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
            }

            is ImportState.Success -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp),
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Импортировано: ${state.imported}",
                            style = MaterialTheme.typography.headlineSmall,
                            modifier = Modifier.testTag("import:successCount"),
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(onClick = onBack) {
                            Text("Готово")
                        }
                    }
                }
            }

            is ImportState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp),
                    ) {
                        Icon(
                            Icons.Default.Error,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.error,
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.testTag("import:errorMessage"),
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(onClick = onReset) {
                            Text("Попробовать снова")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun IdleContent(
    onSelectPdf: () -> Unit,
    onTakePhoto: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp),
        ) {
            Text(
                text = "Выберите способ импорта",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onSelectPdf,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("import:selectPdf"),
            ) {
                Icon(
                    Icons.Default.UploadFile,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text("Выбрать PDF")
            }
            OutlinedButton(
                onClick = onTakePhoto,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("import:takePhoto"),
            ) {
                Icon(
                    Icons.Default.CameraAlt,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text("Сделать фото")
            }
        }
    }
}
