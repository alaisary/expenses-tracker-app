package com.pennywiseai.parser.core.bank

import com.pennywiseai.parser.core.ParsedTransaction
import com.pennywiseai.parser.core.TransactionType
import java.math.BigDecimal

/**
 * Base abstract class for Omani bank parsers.
 *
 * Owns the shared mechanics of Omani bank SMS so concrete parsers only declare
 * their bank name, senders, and any bank-specific quirks:
 *
 * - Currency: OMR by default; the Arabic token ر.ع / ر.ع. / ريال عماني and
 *   cross-currency card charges (e.g. USD) are detected per message. Currency
 *   is read from the *transaction portion* of the SMS (the message cut at the
 *   first balance/limit marker) so the OMR balance at the end of a USD charge
 *   never shadows the transaction currency.
 * - Amounts: 3-decimal baisa, comma thousands, amounts glued to the currency
 *   ("OMR2.000"), dotted currency ("OMR. 4.000"), dropped leading zeros
 *   (".200" → 0.200), and trailing-minus amounts ("مبلغ 0.300- ر.ع.").
 * - Transport corruption: "Short" / "Shortcut input" concat prefixes, stray
 *   \r and zero-width characters, and the broken double-alef rendering
 *   ("االخصم" → "الخصم"). The original smsBody is preserved on the result so
 *   dedup hashing sees the raw SMS.
 * - Transaction types: Arabic markers (تم خصم/تم إيداع/تم سحب/…) plus the
 *   Omani English shapes the generic keyword list misses ("used for OMR…",
 *   "POSSale"). Own-account and domestic transfers, and card payments
 *   ("payment towards … Credit Card"), map to TRANSFER.
 * - Account masks: NNNN********NNNN, 0425XXXXXXXX0011, 500*3101,
 *   01#######72102, 1019XXX017, spaced "001 ###### 00510", XX3502.
 * - Balances/limits: both languages, including the "balans" typo some Omani
 *   banks send. Merchant extraction is purely structural (في/بتاريخ, at/on,
 *   merchant-as-account, للحساب باسم) with generic noise cleanup — no
 *   merchant names are hardcoded here.
 */
abstract class BaseOmaniBankParser : BankParser() {

    override fun getCurrency(): String = "OMR"

    override fun parse(smsBody: String, sender: String, timestamp: Long): ParsedTransaction? {
        val normalized = normalizeOmaniSms(smsBody)
        val transaction = super.parse(normalized, sender, timestamp) ?: return null
        return transaction.copy(
            smsBody = smsBody,
            currency = extractCurrency(normalized) ?: transaction.currency
        )
    }

    /**
     * Removes SMS transport corruption before pattern matching. Whitespace is
     * intentionally NOT collapsed: runs of 2+ spaces are used to split
     * merchant names from padding/location junk.
     */
    protected open fun normalizeOmaniSms(message: String): String {
        return message
            .replace("\r", " ")
            .replace("\u200e", "")
            .replace("\u200f", "")
            .replace("\uFEFF", "")
            .replace(Regex("""^(?:Shortcut input|Short)\s*"""), "")
            .replace("اال", "ال")
    }

    /**
     * The message up to (not including) the first balance/limit marker. Amount
     * and currency extraction run on this portion so balance figures never
     * masquerade as the transaction amount.
     */
    protected fun transactionPortion(message: String): String {
        val lower = message.lowercase()
        var cut = message.length
        for (marker in BALANCE_CUT_MARKERS) {
            val idx = lower.indexOf(marker)
            if (idx >= 0 && idx < cut) cut = idx
        }
        return message.substring(0, cut)
    }

    override fun isTransactionMessage(message: String): Boolean {
        val lower = message.lowercase()
        val omaniMarkers = ARABIC_TXN_MARKERS + listOf("used for omr", "used at", "possale", "you have sent")
        if (omaniMarkers.any { lower.contains(it) }) {
            return NOISE_MARKERS.none { lower.contains(it) }
        }
        return super.isTransactionMessage(message)
    }

