package com.gorib.app.domain.usecase

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.gorib.app.domain.model.OcrParseResult
import com.gorib.app.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class ProcessOcrReceiptUseCase @Inject constructor(
    private val categoryRepository: CategoryRepository
) {
    suspend fun executeFromUri(uriString: String, context: Context): OcrParseResult {
        return try {
            val uri = Uri.parse(uriString)
            val image = InputImage.fromFilePath(context, uri)
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            
            val resultText = suspendCancellableCoroutine<String> { continuation ->
                recognizer.process(image)
                    .addOnSuccessListener { visionText ->
                        continuation.resume(visionText.text)
                    }
                    .addOnFailureListener { e ->
                        continuation.resumeWithException(e)
                    }
            }
            execute(resultText)
        } catch (e: Exception) {
            e.printStackTrace()
            OcrParseResult(false, null, null, null, 0.0, "", listOf("title", "amount", "category"), null)
        }
    }

    suspend fun execute(rawText: String): OcrParseResult {
        if (rawText.isBlank()) {
            return OcrParseResult(
                isSuccess = false, title = null, amount = null, categorySuggestedId = null,
                confidence = 0.0, rawText = rawText, gaps = listOf("title", "amount", "category"), itemsSummary = null
            )
        }

        val categories = categoryRepository.getAllCategories().first()
        val lines = rawText.trim().split("\n").map { it.trim() }.filter { it.isNotEmpty() }
        
        var parsedTitle: String? = null
        if (lines.isNotEmpty()) {
            val potentialTitle = lines.first()
            if (potentialTitle.length in 3..40) {
                parsedTitle = potentialTitle
            }
        }

        // 1. Amount Parsing: Look specifically for "TOTAL" or "RM"
        var parsedAmount: Double? = null
        val totalKeywords = listOf("total", "amount", "amt", "jumlah", "bayaran")
        val priceRegex = """(\d+[\.\,]\d{2})""".toRegex()
        
        val possibleTotalLines = lines.filter { line ->
            totalKeywords.any { keyword -> line.lowercase().contains(keyword) }
        }
        
        for (line in possibleTotalLines.reversed()) {
            if (line.lowercase().contains("subtotal") || line.lowercase().contains("change") || line.lowercase().contains("cash")) continue
            val match = priceRegex.find(line)
            if (match != null) {
                val value = match.value.replace(",", ".").toDoubleOrNull()
                if (value != null && value > 0) {
                    parsedAmount = value
                    break
                }
            }
        }
        
        if (parsedAmount == null) {
            val allPrices = priceRegex.findAll(rawText).mapNotNull { it.value.replace(",", ".").toDoubleOrNull() }.toList()
            if (allPrices.isNotEmpty()) {
                parsedAmount = allPrices.maxOrNull() // Largest price on receipt is often the total
            }
        }

        // 2. Extract Items and Prices
        val itemsList = mutableListOf<String>()
        var itemsTotal = 0.0
        val skipKeywords = listOf("total", "amount", "amt", "jumlah", "bayaran", "cash", "change", "tax", "visa", "mastercard", "debit", "credit", "balance", "rounding")
        
        for (line in lines) {
            val lowerLine = line.lowercase()
            if (skipKeywords.any { lowerLine.contains(it) }) continue
            
            val match = priceRegex.find(line)
            if (match != null) {
                val price = match.value.replace(",", ".").toDoubleOrNull() ?: 0.0
                val itemName = line.replace(priceRegex, "").replace("RM", "", ignoreCase = true).trim()
                if (itemName.any { it.isLetter() } && itemName.length > 2 && price > 0.0 && price != parsedAmount) {
                    itemsList.add("- $itemName: RM %.2f".format(price))
                    itemsTotal += price
                }
            }
        }
        
        val itemsSummary = if (itemsList.isNotEmpty()) {
            "Extracted Items:\n" + itemsList.joinToString("\n")
        } else null

        // 3. Category Suggestion
        var suggestedCategoryId: Long? = null
        val lowerText = rawText.lowercase()
        val categoryMatch = when {
            lowerText.contains("grocer") || lowerText.contains("jaya") || lowerText.contains("lotus") || 
            lowerText.contains("aeon") || lowerText.contains("supermarket") || lowerText.contains("market") || lowerText.contains("tf value mart") -> "Groceries"
            lowerText.contains("mcd") || lowerText.contains("kfc") || lowerText.contains("starbucks") || 
            lowerText.contains("food") || lowerText.contains("restaurant") || lowerText.contains("cafe") -> "Outside Food"
            lowerText.contains("shell") || lowerText.contains("petron") || lowerText.contains("grab") || lowerText.contains("petronas") -> "Fuel & Transport"
            lowerText.contains("lazada") || lowerText.contains("shopee") || lowerText.contains("online") -> "Online Shopping"
            lowerText.contains("clinic") || lowerText.contains("pharmacy") || lowerText.contains("medical") -> "Medical"
            lowerText.contains("cinema") || lowerText.contains("tgv") || lowerText.contains("gsc") -> "Entertainment"
            lowerText.contains("watsons") || lowerText.contains("guardian") || lowerText.contains("care") -> "Personal Care"
            lowerText.contains("uniqlo") || lowerText.contains("zara") || lowerText.contains("clothing") -> "Clothing"
            else -> "Others"
        }

        val matchedCategory = categories.firstOrNull { it.name.equals(categoryMatch, ignoreCase = true) }
        suggestedCategoryId = matchedCategory?.id ?: categories.firstOrNull { it.name.equals("Others", ignoreCase = true) }?.id

        val gaps = mutableListOf<String>()
        if (parsedTitle == null) gaps.add("title")
        if (parsedAmount == null) gaps.add("amount")
        if (suggestedCategoryId == null) gaps.add("category")

        val isSuccess = parsedAmount != null || parsedTitle != null
        val confidence = when {
            parsedAmount != null && parsedTitle != null && suggestedCategoryId != null -> 0.95
            parsedAmount != null && parsedTitle != null -> 0.80
            parsedAmount != null || parsedTitle != null -> 0.50
            else -> 0.0
        }

        return OcrParseResult(
            isSuccess = isSuccess, title = parsedTitle, amount = parsedAmount,
            categorySuggestedId = suggestedCategoryId, confidence = confidence,
            rawText = rawText, gaps = gaps, itemsSummary = itemsSummary
        )
    }
}
