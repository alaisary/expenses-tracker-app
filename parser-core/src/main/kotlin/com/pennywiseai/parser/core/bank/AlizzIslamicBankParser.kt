package com.pennywiseai.parser.core.bank

/**
 * Parser for Alizz Islamic Bank (Oman) SMS messages. Extends
 * [BaseOmaniBankParser] for the shared Omani mechanics; adds only the
 * Alizz-specific merchant quirk.
 *
 * Confirmed templates (from captured samples):
 * - Debit with merchant: "OMR X has been debited from your account NNNN********NNNN at MERCHANT on
 *   yyyy-MM-dd HH:mm:ss. Your available balance is OMR B"
 * - Debit without merchant: "OMR X has been debited from your account MASK on
 *   yyyy-MM-dd HH:mm:ss[.]  Your available balance is OMR B" (merchant null —
 *   the generic "from your account MASK" fragment is rejected)
 * - Debit without "from": "OMR X has been debited MASK on yyyy-MM-dd HH:mm:ss.
 *   Your available balance is OMR B" (mask directly after "debited")
 * - Salary credit: "Your salary of OMR X has been credited into your account
 *   number MASK on …" → INCOME, merchant "Salary"
 * - Card-not-present variant: "OMR X has been debited from your account number
 *   MERCHANT   LOCATION   OMN on DD-MMM , HH:mm:ss.Available Balance is  OMR B"
 *   (the "account number" field carries the merchant, space-padded with
 *   location/country junk)
 * - Installment debit: "عميلنا العزيز، … تم خصم مبلغ القسط الشهري وقدره OMR X
 *   ريال عماني من حسابك رقم MASK." (the customer name is never extracted)
 *
 * Credit form (reported by the bank's alerts, not yet captured on device — the
 * bank misspells "balance" as "balans", both spellings are supported):
 * - "OMR X has been credited to your account … Your available balans is OMR B" → INCOME
 *
 * Not yet covered (need on-device samples): ATM withdrawals, international
 * charges, Arabic-language variants. Sender ID confirmed: "Alizz Bank".
 */
class AlizzIslamicBankParser : BaseOmaniBankParser() {

    override fun getBankName() = "Alizz Islamic Bank"

    override fun canHandle(sender: String): Boolean {
        val stripped = sender.uppercase().replace(Regex("""[\s\-_]"""), "")
        return stripped.contains("ALIZZ") ||
                sender.contains("بنك العز") ||
                sender.contains("العز الاسلامي")
    }

    override fun extractMerchant(message: String, sender: String): String? {
        // "Your salary of OMR X has been credited into your account number MASK
        // on …" — the SMS's own label; the salary row has no real merchant.
        if (SALARY_MARKER.containsMatchIn(message)) return "Salary"
        // Card-not-present charges put the merchant in the "account number"
        // field: "… from your account number  MERCHANT   LOCATION   OMN on …"
        ACCOUNT_NUMBER_MERCHANT.find(message)?.let { match ->
            val cleaned = cleanOmaniMerchant(match.groupValues[1])
            if (isValidOmaniMerchantName(cleaned)) return cleaned
        }
        return super.extractMerchant(message, sender)
    }

    private companion object {
        private val ACCOUNT_NUMBER_MERCHANT = Regex(
            """account number\s+(.+?)\s+on\s+\d""",
            RegexOption.IGNORE_CASE
        )
        private val SALARY_MARKER = Regex("""Your salary of""", RegexOption.IGNORE_CASE)
    }
}