    override fun extractTransactionType(message: String): TransactionType? {
        val lower = message.lowercase()
        return when {
            // Own-account and domestic transfers — before generic debit wording
            lower.contains("own account transfer") || lower.contains("domestic transfer") ->
                TransactionType.TRANSFER
            message.contains("تم تحويل") || message.contains("التحويل الداخلي") ->
                TransactionType.TRANSFER
            AC_TO_AC_TRANSFER.containsMatchIn(message) -> TransactionType.TRANSFER
            lower.contains("payment towards") -> TransactionType.TRANSFER
            lower.contains("you have sent") -> TransactionType.TRANSFER

            message.contains("تم إيداع") -> TransactionType.INCOME

            ARABIC_EXPENSE_MARKERS.any { message.contains(it) } -> TransactionType.EXPENSE
            ENGLISH_EXPENSE_MARKERS.any { lower.contains(it) } -> TransactionType.EXPENSE

            else -> super.extractTransactionType(message)
        }
    }

    override fun extractAmount(message: String): BigDecimal? {
        val txn = transactionPortion(message)
        for (pattern in AMOUNT_PATTERNS) {
            pattern.find(txn)?.let { match ->
                parseOmaniAmount(match.groupValues[1])?.let { return it }
            }
        }
        return super.extractAmount(message)
    }

    override fun extractCurrency(message: String): String? {
        val txn = transactionPortion(message)
        if (txn.contains("ريال") || RIYAL_TOKEN.containsMatchIn(txn)) return "OMR"
        ISO_CURRENCY_TOKEN.find(txn)?.let { return it.groupValues[1].uppercase() }
        return null
    }

    override fun extractBalance(message: String): BigDecimal? {
        for (pattern in BALANCE_PATTERNS) {
            pattern.find(message)?.let { match ->
                parseOmaniAmount(match.groupValues[1])?.let { return it }
            }
        }
        return super.extractBalance(message)
    }

    override fun extractAvailableLimit(message: String): BigDecimal? {
        for (pattern in CREDIT_LIMIT_PATTERNS) {
            pattern.find(message)?.let { match ->
                parseOmaniAmount(match.groupValues[1])?.let { return it }
            }
        }
        return super.extractAvailableLimit(message)
    }

    override fun extractReference(message: String): String? {
        OMAN_REFERENCE.find(message)?.let { return it.groupValues[1] }
        return super.extractReference(message)
    }

    override fun extractAccountLast4(message: String): String? {
        for (pattern in ACCOUNT_PATTERNS) {
            pattern.find(message)?.let { match ->
                extractLast4Digits(match.groupValues[1])?.let { return it }
            }
        }
        return super.extractAccountLast4(message)
    }

    override fun detectIsCard(message: String): Boolean {
        val lower = message.lowercase()
        // Checked before the generic account-word exclusion, because Omani card
        // alerts legitimately mix card and account wording ("Card of a/c …",
        // "Debit Card No … for Acct …").
        if (OMAN_CARD_MARKERS.any { lower.contains(it) }) return true
        if (CARD_BIN_MARKER.containsMatchIn(message)) return true
        return super.detectIsCard(message)
    }

    override fun extractMerchant(message: String, sender: String): String? {
        for (pattern in MERCHANT_PATTERNS) {
            for (match in pattern.findAll(message)) {
                val cleaned = cleanOmaniMerchant(match.groupValues[1])
                if (isValidOmaniMerchantName(cleaned)) return cleaned
            }
        }
        super.extractMerchant(message, sender)?.let { generic ->
            // Account-field leak check on the RAW generic capture — the
            // slash-cut in cleanOmaniMerchant would turn "your A/C 1047**001"
            // into "your A" and destroy the marker.
            val genericLower = generic.lowercase()
            if (genericLower.startsWith("your a/c") ||
                genericLower.startsWith("your account") ||
                genericLower.startsWith("your acct")
            ) {
                return@let
            }
            val cleaned = cleanOmaniMerchant(generic)
            if (isValidOmaniMerchantName(cleaned)) return cleaned
        }
        return null
    }

