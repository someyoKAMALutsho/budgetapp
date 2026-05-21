package com.gorib.app.domain.model

data class ParsedReceiptItem(
    val rawLine: String,        // original line from receipt for debugging
    val name: String?,          // null or blank if could not parse
    val quantity: Double?,      // null if not found
    val unit: String?,          // "kg", "g", "pcs", "pack", null
    val unitPrice: Double?,     // price per unit if shown
    val totalPrice: Double?,    // line total (required — null = unreadable)
    val confidence: Float       // 0.0–1.0, low = needs manual review
)

data class ParsedReceipt(
    val items: List<ParsedReceiptItem>,
    val subtotal: Double?,
    val tax: Double?,
    val grandTotal: Double?,
    val storeName: String?,
    val receiptDate: String?
)

/**
 * Offline fallback receipt parser using ML Kit OCR raw text.
 * Enhanced to handle Malaysian receipt formats (Econsave, Maslee, Lotus's, Jaya Grocer, TF Value Mart)
 * with multi-line item grouping, barcode line detection, and Malay→English translation.
 * For the most accurate results, use GeminiReceiptParser when API key is configured.
 */
object OcrReceiptParser {

    /**
     * Common Malay/abbreviated grocery terms → English translations.
     * Used to append "(English)" after the original name, matching GeminiReceiptParser behavior.
     */
    private val translationDictionary = mapOf(
        // Meats & Protein
        "daging" to "Beef", "dgg" to "Beef", "dagging" to "Beef",
        "daging import" to "Imported Beef", "dgg import" to "Imported Beef",
        "ayam" to "Chicken", "aym" to "Chicken",
        "ayam potong" to "Cut Chicken", "ayam segar" to "Fresh Chicken",
        "ikan" to "Fish", "ikan kembung" to "Mackerel",
        "ikan tenggiri" to "Spanish Mackerel", "ikan selar" to "Yellow-Stripe Scad",
        "udang" to "Prawn", "sotong" to "Squid",
        "telur" to "Eggs", "telur grd a" to "Grade A Eggs", "telur ayam" to "Chicken Eggs",
        // Produce & Vegetables
        "mangga" to "Mango", "pisang" to "Banana", "epal" to "Apple",
        "betik" to "Papaya", "tembikai" to "Watermelon", "limau" to "Lime",
        "limau nipis" to "Calamansi Lime", "oren" to "Orange", "anggur" to "Grape",
        "durian" to "Durian", "nanas" to "Pineapple", "jambu" to "Guava",
        "bawang merah" to "Red Onion", "bawang putih" to "Garlic",
        "bawang besar" to "Big Onion", "kentang" to "Potato",
        "lobak merah" to "Carrot", "lobak putih" to "White Radish",
        "tomato" to "Tomato", "timun" to "Cucumber",
        "sayur" to "Vegetables", "sayur campur" to "Mixed Vegetables",
        "kangkung" to "Water Spinach", "bayam" to "Spinach",
        "sawi" to "Mustard Greens", "kubis" to "Cabbage",
        "brokoli" to "Broccoli", "cili" to "Chilli", "cili merah" to "Red Chilli",
        "cili padi" to "Bird's Eye Chilli", "halia" to "Ginger",
        "serai" to "Lemongrass", "daun bawang" to "Spring Onion",
        "daun sup" to "Celery Leaves", "daun pandan" to "Pandan Leaf",
        "jagung" to "Corn", "kacang" to "Bean", "kacang panjang" to "Long Bean",
        "taugeh" to "Bean Sprouts", "petola" to "Ridge Gourd",
        "terung" to "Eggplant", "labu" to "Pumpkin",
        // Dairy & Drinks
        "susu" to "Milk", "susu segar" to "Fresh Milk", "susu pekat" to "Condensed Milk",
        "mentega" to "Butter", "keju" to "Cheese", "yogurt" to "Yogurt",
        "air" to "Water", "air mineral" to "Mineral Water", "air kotak" to "Box Drink",
        "jus" to "Juice",
        // Grains & Staples
        "beras" to "Rice", "beras wangi" to "Fragrant Rice",
        "roti" to "Bread", "mi" to "Noodle", "mee" to "Noodle", "mihun" to "Rice Vermicelli",
        "bihun" to "Rice Vermicelli", "kuetiau" to "Flat Rice Noodle",
        "tepung" to "Flour", "tepung gandum" to "Wheat Flour",
        "gula" to "Sugar", "gula pasir" to "Granulated Sugar",
        "garam" to "Salt", "serbuk" to "Powder",
        // Cooking & Condiments
        "minyak" to "Oil", "minyak masak" to "Cooking Oil", "minyak sayur" to "Vegetable Oil",
        "kicap" to "Soy Sauce", "sos" to "Sauce", "sos tiram" to "Oyster Sauce",
        "sos cili" to "Chilli Sauce", "cuka" to "Vinegar",
        "santan" to "Coconut Milk", "belacan" to "Shrimp Paste",
        "rempah" to "Spice", "serbuk kari" to "Curry Powder",
        "serbuk kunyit" to "Turmeric Powder",
        // Household & Personal
        "sabun" to "Soap", "sabun basuh" to "Detergent", "syampu" to "Shampoo",
        "pencuci" to "Cleaner", "tisu" to "Tissue", "tuala" to "Towel",
        "ubat" to "Medicine", "lampin" to "Diaper",
        // Common abbreviations on receipts
        "bgn" to "Onion", "bwg" to "Onion", "bwg mrah" to "Red Onion",
        "ktg" to "Potato", "tmto" to "Tomato"
    )

