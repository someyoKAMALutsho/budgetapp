package com.gorib.app.ui.screens.home

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gorib.app.domain.model.Transaction
import com.gorib.app.ui.components.AddTransactionBottomSheet
import com.gorib.app.ui.components.TransactionDetailBottomSheet
import kotlinx.coroutines.launch
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.activity.ComponentActivity
import com.gorib.app.ui.screens.groceries.ReceiptReviewViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToCategory: (Long) -> Unit,
    onNavigateToRentSettings: () -> Unit,
    onNavigateToUtilities: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToReceiptReview: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val displayMonth by viewModel.displayMonth.collectAsState()
    val canGoForward by viewModel.canGoForward.collectAsState()
    val unpaidUtilitiesCount by viewModel.unpaidUtilitiesCount.collectAsState()
    val budgetAlerts by viewModel.budgetAlerts.collectAsState()
    val upcomingRecurring by viewModel.upcomingRecurring.collectAsState()
    var showAddTransactionSheet by remember { mutableStateOf(false) }

    var selectedTransactionForDetail by remember { mutableStateOf<Transaction?>(null) }
    var editingTransaction by remember { mutableStateOf<Transaction?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val context = LocalContext.current
    var photoUri by remember { mutableStateOf<Uri?>(null) }

    val receiptReviewViewModel: ReceiptReviewViewModel = hiltViewModel(context as ComponentActivity)
    var isOcrScanning by remember { mutableStateOf(false) }
    var showOcrChoiceDialog by remember { mutableStateOf(false) }

    val realReceiptPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val uri = result.data?.data
            if (uri != null) {
                isOcrScanning = true
                coroutineScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                    try {
                        val geminiApiKey = uiState.geminiApiKey
                        if (geminiApiKey.isNotBlank()) {
                            // Advanced Gemini Vision Parsing
                            val bitmap = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                                val source = android.graphics.ImageDecoder.createSource(context.contentResolver, uri)
                                android.graphics.ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                                    decoder.allocator = android.graphics.ImageDecoder.ALLOCATOR_SOFTWARE
                                }
                            } else {
                                @Suppress("DEPRECATION")
                                android.provider.MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                            }
                            val parsed = com.gorib.app.domain.model.GeminiReceiptParser.parseReceipt(bitmap, geminiApiKey)
                            
                            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                if (parsed != null && parsed.items.isNotEmpty()) {
                                    receiptReviewViewModel.loadParsedReceipt(
                                        receipt = parsed,
                                        sessionId = null,
                                        rawText = "Extracted by Gemini 2.5 Flash AI — ${parsed.items.size} items"
                                    )
                                    isOcrScanning = false
                                    onNavigateToReceiptReview()
                                } else {
                                    isOcrScanning = false
                                    android.widget.Toast.makeText(context, "AI could not read items. Try a clearer photo.", android.widget.Toast.LENGTH_LONG).show()
                                }
                            }
                        } else {
                            // Fallback to local ML Kit Text Recognition
                            val image = com.google.mlkit.vision.common.InputImage.fromFilePath(context, uri)
                            val recognizer = com.google.mlkit.vision.text.TextRecognition.getClient(
                                com.google.mlkit.vision.text.latin.TextRecognizerOptions.DEFAULT_OPTIONS
                            )
                            recognizer.process(image)
                                .addOnSuccessListener { visionText ->
                                    val rawOcrText = visionText.text
                                    val parsed = com.gorib.app.domain.model.OcrReceiptParser.parse(rawOcrText)
                                    receiptReviewViewModel.loadParsedReceipt(
                                        receipt = parsed,
                                        sessionId = null,
                                        rawText = rawOcrText
                                    )
                                    isOcrScanning = false
                                    onNavigateToReceiptReview()
                                }
                                .addOnFailureListener { e ->
                                    e.printStackTrace()
                                    isOcrScanning = false
                                    android.widget.Toast.makeText(context, "OCR failed: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                                }
                        }
                    } catch (e: Exception) {
                        isOcrScanning = false
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                            android.widget.Toast.makeText(context, "Scan error: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }
    }

    val realReceiptCameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && photoUri != null) {
            isOcrScanning = true
            coroutineScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                try {
                    val geminiApiKey = uiState.geminiApiKey
                    if (geminiApiKey.isNotBlank()) {
                        // Advanced Gemini Vision Parsing
                        val bitmap = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                            val source = android.graphics.ImageDecoder.createSource(context.contentResolver, photoUri!!)
                            android.graphics.ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                                decoder.allocator = android.graphics.ImageDecoder.ALLOCATOR_SOFTWARE
                            }
                        } else {
                            @Suppress("DEPRECATION")
                            android.provider.MediaStore.Images.Media.getBitmap(context.contentResolver, photoUri!!)
                        }
                        val parsed = com.gorib.app.domain.model.GeminiReceiptParser.parseReceipt(bitmap, geminiApiKey)
                        
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                            if (parsed != null && parsed.items.isNotEmpty()) {
                                receiptReviewViewModel.loadParsedReceipt(
                                    receipt = parsed,
                                    sessionId = null,
                                    rawText = "Extracted by Gemini 2.5 Flash AI — ${parsed.items.size} items"
                                )
                                isOcrScanning = false
                                onNavigateToReceiptReview()
                            } else {
                                isOcrScanning = false
                                android.widget.Toast.makeText(context, "AI could not read items. Try a clearer photo.", android.widget.Toast.LENGTH_LONG).show()
                            }
                        }
                    } else {
                        // Fallback to local ML Kit Text Recognition
                        val image = com.google.mlkit.vision.common.InputImage.fromFilePath(context, photoUri!!)
                        val recognizer = com.google.mlkit.vision.text.TextRecognition.getClient(
                            com.google.mlkit.vision.text.latin.TextRecognizerOptions.DEFAULT_OPTIONS
                        )
                        recognizer.process(image)
                            .addOnSuccessListener { visionText ->
                                val rawOcrText = visionText.text
                                  val parsed = com.gorib.app.domain.model.OcrReceiptParser.parse(rawOcrText)
                                receiptReviewViewModel.loadParsedReceipt(
                                    receipt = parsed,
                                    sessionId = null,
                                    rawText = rawOcrText
                                )
                                isOcrScanning = false
                                onNavigateToReceiptReview()
                            }
                            .addOnFailureListener { e ->
                                e.printStackTrace()
                                isOcrScanning = false
                                android.widget.Toast.makeText(context, "OCR failed: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                            }
                    }
                } catch (e: Exception) {
                    isOcrScanning = false
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        android.widget.Toast.makeText(context, "Scan error: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    val launchRealCamera = {
        try {
            val tempFile = java.io.File.createTempFile("captured_receipt_real_", ".jpg", context.cacheDir).apply {
                createNewFile()
                deleteOnExit()
            }
            val uri = androidx.core.content.FileProvider.getUriForFile(
                context,
                "com.gorib.app.provider",
                tempFile
            )
            photoUri = uri
            realReceiptCameraLauncher.launch(uri)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    val homeReceiptPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val uri = result.data?.data
            uri?.toString()?.let { viewModel.runOcrEngineWithImage(it) }
        }
    }

    val homeCameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            photoUri?.toString()?.let { viewModel.runOcrEngineWithImage(it) }
        }
    }

    val takePhoto = {
        try {
            val tempFile = java.io.File.createTempFile("captured_receipt_", ".jpg", context.cacheDir).apply {
                createNewFile()
                deleteOnExit()
            }
            val uri = androidx.core.content.FileProvider.getUriForFile(
                context,
                "com.gorib.app.provider",
                tempFile
            )
            photoUri = uri
            homeCameraLauncher.launch(uri)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 90.dp)
        ) {
            // 1. Premium Header Block (Teal Gradient Accent)
            item {
                val availableBudget = uiState.totalIncomeMonth - uiState.totalExpenseMonth
                HomeHeader(
                    netWorth = availableBudget,
                    displayMonth = displayMonth,
                    onPrevMonth = { viewModel.changeMonth(-1) },
                    onNextMonth = { viewModel.changeMonth(1) },
                    canGoForward = canGoForward
                )
            }

            // 2. Unpaid Bill Alerts (Live notifications on Dashboard)
            if (uiState.unpaidRentAmount > 0.0) {
                item {
                    AlertBanner(
                        title = "Unpaid Rent Configured",
                        description = "RM ${String.format("%,.2f", uiState.unpaidRentAmount)} rent is pending payment.",
                        badgeText = "Rent",
                        color = Color(0xFFE65100),
                        backgroundColor = Color(0xFFFFF3E0),
                        onClick = onNavigateToRentSettings
                    )
                }
            }

            if (unpaidUtilitiesCount > 0) {
                item {
                    // Utilities unpaid banner
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                            .clickable { onNavigateToUtilities() },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        ),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("⚡", fontSize = 20.sp)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "$unpaidUtilitiesCount unpaid utility bill(s)",
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    "Tap to view and mark as paid",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                                )
                            }
                            Icon(Icons.Default.KeyboardArrowRight, contentDescription = null)
                        }
                    }
                }
            }

            // 2b. Critical Budget Alerts
            val criticalAlerts = budgetAlerts.filter {
                it.alertLevel == com.gorib.app.domain.usecase.AlertLevel.CRITICAL
            }
            if (criticalAlerts.isNotEmpty()) {
                items(criticalAlerts) { alert ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 3.dp)
                            .clickable {
                                onNavigateToCategory(alert.categoryId)
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(alert.iconEmoji, fontSize = 20.sp)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "${alert.categoryName} budget exceeded",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Text(
                                    "RM ${"%.2f".format(alert.spent)} spent of RM ${"%.2f".format(alert.budget)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer.copy(0.7f)
                                )
                            }
                            Text(
                                "${"%.0f".format(alert.percentUsed)}%",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }

            // 3. Flat Border-Based Financial Tallies
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    TallyCard(
                        title = "Income this Month",
                        amount = uiState.totalIncomeMonth,
                        isExpense = false,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                    TallyCard(
                        title = "Expenses this Month",
                        amount = uiState.totalExpenseMonth,
                        isExpense = true,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // 4. Horizontal Categories Overviews Scroll
            if (uiState.categories.isNotEmpty()) {
                item {
                    SectionHeader("Category Overviews")
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    ) {
                        items(uiState.categories) { category ->
                            val spent = uiState.categorySpent[category.id] ?: 0.0
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                shape = MaterialTheme.shapes.medium,
                                modifier = Modifier
                                    .width(145.dp)
                                    .clickable { onNavigateToCategory(category.id) }
                                    .border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.medium)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "${category.iconEmoji} ${category.name}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Spent this month",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "RM ${String.format("%,.2f", spent)}",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 15.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 5. OCR FALLBACK SIMULATOR ACTION BANNER
            item {
                OcrSimulationBanner { showOcrChoiceDialog = true }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // 5b. Upcoming Recurring Expenses
            if (upcomingRecurring.isNotEmpty()) {
                item {
                    Text(
                        "Coming Up",
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                items(upcomingRecurring) { r ->
                    val daysUntil = r.dueDay - java.time.LocalDate.now().dayOfMonth
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(r.iconEmoji, fontSize = 24.sp)
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    r.name, 
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    if (daysUntil == 0) "Due today"
                                    else "Due in $daysUntil day${if (daysUntil > 1) "s" else ""}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (daysUntil == 0)
                                        MaterialTheme.colorScheme.error
                                    else
                                        MaterialTheme.colorScheme.onSurface.copy(0.6f),
                                    fontWeight = if (daysUntil == 0) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                            Text(
                                "RM ${"%.2f".format(r.amountRm)}",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            // 6. Recent Activity / Transactions Section
            item {
                SectionHeader("Recent Activity", actionText = "See All", onActionClick = onNavigateToHistory)
            }

            if (uiState.recentTransactions.isEmpty()) {
                item {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 12.dp)
                            .clip(MaterialTheme.shapes.medium)
                            .background(MaterialTheme.colorScheme.surface)
                            .border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.medium)
                            .padding(24.dp)
                    ) {
                        Text(
                            text = "No recent transactions found.\nRun the OCR scanner above to simulate an entry!",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            } else {
                items(uiState.recentTransactions) { transaction ->
                    val cat = uiState.categories.find { it.id == transaction.categoryId }
                    TransactionListItem(
                        transaction = transaction,
                        categoryEmoji = cat?.iconEmoji ?: "✏️",
                        categoryName = cat?.name ?: "General",
                        onRowClick = { selectedTransactionForDetail = transaction }
                    )
                }
            }
        }

        // Floating Action Button to Add Transaction
        FloatingActionButton(
            onClick = { showAddTransactionSheet = true },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Transaction")
        }

        // Reusable Custom Add Transaction Bottom Sheet
        if (showAddTransactionSheet) {
            AddTransactionBottomSheet(
                onDismiss = { showAddTransactionSheet = false },
                onSaveSuccess = {
                    viewModel.showNotification("Expense added ✓")
                },
                onNavigateToReceiptReview = onNavigateToReceiptReview
            )
        }

        if (editingTransaction != null) {
            AddTransactionBottomSheet(
                existingTransaction = editingTransaction,
                onDismiss = { editingTransaction = null },
                onSaveSuccess = {
                    viewModel.showNotification("Expense updated ✓")
                },
                onNavigateToReceiptReview = onNavigateToReceiptReview
            )
        }

        selectedTransactionForDetail?.let { transaction ->
            val cat = uiState.categories.find { it.id == transaction.categoryId }
            TransactionDetailBottomSheet(
                transaction = transaction,
                categoryEmoji = cat?.iconEmoji ?: "✏️",
                categoryName = cat?.name ?: "General",
                onDismiss = { selectedTransactionForDetail = null },
                onEditClick = { tx ->
                    editingTransaction = tx
                },
                onDeleteClick = { tx ->
                    viewModel.deleteTransaction(tx) { restore ->
                        coroutineScope.launch {
                            val result = snackbarHostState.showSnackbar(
                                message = "Deleted '${tx.title}'",
                                actionLabel = "Undo",
                                duration = SnackbarDuration.Short
                            )
                            if (result == SnackbarResult.ActionPerformed) {
                                restore()
                            }
                        }
                    }
                }
            )
        }

        // Floating Toast Notification
        AnimatedVisibility(
            visible = uiState.showNotification != null,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 16.dp, start = 16.dp, end = 16.dp)
        ) {
            uiState.showNotification?.let { msg ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    shape = MaterialTheme.shapes.medium,
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    modifier = Modifier.clickable { viewModel.dismissNotification() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Check", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = msg,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }

        // OCR FALLBACK SIMULATOR DIALOG
        if (uiState.showOcrDialog) {
            OcrFallbackDialog(
                uiState = uiState,
                onClose = { viewModel.onCloseOcrDialog() },
                onInputChange = { viewModel.onOcrInputTextChanged(it) },
                onRunScan = { viewModel.runOcrEngine() },
                onScanImage = { viewModel.runOcrEngineWithImage(it) },
                onPickImage = {
                    val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                    homeReceiptPickerLauncher.launch(intent)
                },
                onTakePhoto = { takePhoto() },
                onConfirmTransaction = { title, amt, catId, isFallback ->
                    viewModel.saveOcrTransaction(title, amt, catId, isFallback)
                }
            )
        }

        // Real OCR Scanner Picker/Camera Choice Dialog
        if (showOcrChoiceDialog) {
            AlertDialog(
                onDismissRequest = { showOcrChoiceDialog = false },
                shape = RoundedCornerShape(24.dp),
                containerColor = MaterialTheme.colorScheme.surface,
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "AI Receipt OCR Scanner ⚡",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        IconButton(onClick = { showOcrChoiceDialog = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }
                },
                text = {
                    Text(
                        text = "Scan a shopping receipt to automatically extract items, quantities, prices, and split them seamlessly.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showOcrChoiceDialog = false
                            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                            realReceiptPickerLauncher.launch(intent)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("📎 Choose from Gallery", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    Button(
                        onClick = {
                            showOcrChoiceDialog = false
                            launchRealCamera()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .padding(top = 8.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    ) {
                        Text("📷 Take Photo", fontWeight = FontWeight.Bold)
                    }
                }
            )
        }

        // Fullscreen Scanning Progress overlay
        if (isOcrScanning) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable(enabled = false) {},
                contentAlignment = Alignment.Center
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Extracting receipt items...",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        // New User Gemini API Onboarding popup
        if (uiState.showGeminiApiKeyIntroDialog) {
            GeminiApiKeyIntroDialog(
                onDismiss = { viewModel.dismissGeminiIntro() },
                onNavigateToSettings = onNavigateToSettings
            )
        }
    }
}
}

@Composable
fun GeminiApiKeyIntroDialog(
    onDismiss: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Premium AI Icon
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.secondary
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🧠", fontSize = 28.sp)
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Welcome to GORIB AI! ✨",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(horizontal = 8.dp)
            ) {
                Text(
                    text = "Unlock premium, lightning-fast receipt OCR by setting up your own free Gemini API key in 4 simple steps:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                
                // Steps
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StepItem(
                            stepNumber = "1",
                            emoji = "🌐",
                            text = "Tap 'Get API Key' to open Google AI Studio"
                        )
                        StepItem(
                            stepNumber = "2",
                            emoji = "🚀",
                            text = "Tap 'Get started' and create your API key"
                        )
                        StepItem(
                            stepNumber = "3",
                            emoji = "📋",
                            text = "Copy the newly created API key"
                        )
                        StepItem(
                            stepNumber = "4",
                            emoji = "⚙️",
                            text = "Go to Settings -> Gemini Vision API and paste it there!"
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://aistudio.google.com/app/apikey"))
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text("Get API Key 🌐", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        onDismiss()
                        onNavigateToSettings()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Text("Configure in Settings ⚙️", fontWeight = FontWeight.Bold)
                }
                
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Text("Maybe Later", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    )
}

@Composable
fun StepItem(stepNumber: String, emoji: String, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        // Step bubble
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stepNumber,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Text(text = emoji, fontSize = 16.sp)
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
    }
}

// --- SUB-COMPONENTS ---

@Composable
fun HomeHeader(
    netWorth: Double,
    displayMonth: String,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    canGoForward: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(230.dp)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                        Color.Transparent
                    )
                )
            )
            .padding(horizontal = 24.dp, vertical = 20.dp)
    ) {
        Column(modifier = Modifier.align(Alignment.BottomStart)) {
            // Month Selector Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                IconButton(
                    onClick = onPrevMonth,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowLeft,
                        contentDescription = "Previous Month",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    text = displayMonth,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(
                    onClick = onNextMonth,
                    enabled = canGoForward,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowRight,
                        contentDescription = "Next Month",
                        tint = if (canGoForward)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                    )
                }
            }

            Text(
                text = "Welcome to GORIB",
                style = MaterialTheme.typography.titleMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Available Budget",
                style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
            )
            Row(
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier.padding(top = 4.dp)
            ) {
                Text(
                    text = "RM ",
                    style = MaterialTheme.typography.displaySmall.copy(
                        color = MaterialTheme.colorScheme.primary, 
                        fontWeight = FontWeight.Black
                    )
                )
                Text(
                    text = String.format("%,.2f", netWorth),
                    style = MaterialTheme.typography.displayLarge.copy(
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Black
                    )
                )
            }
        }
        
        // Premium Brand icon at top right
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), CircleShape)
                .align(Alignment.TopEnd),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "G", 
                color = MaterialTheme.colorScheme.primary, 
                fontWeight = FontWeight.Black, 
                fontSize = 20.sp
            )
        }
    }
}

@Composable
fun AlertBanner(
    title: String,
    description: String,
    badgeText: String,
    color: Color,
    backgroundColor: Color,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 6.dp)
            .clickable { onClick() }
            .border(1.dp, color.copy(alpha = 0.3f), MaterialTheme.shapes.medium)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(color)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = badgeText,
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall.copy(
                            color = Color.Black,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall.copy(color = Color.DarkGray)
                )
            }
            Icon(
                Icons.Default.KeyboardArrowRight, 
                contentDescription = "Go", 
                tint = color,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun SectionHeader(title: String, actionText: String? = null, onActionClick: (() -> Unit)? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(
                color = MaterialTheme.colorScheme.onBackground, 
                fontWeight = FontWeight.Bold
            )
        )
        if (actionText != null && onActionClick != null) {
            Text(
                text = actionText,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                ),
                modifier = Modifier.clickable { onActionClick() }
            )
        }
    }
}

@Composable
fun TallyCard(title: String, amount: Double, isExpense: Boolean, color: Color, modifier: Modifier = Modifier) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = MaterialTheme.shapes.medium,
        modifier = modifier
            .border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.medium)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (isExpense) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = title,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "RM ${String.format("%,.2f", amount)}",
                style = MaterialTheme.typography.titleMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun OcrSimulationBanner(onClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .clickable { onClick() }
            .border(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f), MaterialTheme.shapes.medium)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.05f),
                            Color.Transparent
                        )
                    )
                )
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Send, contentDescription = "OCR Scanner", tint = MaterialTheme.colorScheme.secondary)
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "AI Receipt OCR Scanner",
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    text = "Simulate Malaysian Ringgit receipt scans to experience OCR Fallback Rules",
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )
            }
            
            Icon(Icons.Default.PlayArrow, contentDescription = "Simulate", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun TransactionListItem(
    transaction: Transaction,
    categoryEmoji: String,
    categoryName: String,
    onRowClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 6.dp)
            .border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.medium)
            .clickable { onRowClick() }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(categoryEmoji, fontSize = 18.sp)
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = transaction.title,
                        style = MaterialTheme.typography.titleSmall.copy(
                            color = MaterialTheme.colorScheme.onSurface, 
                            fontWeight = FontWeight.Bold
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (transaction.isOcrProcessed) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(
                                        if (transaction.ocrStatus == "FAILED") MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f) 
                                        else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                    )
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (transaction.ocrStatus == "FAILED") "OCR Option B" else "OCR Option C",
                                    color = if (transaction.ocrStatus == "FAILED") MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        
                        Text(
                            text = "$categoryName • ${com.gorib.app.ui.utils.formatRelativeDate(transaction.loggedAt)}",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                    }
                }
            }
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (transaction.receiptPath != null) {
                    Text("📎", fontSize = 14.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                }
                
                Text(
                    text = "${if (transaction.type == "EXPENSE") "-" else "+"} RM ${String.format("%,.2f", transaction.amount)}",
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = if (transaction.type == "EXPENSE") MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Black
                    )
                )
            }
        }
    }
}

