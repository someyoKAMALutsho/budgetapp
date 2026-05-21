package com.gorib.app.ui.screens.settings.utilities

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gorib.app.data.db.entity.UtilityBillGroupWithItems
import com.gorib.app.domain.repository.UtilityLineItemInput

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UtilitiesScreen(
    viewModel: UtilitiesViewModel,
    onBackClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }
    
    // Track expanded cards
    val expandedStates = remember { mutableStateMapOf<Long, Boolean>() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Utilities Module", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Bill")
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                
                // Outstanding Unpaid Header summary
                val unpaidTotal = uiState.utilityGroups
                    .filter { it.group.paymentStatus == "UNPAID" }
                    .sumOf { it.group.totalAmount }

                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Pending Utilities Total",
                            style = MaterialTheme.typography.labelLarge.copy(
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "RM ${String.format("%,.2f", unpaidTotal)}",
                            style = MaterialTheme.typography.headlineLarge.copy(
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.Black
                            )
                        )
                    }
                }

                Text(
                    text = "Bill History",
                    style = MaterialTheme.typography.titleLarge.copy(
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                if (uiState.utilityGroups.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = "No Bills",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No utility bills configured.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(uiState.utilityGroups) { groupWithItems ->
                            val group = groupWithItems.group
                            val isExpanded = expandedStates[group.id] ?: false
                            val isPaid = group.paymentStatus == "PAID"

                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                shape = MaterialTheme.shapes.medium,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.medium)
                                    .clickable { expandedStates[group.id] = !isExpanded }
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = group.name,
                                                    fontWeight = FontWeight.Bold,
                                                    style = MaterialTheme.typography.titleMedium,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                if (group.isCombined) {
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(4.dp))
                                                            .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f))
                                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                                    ) {
                                                        Text(
                                                            text = "Combined",
                                                            color = MaterialTheme.colorScheme.secondary,
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                }
                                            }
                                            Text(
                                                text = "Month: ${group.billingMonth}",
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            )
                                        }

                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = "RM ${String.format("%,.2f", group.totalAmount)}",
                                                fontWeight = FontWeight.Black,
                                                color = MaterialTheme.colorScheme.primary,
                                                style = MaterialTheme.typography.titleLarge
                                            )
                                            
                                            Spacer(modifier = Modifier.height(4.dp))

                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(
                                                        if (isPaid) Color(0xFFE8F5E9) else Color(0xFFFFF3E0)
                                                    )
                                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                                    .clickable {
                                                        if (isPaid) {
                                                            viewModel.markAsUnpaid(group.id)
                                                        } else {
                                                            viewModel.markAsPaid(group.id)
                                                        }
                                                    }
                                            ) {
                                                Text(
                                                    text = if (isPaid) "Paid" else "Unpaid",
                                                    color = if (isPaid) Color(0xFF2E7D32) else Color(0xFFE65100),
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }

                                    // Expandable Area for Line Items and Note/Receipt Details
                                    AnimatedVisibility(
                                        visible = isExpanded,
                                        enter = expandVertically() + fadeIn(),
                                        exit = shrinkVertically() + fadeOut()
                                    ) {
                                        Column(modifier = Modifier.padding(top = 16.dp)) {
                                            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                                            Spacer(modifier = Modifier.height(8.dp))

                                            Text(
                                                text = "Line Items Details:",
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            
                                            Spacer(modifier = Modifier.height(4.dp))

                                            groupWithItems.lineItems.forEach { item ->
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(vertical = 4.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    val itemLabel = item.customName ?: item.type
                                                    Text(
                                                        text = "• $itemLabel",
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                    Text(
                                                        text = "RM ${String.format("%,.2f", item.amount)}",
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                }
                                            }

                                            if (group.note != null) {
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Text(
                                                    text = "Note: ${group.note}",
                                                    style = MaterialTheme.typography.bodySmall.copy(
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                )
                                            }

                                            if (group.receiptPath != null) {
                                                Spacer(modifier = Modifier.height(6.dp))
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Icon(
                                                        Icons.Default.CheckCircle,
                                                        contentDescription = "Receipt Attached",
                                                        tint = Color(0xFF2E7D32),
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        text = "Receipt Attached (${group.receiptPath})",
                                                        style = MaterialTheme.typography.bodySmall.copy(
                                                            color = Color(0xFF2E7D32),
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(16.dp))

                                            // Swipe / Confirmation Delete Button
                                            Button(
                                                onClick = { viewModel.deleteBill(group.id) },
                                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                                shape = RoundedCornerShape(12.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.White)
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Delete Bill Record", fontWeight = FontWeight.Bold, color = Color.White)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Bill Dialog Setup
    if (showAddDialog) {
        var isCombinedMode by remember { mutableStateOf(false) }
        var singleType by remember { mutableStateOf("Electricity") }
        var singleCustomName by remember { mutableStateOf("") }
        var singleAmount by remember { mutableStateOf("") }
        
        // Combined list states
        val combinedItems = remember { mutableStateListOf<UtilityLineItemInput>() }
        
        var noteText by remember { mutableStateOf("") }
        var mockReceiptPath by remember { mutableStateOf<String?>(null) }
        var validationError by remember { mutableStateOf<String?>(null) }

        val utilityTypes = listOf("Electricity", "Water", "Internet", "Gas", "Waste", "Custom")

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Log New Utility Bill") },
            text = {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        // Single / Combined Mode Selector Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (!isCombinedMode) MaterialTheme.colorScheme.primary else Color.Transparent)
                                    .clickable { isCombinedMode = false }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Single Bill",
                                    color = if (!isCombinedMode) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isCombinedMode) MaterialTheme.colorScheme.primary else Color.Transparent)
                                    .clickable { 
                                        isCombinedMode = true
                                        if (combinedItems.isEmpty()) {
                                            combinedItems.add(UtilityLineItemInput("Electricity", null, 0.0))
                                        }
                                    }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Combined Bill",
                                    color = if (isCombinedMode) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    if (!isCombinedMode) {
                        // SINGLE MODE
                        item {
                            Text("Utility Type", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                            
                            // Simple type chips selection
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                val chipsRow1 = utilityTypes.take(3)
                                chipsRow1.forEach { t ->
                                    FilterChip(
                                        selected = singleType == t,
                                        onClick = { singleType = t },
                                        label = { Text(t, fontSize = 11.sp) }
                                    )
                                }
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                val chipsRow2 = utilityTypes.drop(3)
                                chipsRow2.forEach { t ->
                                    FilterChip(
                                        selected = singleType == t,
                                        onClick = { singleType = t },
                                        label = { Text(t, fontSize = 11.sp) }
                                    )
                                }
                            }
                        }

                        if (singleType == "Custom") {
                            item {
                                OutlinedTextField(
                                    value = singleCustomName,
                                    onValueChange = { singleCustomName = it },
                                    label = { Text("Custom Utility Name") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        item {
                            OutlinedTextField(
                                value = singleAmount,
                                onValueChange = { singleAmount = it },
                                label = { Text("Amount (RM)") },
                                placeholder = { Text("0.00") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    } else {
                        // COMBINED MODE
                        items(combinedItems.size) { index ->
                            val item = combinedItems[index]
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Item #${index + 1}",
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        if (combinedItems.size > 1) {
                                            IconButton(onClick = { combinedItems.removeAt(index) }) {
                                                Icon(Icons.Default.Close, contentDescription = "Remove Item", tint = Color.Red)
                                            }
                                        }
                                    }

                                    // Chips for type
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        utilityTypes.filter { it != "Custom" }.forEach { t ->
                                            FilterChip(
                                                selected = item.utilityType == t,
                                                onClick = {
                                                    combinedItems[index] = item.copy(utilityType = t)
                                                },
                                                label = { Text(t, fontSize = 10.sp) }
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    var amtStr by remember(item.amount) {
                                        mutableStateOf(if (item.amount == 0.0) "" else item.amount.toString())
                                    }

                                    OutlinedTextField(
                                        value = amtStr,
                                        onValueChange = {
                                            amtStr = it
                                            val parsed = it.toDoubleOrNull() ?: 0.0
                                            combinedItems[index] = item.copy(amount = parsed)
                                        },
                                        label = { Text("Amount (RM)") },
                                        placeholder = { Text("0.00") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }

                        item {
                            Button(
                                onClick = {
                                    combinedItems.add(UtilityLineItemInput("Electricity", null, 0.0))
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Add")
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Add Line Item", color = Color.White)
                            }
                        }
                    }

                    item {
                        OutlinedTextField(
                            value = noteText,
                            onValueChange = { noteText = it },
                            label = { Text("Note (optional)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    item {
                        // Mock receipt picker button
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                                .clickable {
                                    mockReceiptPath = "receipts/utility_${System.currentTimeMillis()}.jpg"
                                }
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Share, 
                                    contentDescription = "Attach",
                                    tint = if (mockReceiptPath != null) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (mockReceiptPath != null) "Receipt Attached!" else "Attach Bill Receipt Photo",
                                    color = if (mockReceiptPath != null) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    if (validationError != null) {
                        item {
                            Text(
                                text = validationError!!,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (!isCombinedMode) {
                            val amt = singleAmount.toDoubleOrNull()
                            if (amt == null || amt <= 0.0) {
                                validationError = "Please enter a valid amount greater than RM 0.00"
                            } else {
                                viewModel.saveSingleBill(
                                    type = singleType,
                                    customName = if (singleType == "Custom") singleCustomName.ifBlank { null } else null,
                                    amount = amt,
                                    note = noteText.ifBlank { null },
                                    receiptPath = mockReceiptPath
                                )
                                showAddDialog = false
                            }
                        } else {
                            if (combinedItems.isEmpty()) {
                                validationError = "Please add at least one line item"
                            } else if (combinedItems.any { it.amount <= 0.0 }) {
                                validationError = "All item amounts must be greater than RM 0.00"
                            } else {
                                viewModel.saveCombinedBill(
                                    lineItems = combinedItems.toList(),
                                    note = noteText.ifBlank { null },
                                    receiptPath = mockReceiptPath
                                )
                                showAddDialog = false
                            }
                        }
                    }
                ) {
                    Text("Save Bill")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
