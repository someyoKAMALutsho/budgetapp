package com.gorib.app.ui.screens.history

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gorib.app.domain.model.Transaction
import com.gorib.app.ui.components.AddTransactionBottomSheet
import com.gorib.app.ui.components.TransactionDetailBottomSheet
import com.gorib.app.ui.utils.formatRelativeDate
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel,
    onBackClick: () -> Unit,
    onNavigateToReceiptReview: () -> Unit
) {
    val displayMonth by viewModel.displayMonth.collectAsState()
    val canGoForward by viewModel.canGoForward.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategoryId by viewModel.selectedCategoryId.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val filteredTransactions by viewModel.filteredTransactions.collectAsState()

    var selectedTransactionForDetail by remember { mutableStateOf<Transaction?>(null) }
    var editingTransaction by remember { mutableStateOf<Transaction?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(bottom = 80.dp) // space for shared bottom nav
        ) {
            // ── TOP NAV BAR & SEARCH ────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Transaction History",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Month selector
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = { viewModel.changeMonth(-1) }) {
                        Icon(Icons.Default.KeyboardArrowLeft, "Previous month")
                    }
                    Text(
                        text = displayMonth,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(
                        onClick = { viewModel.changeMonth(1) },
                        enabled = canGoForward
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowRight,
                            contentDescription = "Next month",
                            tint = if (canGoForward) LocalContentColor.current
                                   else LocalContentColor.current.copy(alpha = 0.3f)
                        )
                    }
                }

                // Search field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setSearch(it) },
                    placeholder = { Text("Search by description...", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setSearch("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    ),
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                )

                // Horizontal Filter Chips Row
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    item {
                        FilterChip(
                            selected = selectedCategoryId == null,
                            onClick = { viewModel.setCategory(null) },
                            label = { Text("All") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                    items(categories) { cat ->
                        FilterChip(
                            selected = selectedCategoryId == cat.id,
                            onClick = { viewModel.setCategory(cat.id) },
                            label = { Text("${cat.iconEmoji} ${cat.name}") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }
            }

            // ── TRANSACTION LIST ────────────────────────────────────────────
            if (filteredTransactions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(24.dp)
                        .clip(MaterialTheme.shapes.medium)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.medium),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No transactions found",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    val grouped = filteredTransactions.groupBy { item ->
                        val diff = System.currentTimeMillis() - item.loggedAt
                        val oneDayMs = 24 * 60 * 60 * 1000L
                        when {
                            diff < oneDayMs && SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date(item.loggedAt)) == SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date()) -> "Today"
                            diff < 2 * oneDayMs && SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date(item.loggedAt)) == SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date(System.currentTimeMillis() - oneDayMs)) -> "Yesterday"
                            else -> SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()).format(Date(item.loggedAt))
                        }
                    }

                    grouped.forEach { (dateHeader, transactionList) ->
                        // Sticky Header Date Indicator
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.background)
                                    .padding(horizontal = 24.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = dateHeader,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }

                        items(transactionList, key = { it.id }) { transaction ->
                            val cat = categories.find { it.id == transaction.categoryId }
                            
                            SwipeableHistoryItem(
                                transaction = transaction,
                                categoryEmoji = cat?.iconEmoji ?: "✏️",
                                categoryName = cat?.name ?: "General",
                                onRowClick = {
                                    selectedTransactionForDetail = transaction
                                },
                                onDelete = {
                                    viewModel.deleteTransaction(transaction) {
                                        coroutineScope.launch {
                                            val result = snackbarHostState.showSnackbar(
                                                message = "Deleted '${transaction.title}'",
                                                actionLabel = "Undo",
                                                duration = SnackbarDuration.Short
                                            )
                                            if (result == SnackbarResult.ActionPerformed) {
                                                viewModel.undoDelete()
                                            }
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Detail Bottom Sheet
    selectedTransactionForDetail?.let { transaction ->
        val cat = categories.find { it.id == transaction.categoryId }
        TransactionDetailBottomSheet(
            transaction = transaction,
            categoryEmoji = cat?.iconEmoji ?: "✏️",
            categoryName = cat?.name ?: "General",
            onDismiss = { selectedTransactionForDetail = null },
            onEditClick = { tx ->
                editingTransaction = tx
            },
            onDeleteClick = { tx ->
                viewModel.deleteTransaction(tx) {
                    coroutineScope.launch {
                        val result = snackbarHostState.showSnackbar(
                            message = "Deleted '${tx.title}'",
                            actionLabel = "Undo",
                            duration = SnackbarDuration.Short
                        )
                        if (result == SnackbarResult.ActionPerformed) {
                            viewModel.undoDelete()
                        }
                    }
                }
            }
        )
    }

    // Edit Bottom Sheet
    if (editingTransaction != null) {
        AddTransactionBottomSheet(
            existingTransaction = editingTransaction,
            onDismiss = { editingTransaction = null },
            onSaveSuccess = {},
            onNavigateToReceiptReview = onNavigateToReceiptReview
        )
    }
}

@Composable
fun SwipeableHistoryItem(
    transaction: Transaction,
    categoryEmoji: String,
    categoryName: String,
    onRowClick: () -> Unit,
    onDelete: () -> Unit
) {
    var offsetX by remember { mutableStateOf(0f) }
    val maxDrag = -300f

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 6.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.errorContainer)
    ) {
        // Red Delete background
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(end = 16.dp),
            contentAlignment = Alignment.CenterEnd
        ) {
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }

        // Draggable card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier
                .offset { IntOffset(offsetX.roundToInt(), 0) }
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.medium)
                .clickable { onRowClick() }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onHorizontalDrag = { _, dragAmount ->
                            val original = offsetX
                            val next = (original + dragAmount).coerceIn(maxDrag, 0f)
                            offsetX = next
                        },
                        onDragEnd = {
                            if (offsetX < maxDrag / 2f) {
                                offsetX = maxDrag
                                onDelete()
                                offsetX = 0f
                            } else {
                                offsetX = 0f
                            }
                        }
                    )
                }
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
                                text = "$categoryName • ${formatRelativeDate(transaction.loggedAt)}",
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
}
