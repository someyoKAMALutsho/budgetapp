package com.gorib.app.ui.screens.groceries

import androidx.activity.ComponentActivity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiptReviewScreen(
    navController: NavController,
    viewModel: ReceiptReviewViewModel = hiltViewModel(LocalContext.current as ComponentActivity)
) {
    val reviewItems by viewModel.reviewItems.collectAsState()
    val storeName by viewModel.storeName.collectAsState()
    val grandTotal by viewModel.grandTotal.collectAsState()
    val calculatedTotal by viewModel.calculatedTotal.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()
    val savedSuccessfully by viewModel.savedSuccessfully.collectAsState()

    LaunchedEffect(savedSuccessfully) {
        if (savedSuccessfully) {
            navController.popBackStack()
        }
    }

    val needsReviewCount = reviewItems.count { it.needsReview || it.name.isBlank() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 80.dp)
    ) {

        // ── TOP BAR ──────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.Default.ArrowBack, "Back")
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Review Receipt",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = storeName.ifBlank { "Unknown store" },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(0.6f)
                )
            }
        }

        // ── SUMMARY CHIP ROW ──────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Items found
            SuggestionChip(
                onClick = {},
                label = { Text("${reviewItems.size} items found") }
            )
            // Needs review
            if (needsReviewCount > 0) {
                SuggestionChip(
                    onClick = {},
                    label = { Text("$needsReviewCount need review") },
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                )
            }
            // Total
            SuggestionChip(
                onClick = {},
                label = {
                    Text(
                        text = "RM ${"%.2f".format(
                            if (grandTotal > 0) grandTotal else calculatedTotal
                        )}"
                    )
                },
                colors = SuggestionChipDefaults.suggestionChipColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        // ── INSTRUCTION TEXT ─────────────────────────────────────────────
        if (needsReviewCount > 0) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "⚠️ Some items couldn't be read clearly. Fill in the blanks or tap ✕ to remove an item.",
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        // ── ITEM LIST ─────────────────────────────────────────────────────
        reviewItems.forEachIndexed { index, item ->
            val borderColor = if (item.needsReview || item.name.isBlank()) {
                MaterialTheme.colorScheme.error.copy(0.5f)
            } else {
                MaterialTheme.colorScheme.outlineVariant
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, borderColor)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {

                    // Row 1: Item number + delete button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Item ${index + 1}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(0.4f)
                        )
                        IconButton(
                            onClick = { viewModel.removeItem(item.id) },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Remove",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    // Row 2: Name field (editable)
                    var nameValue by remember(item.id) { mutableStateOf(item.name) }
                    OutlinedTextField(
                        value = nameValue,
                        onValueChange = {
                            nameValue = it
                            viewModel.updateItem(item.id, name = it)
                        },
                        label = { Text("Item Name") },
                        placeholder = { Text("e.g. Bawang Merah") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        isError = nameValue.isBlank(),
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words,
                            imeAction = ImeAction.Next
                        ),
                        trailingIcon = {
                          if (nameValue.isBlank()) {
                              Icon(
                                  imageVector = Icons.Default.Warning,
                                  contentDescription = null,
                                  tint = MaterialTheme.colorScheme.error
                              )
                          }
                        }
                    )

                    Spacer(Modifier.height(8.dp))

                    // Row 3: Qty + Unit + Price
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Qty
                        var qtyValue by remember(item.id) {
                            mutableStateOf(
                                item.quantity.toString()
                                    .trimEnd('0').trimEnd('.')
                            )
                        }
                        OutlinedTextField(
                            value = qtyValue,
                            onValueChange = {
                                qtyValue = it
                                viewModel.updateItem(
                                    id = item.id,
                                    qty = it.toDoubleOrNull() ?: item.quantity
                                )
                            },
                            label = { Text("Qty") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Decimal,
                                imeAction = ImeAction.Next
                            )
                        )

                        // Unit
                        var unitValue by remember(item.id) { mutableStateOf(item.unit) }
                        var unitExpanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = unitExpanded,
                            onExpandedChange = { unitExpanded = it },
                            modifier = Modifier.weight(1f)
                        ) {
                            OutlinedTextField(
                                value = unitValue,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Unit") },
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(unitExpanded)
                                },
                                modifier = Modifier.menuAnchor()
                            )
                            ExposedDropdownMenu(
                                expanded = unitExpanded,
                                onDismissRequest = { unitExpanded = false }
                            ) {
                                listOf("pcs", "kg", "g", "pack", "litre", "ml", "box", "dozen")
                                    .forEach { opt ->
                                        DropdownMenuItem(
                                            text = { Text(opt) },
                                            onClick = {
                                                unitValue = opt
                                                unitExpanded = false
                                                viewModel.updateItem(item.id, unit = opt)
                                            }
                                        )
                                    }
                            }
                        }

                        // Price
                        var priceValue by remember(item.id) {
                            mutableStateOf("%.2f".format(item.totalPrice))
                        }
                        OutlinedTextField(
                            value = priceValue,
                            onValueChange = {
                                priceValue = it
                                viewModel.updateItem(
                                    id = item.id,
                                    price = it.toDoubleOrNull() ?: item.totalPrice
                                )
                            },
                            label = { Text("Price (RM)") },
                            modifier = Modifier.weight(1.2f),
                            singleLine = true,
                            prefix = { Text("RM ") },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Decimal,
                                imeAction = ImeAction.Done
                            )
                        )
                    }

                    // Low confidence warning
                    if (item.confidence < 0.70f) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "⚠ Low confidence — please verify",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // ── TOTAL SUMMARY ─────────────────────────────────────────────────
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
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
                        text = "Items Total",
                        style = MaterialTheme.typography.labelSmall
                    )
                    Text(
                        text = "RM %.2f".format(calculatedTotal),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                if (grandTotal > 0 && Math.abs(grandTotal - calculatedTotal) > 0.01) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Receipt Total",
                            style = MaterialTheme.typography.labelSmall
                        )
                        Text(
                            text = "RM %.2f".format(grandTotal),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(0.6f)
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // ── APPROVE BUTTON ────────────────────────────────────────────────
        Button(
            onClick = { viewModel.approveAndSave() },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(50),
            enabled = !isSaving && reviewItems.isNotEmpty()
        ) {
            if (isSaving) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text = "✓ Approve & Save ${reviewItems.size} Items",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // Discard button
        TextButton(
            onClick = { navController.popBackStack() },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = "Discard Receipt",
                color = MaterialTheme.colorScheme.error
            )
        }

        Spacer(Modifier.height(16.dp))
    }
}