    fun parse(rawText: String): ParsedReceipt {
        val lines = rawText.lines()
            .map { it.trim() }
            .filter { it.isNotBlank() }

        val items = mutableListOf<ParsedReceiptItem>()
        var subtotal: Double? = null
        var tax: Double? = null
        var grandTotal: Double? = null
        var storeName: String? = null
        var receiptDate: String? = null

        // --- Store name: usually first 1-3 non-empty lines before items ---
        val storeNameCandidates = lines.take(3)
            .filter { it.length > 3 && !it.contains(Regex("""\d{2}/\d{2}""")) }
        storeName = storeNameCandidates.firstOrNull()

        // --- Date: look for DD/MM/YYYY or YYYY-MM-DD pattern ---
        val dateRegex = Regex("""\d{1,2}[/-]\d{1,2}[/-]\d{2,4}""")
        receiptDate = lines.firstNotNullOfOrNull {
            dateRegex.find(it)?.value
        }

        // --- Grand total: look for TOTAL / JUMLAH / BALANCE / AMOUNT DUE lines ---
        // Search from bottom up, take the first match (most likely the final total)
        val totalKeywords = listOf(
            "grand total", "jumlah besar", "total amount", "net total",
            "to pay", "amount due", "balance",
            "total", "jumlah", "amaun", "bayaran"
        )
        for (line in lines.reversed()) {
            val lower = line.lowercase()
            for (keyword in totalKeywords) {
                if (lower.contains(keyword)) {
                    val amount = extractAmount(line)
                    if (amount != null && amount > 0) {
                        grandTotal = amount
                        break
                    }
                }
            }
            if (grandTotal != null) break
        }

        // --- Subtotal line ---
        val subtotalKeywords = listOf("subtotal", "sub total", "sub-total")
        for (line in lines) {
            val lower = line.lowercase()
            if (subtotalKeywords.any { lower.contains(it) }) {
                val amount = extractAmount(line)
                if (amount != null) { subtotal = amount; break }
            }
        }

        // --- Tax line ---
        val taxKeywords = listOf("gst", "sst", "tax", "cukai", "service charge")
        for (line in lines) {
            val lower = line.lowercase()
            if (taxKeywords.any { lower.contains(it) }) {
                val amount = extractAmount(line)
                if (amount != null) { tax = amount; break }
            }
        }

        // --- Advanced Line Items Parsing ---
        // Malaysian receipts often have:
        //   Line 1: ITEM NAME (text)
        //   Line 2: BARCODE_NUMBER    PRICE
        // We need to group these together.

        val skipKeywords = listOf(
            "total", "jumlah", "tax", "gst", "sst",
            "change", "cash", "card", "visa", "mastercard", "receipt",
            "thank", "terima", "cashier", "pelayan", "invoice",
            "subtotal", "sub total", "discount", "diskaun", "rounding",
            "balance", "bayaran", "amaun", "tender", "amount",
            "member", "point", "saving", "round", "payment",
            "tunai", "baki", "no.", "date", "time", "trn",
            "jimat", "penjimatan", "debit", "credit"
        )

        val pricePattern = Regex("""\d+\.\d{2}""")
        val barcodePattern = Regex("""^\d{7,14}$""")  // Standalone barcode line (7-14 digits only)
        val startsWithBarcodePattern = Regex("""^\d{7,14}\s""")  // Line starting with barcode then space
        val qtyPattern = Regex(
            """(\d+\.?\d*)\s*(kg|g|ml|l|liter|litre|pcs|pc|pack|beg|box|tin|btl|bottle)\b""",
            RegexOption.IGNORE_CASE
        )
        val multiplyPattern = Regex("""(\d+)\s*[xX@]\s*\d+\.\d{2}""")

        // Phase 1: Classify each line
        data class ClassifiedLine(
            val index: Int,
            val text: String,
            val isSkipLine: Boolean,
            val hasPrice: Boolean,
            val isBarcodeOnly: Boolean,
            val isBarcodeWithPrice: Boolean,
            val isTextOnly: Boolean
        )

        val classified = lines.mapIndexed { i, line ->
            val lower = line.lowercase()
            val isSkip = skipKeywords.any { lower.contains(it) }
            val hasPrice = pricePattern.containsMatchIn(line)
            val trimmedNoPrice = line.replace(pricePattern, "").trim()
            val isBarcodeOnly = barcodePattern.matches(line.trim())
            val isBarcodeWithPrice = startsWithBarcodePattern.containsMatchIn(line) && hasPrice
            val isTextOnly = !hasPrice && !isBarcodeOnly && line.any { it.isLetter() } && line.length > 2

            ClassifiedLine(i, line, isSkip, hasPrice, isBarcodeOnly, isBarcodeWithPrice, isTextOnly)
        }

        // Phase 2: Group lines into items
        // Strategy: Walk through lines, when we see a text-only line followed by a barcode+price line,
        // group them. Otherwise treat price-bearing lines as standalone items.
        var i = 0
        while (i < classified.size) {
            val current = classified[i]

            // Skip non-item lines
            if (current.isSkipLine || current.isBarcodeOnly) {
                i++
                continue
            }

            // Case A: Text-only line (possible item name) + next line has price (barcode+price)
            if (current.isTextOnly && i + 1 < classified.size) {
                val next = classified[i + 1]
                if (!next.isSkipLine && next.hasPrice) {
                    // Group these two lines
                    val nameFromCurrent = current.text.trim()
                    val priceLine = next.text
                    val item = extractItemFromGroupedLines(nameFromCurrent, priceLine,
                        pricePattern, qtyPattern, multiplyPattern, grandTotal)
                    if (item != null) {
                        items.add(item)
                    }
                    i += 2
                    continue
                }
            }

            // Case B: Line with both text and price (single-line item)
            if (current.hasPrice && !current.isBarcodeOnly) {
                val lower = current.text.lowercase()
                if (skipKeywords.none { lower.contains(it) } && current.text.length > 4) {
                    val item = extractItemFromSingleLine(current.text,
                        pricePattern, qtyPattern, multiplyPattern, grandTotal)
                    if (item != null) {
                        items.add(item)
                    }
                }
            }

            i++
        }

        // Phase 3: Apply translations to item names
        val translatedItems = items.map { item ->
            if (item.name != null) {
                val translated = translateName(item.name)
                if (translated != item.name) {
                    item.copy(name = translated, rawLine = item.rawLine)
                } else item
            } else item
        }

        return ParsedReceipt(
            items = translatedItems,
            subtotal = subtotal,
            tax = tax,
            grandTotal = grandTotal,
            storeName = storeName,
            receiptDate = receiptDate
        )
    }

