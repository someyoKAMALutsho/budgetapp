package com.gorib.app.ui.screens.groceries

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.zIndex
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import com.gorib.app.data.db.entity.GroceryItemEntity
import com.gorib.app.domain.model.Category
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.content.Intent
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.compose.ui.platform.LocalContext
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.gorib.app.ui.screens.groceries.ReceiptReviewViewModel
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import androidx.compose.foundation.BorderStroke

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroceriesScreen(
    viewModel: GroceriesViewModel,
    onNavigateToSessionDetail: (Long) -> Unit,
    onNavigateToReceiptReview: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val activeSession = uiState.activeSession

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val receiptReviewViewModel: ReceiptReviewViewModel = hiltViewModel(context as ComponentActivity)
    var isOcrScanning by remember { mutableStateOf(false) }

    val receiptPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val uri = result.data?.data
            if (uri != null) {
                isOcrScanning = true
                scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                    try {
                        if (uiState.geminiApiKey.isNotBlank()) {
                            // Advanced Gemini Vision Parsing
                            android.util.Log.d("GroceriesScreen", "Starting Gemini receipt scan...")
                            val bitmap = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                                val source = android.graphics.ImageDecoder.createSource(context.contentResolver, uri)
                                android.graphics.ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                                    decoder.allocator = android.graphics.ImageDecoder.ALLOCATOR_SOFTWARE
                                }
                            } else {
                                @Suppress("DEPRECATION")
                                android.provider.MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                            }
                            android.util.Log.d("GroceriesScreen", "Bitmap loaded: ${bitmap.width}x${bitmap.height}, config=${bitmap.config}")

                            val parsed = com.gorib.app.domain.model.GeminiReceiptParser.parseReceipt(bitmap, uiState.geminiApiKey)
                            
                            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                if (parsed != null && parsed.items.isNotEmpty()) {
                                    receiptReviewViewModel.loadParsedReceipt(
                                        receipt = parsed,
                                        sessionId = activeSession?.session?.id,
                                        rawText = "Extracted by Gemini 2.5 Flash AI — ${parsed.items.size} items"
                                    )
                                    isOcrScanning = false
                                    onNavigateToReceiptReview()
                                } else {
                                    isOcrScanning = false
                                    android.widget.Toast.makeText(
                                        context,
                                        "AI could not read any items from this receipt. Try a clearer photo.",
                                        android.widget.Toast.LENGTH_LONG
                                    ).show()
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
                                        sessionId = activeSession?.session?.id,
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
                        android.util.Log.e("GroceriesScreen", "Receipt scan exception", e)
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                            isOcrScanning = false
                            android.widget.Toast.makeText(context, "Scan error: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Smart Grocery (SGIS)",
                        fontWeight = FontWeight.Black,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            if (activeSession != null) {
                ExtendedFloatingActionButton(
                    onClick = { viewModel.showAddItemSheet(true) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add Item", fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            AnimatedContent(
                targetState = activeSession != null,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "ScreenState"
            ) { hasActiveSession ->
                if (hasActiveSession && activeSession != null) {
                    ActiveSessionContent(
                        sessionWithItems = activeSession,
                        categories = uiState.categories,
                        onRemoveItem = { viewModel.removeItem(it) },
                        onEndSessionClick = { viewModel.showEndSessionSheet(true) },
                        onScanReceiptClick = {
                            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                            receiptPickerLauncher.launch(intent)
                        }
                    )
                } else {
                    InactiveSessionContent(
                        history = uiState.sessionHistory,
                        isLoading = uiState.isLoading,
                        onStartShoppingClick = { viewModel.showStartSheet(true) },
                        onSessionClick = { onNavigateToSessionDetail(it) }
                    )
                }
            }

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
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // MVI driven Bottom Sheets
            if (uiState.showStartSheet) {
                StartSessionBottomSheet(
                    isLoading = uiState.isLoading,
                    onDismiss = { viewModel.showStartSheet(false) },
                    onConfirm = { storeName -> viewModel.startSession(storeName) }
                )
            }

            if (uiState.showAddItemSheet && activeSession != null) {
                AddItemBottomSheet(
                    categories = uiState.categories,
                    autocompleteResults = uiState.autocompleteResults,
                    onSearchQueryChange = { viewModel.searchItems(it) },
                    onClearAutocomplete = { viewModel.clearAutocomplete() },
                    onDismiss = { viewModel.showAddItemSheet(false) },
                    onConfirm = { name, qty, unit, totalPrice, catId ->
                        viewModel.addItem(name, qty, unit, totalPrice, catId)
                    }
                )
            }

            if (uiState.showEndSessionSheet && activeSession != null) {
                EndSessionBottomSheet(
                    sessionWithItems = activeSession,
                    isLoading = uiState.isLoading,
                    onDismiss = { viewModel.showEndSessionSheet(false) },
                    onConfirm = { viewModel.endSession() }
                )
            }
        }
    }
}

@Composable
fun InactiveSessionContent(
    history: List<com.gorib.app.data.db.entity.ShoppingSessionWithItems>,
    isLoading: Boolean,
    onStartShoppingClick: () -> Unit,
    onSessionClick: (Long) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Welcome and Start Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        RoundedCornerShape(24.dp)
                    ),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
                                    Color.Transparent
                                )
                            )
                        )
                        .padding(24.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.ShoppingCart,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Smart Grocery Assistant",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Track your groceries live, auto-total items, and automatically log a transaction under your Groceries budget.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(20.dp))

                        Button(
                            onClick = onStartShoppingClick,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Start Shopping", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // History Section
        item {
            Text(
                text = "Recent Shopping Sessions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }

        if (history.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No completed sessions yet",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(history) { sessionWithItems ->
                val session = sessionWithItems.session
                val itemsCount = sessionWithItems.items.size
                val dateStr = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                    .format(Date(session.completedAt ?: session.startedAt))

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSessionClick(session.id) }
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                            RoundedCornerShape(16.dp)
                        ),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = session.storeName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "$dateStr • $itemsCount items",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = "RM %.2f".format(Locale.US, session.totalAmount),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ActiveSessionContent(
    sessionWithItems: com.gorib.app.data.db.entity.ShoppingSessionWithItems,
    categories: List<Category>,
    onRemoveItem: (Long) -> Unit,
    onEndSessionClick: () -> Unit,
    onScanReceiptClick: () -> Unit
) {
    val session = sessionWithItems.session
    val items = sessionWithItems.items

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Active session running total card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                        RoundedCornerShape(24.dp)
                    ),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "SHOPPING AT",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = session.storeName,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Button(
                                onClick = onEndSessionClick,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error
                                )
                            ) {
                                Icon(imageVector = Icons.Default.Check, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("End Session", fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Running Total",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "RM %.2f".format(Locale.US, session.totalAmount),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedButton(
                            onClick = onScanReceiptClick,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text("📷 Scan Receipt (Auto Import Items)", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // List of current items header
        item {
            Text(
                text = "Cart Items (${items.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }

        if (items.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Your shopping cart is empty. Tap '+ Add Item' below to start adding items!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(items) { item ->
                val matchedCat = categories.firstOrNull { it.id == item.categoryId }
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                            RoundedCornerShape(16.dp)
                        ),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = matchedCat?.iconEmoji ?: "📦",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = item.itemName,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${"%.2f".format(Locale.US, item.quantity)} ${item.unit} • Total: RM ${"%.2f".format(Locale.US, item.totalPrice)}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { onRemoveItem(item.id) }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete Item",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
}

// 1. Start Session Bottom Sheet
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StartSessionBottomSheet(
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var storeName by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Text(
                text = "Enter Store Name",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = storeName,
                onValueChange = {
                    storeName = it
                    showError = false
                },
                label = { Text("Store Name") },
                placeholder = { Text("e.g. Lotus's, Giant, Village Grocer") },
                modifier = Modifier.fillMaxWidth(),
                isError = showError,
                supportingText = {
                    if (showError) {
                        Text("Store name cannot be empty")
                    }
                },
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if (storeName.trim().isEmpty()) {
                        showError = true
                    } else {
                        onConfirm(storeName)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp))
                } else {
                    Text("Begin Shopping Session", fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// 2. Add Item Bottom Sheet
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddItemBottomSheet(
    categories: List<Category>,
    autocompleteResults: List<GroceryItemEntity>,
    onSearchQueryChange: (String) -> Unit,
    onClearAutocomplete: () -> Unit,
    onDismiss: () -> Unit,
    onConfirm: (String, Double, String, Double, Long) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var qtyStr by remember { mutableStateOf("1") }
    var unit by remember { mutableStateOf("pcs") }
    var totalPriceStr by remember { mutableStateOf("") }
    var selectedCategoryId by remember { mutableStateOf(1L) }
    var showCategoryDropdown by remember { mutableStateOf(false) }
    var unitExpanded by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        windowInsets = WindowInsets.ime
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 16.dp)
                .imePadding()
                .verticalScroll(rememberScrollState())
        ) {
            // Sheet drag handle
            Box(
                modifier = Modifier
                    .width(40.dp).height(4.dp)
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(2.dp))
                    .align(Alignment.CenterHorizontally)
            )
            Spacer(Modifier.height(16.dp))

            Text("Add Item", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(16.dp))

            // FIELD 1: Item name (with autocomplete dropdown ABOVE keyboard)
            Box {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        onSearchQueryChange(it)
                    },
                    label = { Text("Item") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Next
                    )
                )

                // Autocomplete dropdown — appears ABOVE the field, not below
                if (autocompleteResults.isNotEmpty() && name.length >= 2) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .offset(y = (-autocompleteResults.size * 52).dp)  // floats above field
                            .zIndex(10f),
                        elevation = CardDefaults.cardElevation(8.dp)
                    ) {
                        autocompleteResults.take(4).forEach { item ->
                            ListItem(
                                headlineContent = { Text(item.name) },
                                supportingContent = { Text("${item.groceryCategory} • Default: ${item.defaultUnit}") },
                                modifier = Modifier.clickable {
                                    name = item.name
                                    unit = item.defaultUnit
                                    val matched = categories.firstOrNull {
                                        it.name.equals(item.groceryCategory, ignoreCase = true)
                                    }
                                    if (matched != null) {
                                        selectedCategoryId = matched.id
                                    }
                                    onClearAutocomplete()
                                }
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))

            // FIELD 2: Qty + Unit on same row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = qtyStr,
                    onValueChange = { qtyStr = it },
                    label = { Text("Qty") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    placeholder = { Text("1") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Next
                    )
                )

                // Unit dropdown using ExposedDropdownMenuBox
                ExposedDropdownMenuBox(
                    expanded = unitExpanded,
                    onExpandedChange = { unitExpanded = it },
                    modifier = Modifier.weight(1.5f)
                ) {
                    OutlinedTextField(
                        value = unit,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Unit") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(unitExpanded) },
                        modifier = Modifier.menuAnchor(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = unitExpanded,
                        onDismissRequest = { unitExpanded = false }
                    ) {
                        listOf("pcs", "kg", "g", "pack", "litre", "ml", "box", "dozen")
                            .forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option) },
                                    onClick = {
                                        unit = option
                                        unitExpanded = false
                                    }
                                )
                            }
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))

            // FIELD 3: Total price
            OutlinedTextField(
                value = totalPriceStr,
                onValueChange = { totalPriceStr = it.filter { c -> c.isDigit() || c == '.' } },
                label = { Text("Total Price (RM)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                prefix = { Text("RM ") },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        if (name.trim().isNotEmpty()) {
                            val quantity = qtyStr.toDoubleOrNull() ?: 1.0
                            val totalPrice = totalPriceStr.toDoubleOrNull() ?: 0.0
                            if (totalPrice > 0) {
                                onConfirm(name, quantity, unit, totalPrice, selectedCategoryId)
                            }
                        }
                    }
                )
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Category choice dropdown
            Box(modifier = Modifier.fillMaxWidth()) {
                val currentCategory = categories.firstOrNull { it.id == selectedCategoryId }
                OutlinedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showCategoryDropdown = true },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = currentCategory?.iconEmoji ?: "📦",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = currentCategory?.name ?: "Others",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null)
                    }
                }

                DropdownMenu(
                    expanded = showCategoryDropdown,
                    onDismissRequest = { showCategoryDropdown = false }
                ) {
                    categories.forEach { category ->
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(category.iconEmoji)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(category.name)
                                }
                            },
                            onClick = {
                                selectedCategoryId = category.id
                                showCategoryDropdown = false
                            }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))

            // Add button
            Button(
                onClick = {
                    if (name.trim().isNotEmpty()) {
                        val quantity = qtyStr.toDoubleOrNull() ?: 1.0
                        val totalPrice = totalPriceStr.toDoubleOrNull() ?: 0.0
                        if (totalPrice > 0) {
                            onConfirm(name, quantity, unit, totalPrice, selectedCategoryId)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(50)
            ) {
                Text("Add Item", style = MaterialTheme.typography.labelLarge)
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

// 3. End Session Bottom Sheet
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EndSessionBottomSheet(
    sessionWithItems: com.gorib.app.data.db.entity.ShoppingSessionWithItems,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val session = sessionWithItems.session
    val items = sessionWithItems.items

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Text(
                text = "End Shopping Session?",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Review your session recap before converting it to a standard month transaction.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(24.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Store Name", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(session.storeName, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Total Items", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${items.size} items", fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Grand Total", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                        Text(
                            text = "RM %.2f".format(Locale.US, session.totalAmount),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onConfirm,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp))
                } else {
                    Text("Confirm & Log Transaction", fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
