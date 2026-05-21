package com.gorib.app.domain.model

import android.graphics.Bitmap
import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.min

/**
 * Advanced Receipt Parser powered by Google Gemini 2.5 Flash Vision.
 * Handles Malaysian receipts (Econsave, Maslee, Lotus's, Jaya Grocer, TF Value Mart)
 * and international receipt formats with multi-line item grouping.
 */
object GeminiReceiptParser {

    private const val TAG = "GeminiReceiptParser"
    private const val MAX_IMAGE_DIMENSION = 1536 // Resize to fit API limits while preserving text clarity

    /**
     * Parses a receipt bitmap using Gemini Vision AI.
     * Returns a ParsedReceipt on success, or null on failure (errors are logged).
     */
    suspend fun parseReceipt(bitmap: Bitmap, apiKey: String): ParsedReceipt? = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting receipt parse. Original bitmap: ${bitmap.width}x${bitmap.height}")

            // Step 1: Resize the image to prevent API payload/token limits
            val resized = resizeBitmap(bitmap)
            Log.d(TAG, "Resized bitmap: ${resized.width}x${resized.height}")

            // Step 2: Initialize the model — use gemini-2.5-flash for best vision accuracy
            // Do NOT use responseMimeType — it causes failures on some SDK versions.
            // Instead, we instruct the model to return JSON and parse it ourselves.
            val generativeModel = GenerativeModel(
                modelName = "gemini-2.5-flash",
                apiKey = apiKey
            )

            // Step 3: Build a focused, concise prompt
            val prompt = buildPrompt()

            Log.d(TAG, "Sending image to Gemini 2.5 Flash...")
            val response = generativeModel.generateContent(
                content {
                    image(resized)
                    text(prompt)
                }
            )

            val responseText = response.text
            if (responseText.isNullOrBlank()) {
                Log.e(TAG, "Gemini returned empty response")
                return@withContext null
            }

            Log.d(TAG, "Gemini raw response (first 500 chars): ${responseText.take(500)}")

            // Step 4: Extract JSON from response (may be wrapped in ```json blocks)
            val json = extractJson(responseText)
            if (json == null) {
                Log.e(TAG, "Failed to extract JSON from response: ${responseText.take(300)}")
                return@withContext null
            }

