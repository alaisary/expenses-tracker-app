package com.pennywiseai.parser.core.bank

import java.math.BigDecimal

/**
 * Parser for National Bank of Oman (NBO) SMS messages. Extends
 * [BaseOmaniBankParser]; adds the NBO-specific merchant quirk.
 *
 * Supported formats:
 * - NBO Wallet debit: "OMR  X, is debited from your NBO Wallet on DATE. Your new
 *   Available balance is B. Thank you for Banking with NBO." (no account number;
 *   EXPENSE)
 * - Plain account debit: "OMR X is debited from your A/C MASK on dd-MM-yyyy
 *   HH:mm. \nAvailable balance in your account is OMR B." (EXPENSE, no merchant)
 * - Loan installment: "OMR X has been debited from your A/C MASK for your monthly
 *   loan installment. Your available balance is OMR B. … www.nbo.om/mb" (EXPENSE)
 * - Own-account transfer (EN): "Account MASK is debited OMR X on DATE as Own
 *   Account Transfer To MASK. Available balance OMR B." → TRANSFER
 * - Domestic transfer (AR): "تم سحب OMR X من الحساب MASK بتاريخ DATE عن طريق
 *   Domestic Transfer To NAME. الرصيد الحالي OMR B" → TRANSFER
 * - Own-account deposit (AR): "تم إيداع OMR X في الحساب MASK بتاريخ DATE عن طريق
 *   Own Account Transfer From MASK. الرصيد الحالي OMR B" → TRANSFER
 *
 * Currency: OMR (2- or 3-decimal amounts both occur). Senders: NBO / NBO Alerts.
 */
class NBOParser : BaseOmaniBankParser() {

    override fun getBankName() = "National Bank of Oman"

    override fun canHandle(sender: String): Boolean {
        val normalized = sender.uppercase()
        return normalized.contains("NBO") ||
                sender.contains("بنك عمان الوطني")
    }

    override fun extractMerchant(message: String, sender: String): String? {
        // "Domestic Transfer To BENEFICIARY." — the transfer beneficiary is the
        // most useful label. Own-account transfers stay unlabeled (masked own account).
        DOMESTIC_BENEFICIARY.find(message)?.let { match ->
            val cleaned = cleanOmaniMerchant(match.groupValues[1])
            if (isValidOmaniMerchantName(cleaned)) return cleaned
        }
        return super.extractMerchant(message, sender)
    }

    private companion object {
        private val DOMESTIC_BENEFICIARY = Regex("""Domestic Transfer To\s+(.+?)(?:\.|$)""")
    }
}