    /**
     * Extract an item from two grouped lines (name line + price line).
     */
    private fun extractItemFromGroupedLines(
        nameLine: String,
        priceLine: String,
        pricePattern: Regex,
        qtyPattern: Regex,
        multiplyPattern: Regex,
        grandTotal: Double?
    ): ParsedReceiptItem? {
        val prices = pricePattern.findAll(priceLine).mapNotNull { it.value.toDoubleOrNull() }.toList()
        if (prices.isEmpty()) return null

        val totalPrice = prices.last()
        if (grandTotal != null && totalPrice == grandTotal) return null

        // Clean up the name: remove leading item codes (pure numbers)
        val cleanName = nameLine
            .replace(Regex("""^\d+\s+"""), "")  // remove leading item codes
            .replace(Regex("""\s{2,}"""), " ")
            .trim()

        if (cleanName.length < 2) return null

        // Check for quantity in either line
        val qtyMatch = qtyPattern.find(nameLine) ?: qtyPattern.find(priceLine)
        val multiplyMatch = multiplyPattern.find(priceLine) ?: multiplyPattern.find(nameLine)

        val quantity = qtyMatch?.groupValues?.get(1)?.toDoubleOrNull()
            ?: multiplyMatch?.groupValues?.get(1)?.toDoubleOrNull()
            ?: 1.0
        val unit = qtyMatch?.groupValues?.get(2)?.lowercase()
            ?: if (multiplyMatch != null) "pcs" else "pcs"

        return ParsedReceiptItem(
            rawLine = "$nameLine | $priceLine",
            name = cleanName,
            quantity = quantity,
            unit = unit,
            unitPrice = if (prices.size > 1) prices.first() else null,
            totalPrice = totalPrice,
            confidence = if (cleanName.length >= 3 && totalPrice > 0) 0.85f else 0.60f
        )
    }

