package com.pennywiseai.parser.core.bank

import com.pennywiseai.parser.core.ParsedTransaction

/**
 * Parser for Meethaq (Bank Sohar's Islamic window — now Meethaq Islamic Bank,
 * Oman) SMS messages. Extends [BaseOmaniBankParser].
 *
 * Meethaq and Bank Muscat use overlapping alert wordings (same SMS vendor), so
 * this parser is strictly content-gated: it only claims messages that identify
 * Meethaq by name (Arabic or English), by the "Txn Id MTHQ…" reference prefix,
 * or by a Meethaq-issued card BIN. Everything else falls through.
 *
 * Supported formats:
 * - Card payment: "Dear Customer, Your a/c no MASK has been debited for an amt of
 *   OMR X as payment towards Meethaq Credit Card No. MASK" → TRANSFER
 * - Mobile payment: "Dear Customer, You have sent OMR X to NAME from your a/c MASK
 *   … Txn Id MTHQ…. Avl Bal OMR B." → TRANSFER
 * - Credit card spend: "Card 447627******NNNN used for OMR X at MERCHANT on DATE.
 *   Available limit OMR L." (BINs 447627/483430 only) → EXPENSE
 *
 * The Arabic "عزيزي العميل … في تمام الساعة" family uses 11xx account masks
 * (Meethaq traffic uses 06xx), so it is deliberately NOT claimed.
 *
 * Currency: OMR. Senders: Meethaq / MTHQ / ميثاق.
 */
class MeethaqParser : BaseOmaniBankParser() {

    override fun getBankName() = "Meethaq"

    override fun canHandle(sender: String): Boolean {
        val normalized = sender.uppercase()
        return normalized.contains("MEETHAQ") ||
                normalized.contains("MTHQ") ||
                sender.contains("ميثاق")
    }

    override fun parse(smsBody: String, sender: String, timestamp: Long): ParsedTransaction? {
        val lower = smsBody.lowercase()
        val selfIdentified = lower.contains("meethaq") ||
                smsBody.contains("ميثاق") ||
                lower.contains("txn id mthq")
        if (!selfIdentified && !MEETHAQ_CARD_SPEND.containsMatchIn(smsBody)) return null
        return super.parse(smsBody, sender, timestamp)
    }

    private companion object {
        // Meethaq-issued Visa/Mastercard BINs observed on card-spend alerts.
        private val MEETHAQ_CARD_SPEND =
            Regex("""Card\s+(?:447627|483430)\*""", RegexOption.IGNORE_CASE)
    }
}
