package com.pennywiseai.parser.core.bank

/**
 * Parser for Bank Dhofar (Oman) SMS messages. Extends [BaseOmaniBankParser].
 *
 * Supported formats:
 * - POS sale: "Thank you for using Bank Dhofar POSSale made at MERCHANT for
 *   OMR. X with CardNo XXXXXX RRN N  AuthCode N on DATE at HH:mm:ss"
 *   (EXPENSE, isFromCard, RRN as reference; no balance)
 *
 * Note: the high-volume English "Card of a/c … Avl Bal" family was re-attributed
 * to Bank Muscat in round 2 (account-mask forensics). Remaining Dhofar coverage
 * (Arabic ATM etc.) needs sender-verified samples.
 *
 * Currency: OMR. Senders: BKDhofar / BankDhofar / بنك ظفار.
 */
class BankDhofarParser : BaseOmaniBankParser() {

    override fun getBankName() = "Bank Dhofar"

    override fun canHandle(sender: String): Boolean {
        val normalized = sender.uppercase()
        return normalized.contains("DHOFAR") ||
                sender.contains("بنك ظفار")
    }
}