    /**
     * Extract an item from a single line containing both name and price.
     */
    private fun extractItemFromSingleLine(
        line: String,
        pricePattern: Regex,
        qtyPattern: Regex,
        multiplyPattern: Regex,
        grandTotal: Double?
    ): ParsedReceiptItem? {
        val prices = pricePattern.findAll(line).mapNotNull { it.value.toDoubleOrNull() }.toList()
        if (prices.isEmpty()) return null

        val totalPrice = prices.last()
        if (grandTotal != null && totalPrice == grandTotal) return null

        val qtyMatch = qtyPattern.find(line)
        val multiplyMatch = multiplyPattern.find(line)

        val quantity = qtyMatch?.groupValues?.get(1)?.toDoubleOrNull()
            ?: multiplyMatch?.groupValues?.get(1)?.toDoubleOrNull()
            ?: 1.0
        val unit = qtyMatch?.groupValues?.get(2)?.lowercase()
            ?: if (multiplyMatch != null) "pcs" else "pcs"

        // Extract name: remove prices, qty patterns, leading item codes
        var nameRaw = line
        if (qtyMatch != null) nameRaw = nameRaw.replace(qtyMatch.value, "")
        nameRaw = nameRaw.replace(multiplyPattern, "")
            .replace(pricePattern, "")
            .replace(Regex("""^\d+\s+"""), "")
            .trim()
            .replace(Regex("""\s{2,}"""), " ")

        val name = if (nameRaw.length >= 2) nameRaw else null

        val confidence = when {
            name != null && name.length >= 3 && totalPrice > 0 -> 0.85f
            name != null && totalPrice > 0 -> 0.65f
            totalPrice > 0 -> 0.40f
            else -> 0.20f
        }

        return ParsedReceiptItem(
            rawLine = line,
            name = name,
            quantity = quantity,
            unit = unit,
            unitPrice = if (prices.size > 1) prices.first() else null,
            totalPrice = totalPrice,
            confidence = confidence
        )
    }

    /**
     * Translate a Malay/abbreviated item name to include English translation in parentheses.
     * E.g., "BAWANG MERAH" → "BAWANG MERAH (Red Onion)"
     * If already English or unknown, returns original name unchanged.
     */
    private fun translateName(name: String): String {
        // Skip if already has a translation in parentheses
        if (name.contains("(") && name.contains(")")) return name

        val lowerName = name.lowercase().trim()

        // Try exact match first (longest match wins)
        val sortedKeys = translationDictionary.keys.sortedByDescending { it.length }
        for (key in sortedKeys) {
            if (lowerName == key || lowerName.startsWith("$key ") || lowerName.endsWith(" $key")) {
                return "$name (${translationDictionary[key]})"
            }
        }

        // Try partial matching — check if the name contains any known Malay word
        for (key in sortedKeys) {
            if (key.length >= 4 && lowerName.contains(key)) {
                return "$name (${translationDictionary[key]})"
            }
        }

        return name
    }

    private fun extractAmount(line: String): Double? {
        val pattern = Regex("""\d+\.\d{2}""")
        return pattern.findAll(line).lastOrNull()?.value?.toDoubleOrNull()
    }
}