    /**
     * Strips Omani merchant-field noise without touching real merchant names:
     * backslash/slash-embedded locations ("MERCHANT\TOWN\"), space-padded
     * fields ("Khedmah          112"), numeric terminal-ID prefixes
     * ("181621-KFC", "7006OMANOIL…"), "PO BOX n P C" suffixes, trailing
     * country/terminal tokens (OM, OMN, OM000000000000, omOM00000, MCT, MUSC),
     * embedded/trailing terminal IDs, and truncated-field punctuation.
     */
    protected open fun cleanOmaniMerchant(raw: String): String {
        var t = raw.trim()
        if (t.isEmpty()) return t
        val slashIdx = listOf(t.indexOf('\\'), t.indexOf('/')).filter { it >= 0 }.minOrNull() ?: -1
        if (slashIdx > 0) t = t.substring(0, slashIdx)
        t = t.split(Regex("""\s{2,}""")).first().trim()
        t = t.replace(Regex("""^\d{4,}\s*-\s*"""), "")
        t = t.replace(Regex("""^\d{4,6}(?=[A-Za-z])"""), "")
        // "PO BOX …" and everything after it is address noise ("55 COFFEE
        // PO BOX 89 PC", "OMAN OIL IBRA PO BOX 92MCT", "PO BOXMCT") — strip
        // to end of field.
        t = t.replace(Regex("""\s+p\.?\s*o\.?\s*box.*$""", RegexOption.IGNORE_CASE), "")
        while (true) {
            val cleaned = t.replace(Regex("""\s+(?:OMN|OM[A-Z]*\d+|OM|MCT|MUSC)$""", RegexOption.IGNORE_CASE), "")
            if (cleaned == t) break
            t = cleaned
        }
        t = t.replace(Regex("""-\d{4,}"""), "")
        t = t.replace(Regex("""\s+\d{2,}$"""), "")
        t = t.replace(Regex("""[.\s\-/>]+$"""), "")
        return t.replace(Regex("""\s+"""), " ").trim()
    }

    /**
     * Merchant validation on top of the generic rules: rejects captures that
     * are mostly digits (phone numbers, terminal IDs) and any capture that
     * swallowed transaction wording (a lazy match that ran past the merchant
     * field into "…debited for OMR2.000 on …").
     */
    protected open fun isValidOmaniMerchantName(name: String): Boolean {
        if (!isValidMerchantName(name)) return false
        if (name.count { it.isDigit() } > name.count { it.isLetter() }) return false
        val lower = name.lowercase()
        return INVALID_MERCHANT_WORDS.none { lower.contains(it) }
    }

    private fun parseOmaniAmount(raw: String): BigDecimal? {
        val cleaned = raw.replace(",", "").let { if (it.startsWith(".")) "0$it" else it }
        return cleaned.toBigDecimalOrNull()
    }