            // Step 5: Parse the JSON into our data model
            val receipt = parseJson(json)
            Log.d(TAG, "Successfully parsed ${receipt.items.size} items, grandTotal=${receipt.grandTotal}")
            receipt

        } catch (e: Exception) {
            Log.e(TAG, "Receipt parsing FAILED with exception", e)
            null
        }
    }

    /**
     * Resize bitmap so neither dimension exceeds MAX_IMAGE_DIMENSION.
     * Preserves aspect ratio. Uses ARGB_8888 to ensure software rendering compatibility.
     */
    private fun resizeBitmap(original: Bitmap): Bitmap {
        val w = original.width
        val h = original.height
        if (w <= MAX_IMAGE_DIMENSION && h <= MAX_IMAGE_DIMENSION) {
            // Still need to ensure it's a software bitmap (ARGB_8888)
            return if (original.config == Bitmap.Config.ARGB_8888) original
            else original.copy(Bitmap.Config.ARGB_8888, false)
        }
        val scale = min(MAX_IMAGE_DIMENSION.toFloat() / w, MAX_IMAGE_DIMENSION.toFloat() / h)
        val newW = (w * scale).toInt()
        val newH = (h * scale).toInt()
        val scaled = Bitmap.createScaledBitmap(original, newW, newH, true)
        return if (scaled.config == Bitmap.Config.ARGB_8888) scaled
        else scaled.copy(Bitmap.Config.ARGB_8888, false)
    }

    /**
     * Build a focused prompt that works across receipt formats.
     * Key insight: Don't over-constrain. Let the vision model read the image naturally.
     */
    private fun buildPrompt(): String = """
Extract every purchased item from this grocery receipt image.

Return ONLY a JSON object with this exact structure (no other text):
{
  "storeName": "store name or null",
  "receiptDate": "YYYY-MM-DD or null",
  "items": [
    {
      "name": "original name (English translation)",
      "qty": 1,
      "unit": "pcs",
      "price": 4.30
    }
  ],
  "grandTotal": 73.85
}

Rules:
- "name": Copy the product name from the receipt exactly as printed. Keep package sizes (550G, 1KG, 1.5L, 30S, 15X27G) as part of the name. Remove barcodes and item codes.
  IMPORTANT — Translation: If the item name is in Malay, Chinese, Tamil, or any non-English language, append a short English translation in parentheses after the original name. Examples:
    • "DGG IMPORT" → "DGG IMPORT (Imported Beef)"
    • "MANGGA" → "MANGGA (Mango)"
    • "BAWANG MERAH" → "BAWANG MERAH (Red Onion)"
    • "SUSU SEGAR" → "SUSU SEGAR (Fresh Milk)"
    • "AYAM POTONG" → "AYAM POTONG (Cut Chicken)"
    • "ROTI GARDENIA" → "ROTI GARDENIA (Gardenia Bread)"
    • "GULA PASIR" → "GULA PASIR (Granulated Sugar)"
    • "TELUR GRD A" → "TELUR GRD A (Grade A Eggs)"
    • "IKAN KEMBUNG" → "IKAN KEMBUNG (Mackerel)"
  If the name is already in English (e.g. "COCA COLA", "MILO", "MAGGI"), do NOT add a translation — keep it as is.
  Use your best knowledge to translate abbreviations common on Malaysian receipts (DGG = Daging = Beef, AYM = Ayam = Chicken, etc.).
- "qty": The count of items purchased (usually 1). If receipt shows "2 QTY" or "x2", use that number. For weighed produce (e.g. 0.65 KG at RM4/kg), qty is the weight like 0.65.
- "unit": Use "pcs" for packaged items, "kg" for weighed produce, "pack" for multipacks.
- "price": The final price PAID for this line item (after any discounts). This is usually the rightmost number on the item's line.
- "grandTotal": The final total amount paid. Look for "TOTAL", "BALANCE", "JUMLAH", or the last/largest total on the receipt. Do NOT confuse this with the last item's price.
- Do NOT include subtotal lines, tax lines, payment lines, change lines, or discount-only lines as items.
- Many Malaysian receipts (Econsave, Maslee, Lotus, Jaya Grocer) print the item name on one line and the barcode + price on the next line. Group them together as one item.
""".trimIndent()

    /**
     * Extract JSON object from Gemini response text.
     * Handles markdown code blocks, leading/trailing text, etc.
     */
    private fun extractJson(text: String): JSONObject? {
        // Try to find JSON within ```json ... ``` blocks
        val codeBlockPattern = Regex("""```(?:json)?\s*\n?([\s\S]*?)\n?```""")
        val codeMatch = codeBlockPattern.find(text)
        val jsonStr = if (codeMatch != null) {
            codeMatch.groupValues[1].trim()
        } else {
            // Try to find raw JSON object
            val firstBrace = text.indexOf('{')
            val lastBrace = text.lastIndexOf('}')
            if (firstBrace >= 0 && lastBrace > firstBrace) {
                text.substring(firstBrace, lastBrace + 1)
            } else {
                text.trim()
            }
        }

        return try {
            JSONObject(jsonStr)
        } catch (e: Exception) {
            Log.e(TAG, "JSON parse failed: ${e.message}\nAttempted to parse: ${jsonStr.take(200)}")
            null
        }
    }

    /**
     * Parse the validated JSON object into our ParsedReceipt data model.
     */
    private fun parseJson(root: JSONObject): ParsedReceipt {
        val itemsArray = root.optJSONArray("items") ?: JSONArray()
        val parsedItems = mutableListOf<ParsedReceiptItem>()

        for (i in 0 until itemsArray.length()) {
            val obj = itemsArray.optJSONObject(i) ?: continue

            val name = obj.optString("name", "").takeIf { it.isNotBlank() && it != "null" }
            val qty = parseNumber(obj.opt("qty")) ?: parseNumber(obj.opt("quantity"))
            val unit = obj.optString("unit", "pcs").takeIf { it.isNotBlank() && it != "null" } ?: "pcs"
            val price = parseNumber(obj.opt("price")) ?: parseNumber(obj.opt("totalPrice"))
            val unitPrice = parseNumber(obj.opt("unitPrice"))

            // Skip items with no name and no price (junk lines)
            if (name == null && price == null) continue

            val confidence = when {
                name != null && price != null -> 0.95f
                name != null -> 0.60f
                price != null -> 0.40f
                else -> 0.20f
            }

            parsedItems.add(
                ParsedReceiptItem(
                    rawLine = name ?: "Item ${i + 1}",
                    name = name,
                    quantity = qty ?: 1.0,
                    unit = unit,
                    unitPrice = unitPrice,
                    totalPrice = price,
                    confidence = confidence
                )
            )
        }

        val grandTotal = parseNumber(root.opt("grandTotal"))
        val subtotal = parseNumber(root.opt("subtotal"))
        val tax = parseNumber(root.opt("tax"))
        val storeName = root.optString("storeName").takeIf { it.isNotBlank() && it != "null" }
        val receiptDate = root.optString("receiptDate").takeIf { it.isNotBlank() && it != "null" }

        return ParsedReceipt(
            items = parsedItems,
            subtotal = subtotal,
            tax = tax,
            grandTotal = grandTotal,
            storeName = storeName,
            receiptDate = receiptDate
        )
    }

    /** Safely parse any JSON value to Double */
    private fun parseNumber(value: Any?): Double? = when (value) {
        is Number -> value.toDouble()
        is String -> value.toDoubleOrNull()
        else -> null
    }
}
