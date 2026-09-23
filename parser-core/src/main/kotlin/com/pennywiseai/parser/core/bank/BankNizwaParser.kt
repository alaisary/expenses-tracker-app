package com.pennywiseai.parser.core.bank

/**
 * Parser for Bank Nizwa (Oman) SMS messages. Extends [BaseOmaniBankParser]
 * for the shared Omani mechanics (OMR/ر.ع currency, 3-decimal amounts, Arabic
 * normalization, account masks, balances); this class adds only the
 * Bank-Nizwa-specific shapes.
 *
 * Supported formats (Arabic):
 * - Deposit: "تم ايداع ر.ع 30.000 في حسابك رقم 001 ###### 00610 الرقم المرجعي.
 *   48950972 . الرصيد المتوفر هو 31.500 ر.ع ." → INCOME
 * - Withdrawal: "تم سحب ر.ع 30.000 من حساب رقم 001 ###### 00610 الى … الرصيد
 *   المتوفر هو 1.500 ر.ع" → EXPENSE
 * - Debit/card purchase: "تم خصم ر.ع 1.500 من حسابك رقم … في …" → EXPENSE
 *
 * Bank Nizwa writes the income marker without the hamza ("تم ايداع" rather than
 * "تم إيداع"), so normalization folds the hamza-less form before the shared
 * Omani markers run. The balance wording ("الرصيد المتوفر هو … ر.ع") and the
 * reference wording ("الرقم المرجعي.") are already handled by the base class.
 *
 * Currency: OMR (Omani Rial)
 * Senders: BANK NIZWA / BankNizwa / بنك نزوى
 */
class BankNizwaParser : BaseOmaniBankParser() {

    override fun getBankName() = "Bank Nizwa"

    override fun canHandle(sender: String): Boolean {
        val normalized = sender.uppercase()
        return normalized.contains("NIZWA") ||
                sender.contains("نزوى")
    }

    override fun normalizeOmaniSms(message: String): String {
        return super.normalizeOmaniSms(message)
            // "تم ايداع" (no hamza) is Bank Nizwa's standard spelling.
            .replace("ايداع", "إيداع")
    }
}