    companion object {
        private const val AMT = """(\d[\d,]*(?:\.\d+)?|\.\d+)"""

        private val RIYAL_TOKEN = Regex("""ر\.?\s?ع""")
        private val ISO_CURRENCY_TOKEN =
            Regex("""\b(OMR|USD|SAR|AED|EUR|GBP|KWD|BHD|QAR|JOD|INR)\b""", RegexOption.IGNORE_CASE)
        private val AC_TO_AC_TRANSFER = Regex(
            """debited from your a/c\s+\S+\s+and credited to your a/c""",
            RegexOption.IGNORE_CASE
        )
        private val CARD_BIN_MARKER = Regex("""\bcard\s+[\dXx*#]{6,}""", RegexOption.IGNORE_CASE)
        private val OMAN_REFERENCE = Regex(
            """(?:RRN|Txn\s*Id|الرقم المرجعي(?:\s*هو)?)\s*[:.]?\s*([A-Za-z0-9]+)""",
            RegexOption.IGNORE_CASE
        )

        private val ARABIC_TXN_MARKERS = listOf(
            "تم خصم", "تم إيداع", "تم سحب", "تم سداد", "تم تحويل", "تم دفع",
            "تم اجراء معاملة", "تم إجراء معاملة", "تم استخدام", "لدفع مبلغ", "القسط الشهري"
        )
        private val ARABIC_EXPENSE_MARKERS = listOf(
            "تم خصم", "تم سحب", "تم سداد", "تم دفع",
            "تم اجراء معاملة", "تم إجراء معاملة", "تم استخدام", "لدفع مبلغ", "القسط الشهري"
        )
        private val ENGLISH_EXPENSE_MARKERS = listOf("used for", "used at", "possale")

        private val NOISE_MARKERS = listOf(
            "otp", "one time password", "verification code", "offer", "discount",
            "cashback offer", "win ", "has requested", "payment request",
            "collect request", "is due", "minimum amount due", "in arrears",
            "is overdue", "ignore if"
        )

        private val OMAN_CARD_MARKERS = listOf(
            "card of a/c", "debit card", "credit card", "cardno", "card ending",
            "بطاقة", "بطاقت"
        )

        private val BALANCE_CUT_MARKERS = listOf(
            "رصيدك", "الرصيد", "الحد الإئتماني",
            "available bal", "avl bal", "new available", "balance is"
        )

        private val INVALID_MERCHANT_WORDS = listOf(
            "debited", "credited", "withdrawn", "omr", "ر.ع", "بتاريخ",
            // Account-field leaks from the generic (super) patterns: the SMS
            // phrasing "…debited from your account MASK…" must never surface
            // the account fragment as a merchant.
            "your account", "your acct", "your a/c"
        )

        // Ordered: first match wins.
        private val AMOUNT_PATTERNS = listOf(
            // Trailing-minus: "مبلغ 0.300- ر.ع."
            Regex("""$AMT\s*-\s*ر\.?\s?ع"""),
            // Currency token before amount: "ر.ع 0.550"
            Regex("""ر\.?\s?ع\.?\s*$AMT"""),
            // Amount before token: "5.270 ر.ع", "2200ريال"
            Regex("""$AMT\s*ر\.?\s?ع"""),
            Regex("""$AMT\s*ريال"""),
            // "OMR 0.650" / "OMR2.000" / "OMR. 4.000"
            Regex("""OMR\.?\s*$AMT""", RegexOption.IGNORE_CASE),
            // "0.650 OMR"
            Regex("""$AMT\s*OMR""", RegexOption.IGNORE_CASE),
            // Cross-currency card charges: "USD 4.99" / "4.99 USD"
            Regex("""\b(?:USD|SAR|AED|EUR|GBP|KWD|BHD|QAR|JOD|INR)\s*$AMT""", RegexOption.IGNORE_CASE),
            Regex("""$AMT\s*(?:USD|SAR|AED|EUR|GBP|KWD|BHD|QAR|JOD|INR)\b""", RegexOption.IGNORE_CASE)
        )

        private val BALANCE_PATTERNS = listOf(
            // "رصيدك الحالي هو 23.118 OMR" / "رصيدك الحالي هو: 0.185 . ر.ع." / "رصيدك الحالي 7,839.615 ر.ع"
            Regex("""رصيدك الحالي(?:\s*هو)*\s*:?\s*(?:OMR\.?\s*)?$AMT"""),
            // "الرصيد المتوفر هو 180.954 ر.ع" / "الرصيد المتوفر لديك OMR 177.489"
            // "الرصيد المتاح في حسابكم هو OMR 1,493.318" / "الرصيد الحالي OMR2926.029"
            Regex("""الرصيد\s+(?:المتوفر|الحالي|المتاح)(?:\s*(?:هو|لديك|في حسابكم))*\s*:?\s*(?:OMR\.?\s*)?$AMT"""),
            // "Your available balance is OMR 1974.984" / "Avl Bal OMR 28.310."
            // "New Available Balance is OMR 33.839." / "available balans is OMR 456.468"
            // "Available balance in your account is OMR 33.898" (NBO)
            Regex("""(?:available|avl)\s+bal\w*(?:\s+in\s+your\s+account)?\.?\s*(?:is)?\s*:?\s*(?:OMR\.?\s*)?-?$AMT""", RegexOption.IGNORE_CASE)
        )

        private val CREDIT_LIMIT_PATTERNS = listOf(
            // "الحد الإئتماني المتوفر الأن OMR 1,046.140"
            Regex("""الحد الإئتماني المتوفر(?:\s*الأن)?\s*(?:OMR\.?\s*)?$AMT"""),
            // "Your available limit is OMR 2,359.954." / "Available limit OMR 359.885."
            Regex("""(?:available|avl)\s+limit\s*(?:is)?\s*:?\s*(?:OMR\.?\s*)?$AMT""", RegexOption.IGNORE_CASE)
        )

        // Ordered: first pattern that yields ≥3 digits wins.
        private val ACCOUNT_PATTERNS = listOf(
            // حسابك رقم MASK / حسابكم رقم MASK / حساب التوفير رقم MASK
            Regex("""حساب[^\n]{0,20}?رقم\s+([\dXx*#\s]+)"""),
            // "من حسابك  0070********4001" (masked account without the رقم marker)
            Regex("""من حسابك\s+([\dXx*#]{6,})"""),
            // "A/C 0322XXXXXXXX0027" / "account 0010********1001" / "a/c no 0383XXXXXXXX0039"
            Regex("""(?:a/c|account(?:\s+number)?|acct)(?:\s+no)?\.?\s+([\dXx*#\s]{4,})""", RegexOption.IGNORE_CASE),
            // "بطاقة موجب الخاصة بك رقم 4027XXXXXXXX8069"
            Regex("""(?:بطاقة|بطاقتك|بطاقتكم)[^\n]{0,40}?رقم\s+([\dXx*#\s]+)"""),
            // "تنتهي بالرقم 5769" / "المنتهية بـ 8890"
            Regex("""(?:تنتهي بالرقم|المنتهية بـ)\s*(\d{4})"""),
            // "debited MASK on …" (Alizz debit template without the "from your
            // account" wording: "OMR 30 has been debited 1600********4001 on …")
            Regex("""debited\s+([\dXx*#]{6,})\s+on""", RegexOption.IGNORE_CASE),
            // "Debit Card No 419291######8055" / "CardNo X6189" / "credit card no 4227XXXXXXXX6598"
            Regex("""(?:card|debit card|credit card)\s*(?:no\.?|number)?\s*\.?\s*([\dXx*#]{4,})""", RegexOption.IGNORE_CASE),
            // "card ending with 1083"
            Regex("""card ending(?: with)?\s*(\d{4})""", RegexOption.IGNORE_CASE),
            // "من الحساب XXXXXXX05696"
            Regex("""الحساب\s+([\dXx*#]{6,})""")
        )

        // Ordered: most specific merchant field first. Merchant extraction is
        // structural — patterns capture whatever the SMS places in the merchant
        // position; nothing bank- or merchant-specific is hardcoded.
        private val MERCHANT_PATTERNS = listOf(
            // "في MERCHANT بتاريخ" (Arabic card purchases)
            Regex("""في\s+(.+?)\s+بتاريخ"""),
            // "إلى MERCHANT بتاريخ" (prepaid wallet top-ups)
            Regex("""إلى\s+(.+?)\s+بتاريخ"""),
            // Merchant injected as the account field: "من حسابك رقم MERCHANT\TOWN\"
            Regex("""من حسابك رقم\s+(.+?)\s*\\"""),
            // "للحساب باسم MERCHANT"
            Regex("""للحساب باسم\s+(.+?)\s*\.?\s*(?:رصيدك|الرصيد)"""),
            // "في MERCHANT رصيدك/الرصيد/الحد الإئتماني" (merchant directly before the balance)
            Regex("""في\s+(.+?)\s*(?:رصيدك|الرصيد|الحد)""", RegexOption.DOT_MATCHES_ALL),
            // "at MERCHANT on 01/08/2026"
            Regex("""\bat\s+(.+?)\s+on\s+\d""", RegexOption.IGNORE_CASE),
            // "at MERCHANT for OMR 1.850" / "for an amount of OMR 8.534"
            Regex("""\bat\s+(.+?)\s+for\s+(?:an amount of\s+)?(?:OMR|[A-Z]{3})""", RegexOption.IGNORE_CASE),
            // "for MERCHANT\ …" (backslash-delimited merchant field)
            Regex("""\bfor\s+(.+?)\s*\\""", RegexOption.IGNORE_CASE),
            // "credited to your a/c MASK by PAYER on DATE" (credits name the payer)
            Regex(
                """credited to your (?:a/c|account)\s+\S+\s+by\s+(.+?)\s+on\s+\d""",
                RegexOption.IGNORE_CASE
            ),
            // "at MERCHANT." (dot-terminated merchant field, with or without a
            // trailing space — covers "Dar Al Atta APP.", "SPC..", "DELIVERY.AMERAT")
            Regex("""\bat\s+([^.\n]+?)\.""", RegexOption.IGNORE_CASE),
            // "sent to NAME from your a/c" (mobile-payment recipients)
            Regex("""\bto\s+(.+?)\s+from your a/c""", RegexOption.IGNORE_CASE)
        )
    }
}