@Composable
fun OcrFallbackDialog(
    uiState: HomeUiState,
    onClose: () -> Unit,
    onInputChange: (String) -> Unit,
    onRunScan: () -> Unit,
    onScanImage: (String) -> Unit,
    onPickImage: () -> Unit,
    onTakePhoto: () -> Unit,
    onConfirmTransaction: (title: String, amount: Double, categoryId: Long, isFallback: Boolean) -> Unit
) {
    var editTitle by remember { mutableStateOf("") }
    var editAmount by remember { mutableStateOf("") }
    var selectedCategoryId by remember { mutableStateOf(1L) }

    LaunchedEffect(uiState.currentOcrStep) {
        if (uiState.currentOcrStep == "OPTION_C") {
            editTitle = uiState.ocrResult?.title ?: ""
            editAmount = uiState.ocrResult?.amount?.toString() ?: ""
            selectedCategoryId = uiState.ocrResult?.categorySuggestedId ?: 1L
        } else if (uiState.currentOcrStep == "OPTION_B") {
            editTitle = ""
            editAmount = ""
            selectedCategoryId = 1L
        }
    }

    AlertDialog(
        onDismissRequest = onClose,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clip(MaterialTheme.shapes.large)
            .border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.large),
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "AI OCR Fallback Rule Simulator", 
                    color = MaterialTheme.colorScheme.onSurface, 
                    fontSize = 18.sp, 
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when (uiState.currentOcrStep) {
                    "IDLE" -> {
                        Text(
                            text = "Choose a sample receipt below to run the OCR engine, or type custom receipt text.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = onTakePhoto,
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Add, contentDescription = "Camera", tint = Color.White)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("📷 Take Photo", fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }

                            Button(
                                onClick = onPickImage,
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Share, contentDescription = "Gallery", tint = Color.Black)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("📎 Pick Image", fontWeight = FontWeight.Bold, color = Color.Black)
                                }
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onInputChange("JAYA GROCER SDN BHD\n17-05-2026\n---------------------\nAPPLE BAG : RM 12.00\nORGANIC MILK : RM 18.50\nTOTAL AMOUNT DUE : RM 30.50\nCASH TENDERED : RM 50.00\nTHANK YOU FOR SHOPPING WITH US")
                                    }
                                    .border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.medium)
                            ) {
                                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = "Sample A", tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text("Receipt Sample A (Option C Flow)", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                                        Text("Clear receipt texts. Extracts RM 30.50, merchant Jaya Grocer.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }

                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onInputChange("blurry-receipt-unrecognizable-shadows-spots.jpeg [No OCR parseable texts found in raw scan result buffers]")
                                    }
                                    .border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.medium)
                            ) {
                                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Warning, contentDescription = "Sample B", tint = MaterialTheme.colorScheme.secondary)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text("Receipt Sample B (Option B Fallback)", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                                        Text("Blurry text. Fails OCR extraction and skips to manual entry.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }

                        OutlinedTextField(
                            value = uiState.ocrRawInputText,
                            onValueChange = onInputChange,
                            label = { Text("Receipt Raw Text Scanned", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                            placeholder = { Text("Click a sample above or write custom receipt information...", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            ),
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                        )
                    }

                    "SCANNING" -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp)
                        ) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Running GORIB OCR Parse Algorithms...", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
                            Text("Extracting currency, merchants, and categories", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                        }
                    }

                    "OPTION_C" -> {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(MaterialTheme.shapes.medium)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), MaterialTheme.shapes.medium)
                                    .padding(12.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Check, contentDescription = "Success", tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text("OCR Success: Step 1 Option C Triggered", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                        Text("Extracts partial details. Confirm or fill in the blanks.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = editTitle,
                                onValueChange = { editTitle = it },
                                label = { Text("Merchant / Title", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = MaterialTheme.colorScheme.onSurface, unfocusedTextColor = MaterialTheme.colorScheme.onSurface, focusedBorderColor = MaterialTheme.colorScheme.primary, unfocusedBorderColor = MaterialTheme.colorScheme.outline),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = editAmount,
                                onValueChange = { editAmount = it },
                                label = { Text("Amount (RM)", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = MaterialTheme.colorScheme.onSurface, unfocusedTextColor = MaterialTheme.colorScheme.onSurface, focusedBorderColor = MaterialTheme.colorScheme.primary, unfocusedBorderColor = MaterialTheme.colorScheme.outline),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            val gapsInfo = uiState.ocrResult?.gaps ?: emptyList()
                            if (gapsInfo.isNotEmpty()) {
                                Text(
                                    text = "Missing/Gap Fields Filled: ${gapsInfo.joinToString(", ")}",
                                    color = MaterialTheme.colorScheme.secondary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    "OPTION_B" -> {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(MaterialTheme.shapes.medium)
                                    .background(MaterialTheme.colorScheme.error.copy(alpha = 0.08f))
                                    .border(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f), MaterialTheme.shapes.medium)
                                    .padding(12.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Warning, contentDescription = "Failed", tint = MaterialTheme.colorScheme.error)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text("OCR Failed: Skipping to Option B", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                        Text("No parseable text detected. Complete fully manual entry.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = editTitle,
                                onValueChange = { editTitle = it },
                                label = { Text("Enter Merchant Title (Required)", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = MaterialTheme.colorScheme.onSurface, unfocusedTextColor = MaterialTheme.colorScheme.onSurface, focusedBorderColor = MaterialTheme.colorScheme.primary, unfocusedBorderColor = MaterialTheme.colorScheme.outline),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = editAmount,
                                onValueChange = { editAmount = it },
                                label = { Text("Enter Amount (RM) (Required)", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = MaterialTheme.colorScheme.onSurface, unfocusedTextColor = MaterialTheme.colorScheme.onSurface, focusedBorderColor = MaterialTheme.colorScheme.primary, unfocusedBorderColor = MaterialTheme.colorScheme.outline),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (uiState.currentOcrStep == "IDLE") {
                    Button(
                        onClick = onRunScan,
                        enabled = uiState.ocrRawInputText.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("RUN AI OCR", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                    }
                } else if (uiState.currentOcrStep == "OPTION_C" || uiState.currentOcrStep == "OPTION_B") {
                    Button(
                        onClick = {
                            val amt = editAmount.toDoubleOrNull() ?: 0.0
                            if (editTitle.isNotBlank() && amt > 0.0) {
                                onConfirmTransaction(
                                    editTitle, 
                                    amt, 
                                    selectedCategoryId, 
                                    uiState.currentOcrStep == "OPTION_B"
                                )
                            }
                        },
                        enabled = editTitle.isNotBlank() && editAmount.toDoubleOrNull() != null && (editAmount.toDoubleOrNull() ?: 0.0) > 0.0,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (uiState.currentOcrStep == "OPTION_B") MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                        ),
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = if (uiState.currentOcrStep == "OPTION_B") "SAVE MANUAL ENTRY" else "CONFIRM & SAVE",
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    )
}
