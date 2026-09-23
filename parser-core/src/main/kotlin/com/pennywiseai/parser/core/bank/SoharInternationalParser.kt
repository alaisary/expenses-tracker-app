package com.pennywiseai.parser.core.bank

import com.pennywiseai.parser.core.ParsedTransaction

/**
 * Parser for Sohar International Bank (Oman) SMS messages. Extends
 * [BaseOmaniBankParser].
 *
 * Bank attribution is LOW CONFIDENCE (round-1 open question: sender
 * "SoharIntl"/"SIB" unverified), so this parser is content-gated — it only
 * claims its template shapes and stays silent otherwise (a wrong attribution
 * can cause a missed parse, never a wrong transaction).
 *
 * Supported formats (English/Arabic twins, card ending with NNNN):
 * - "A transaction of OMR X was debited from your card ending with NNNN on
 *   yyyy-MM-dd HH:mm at MERCHANT. Your available balance is OMR B"
 *   (balance sometimes glued: "isOMR B"; merchant field dot-terminated and
 *   often dotted + space-padded with a location suffix)
 * - "تم إجراء معاملة بمبلغ OMR X على بطاقتك التي تنتهي بالرقم NNNN بتاريخ
 *   yyyy-dd-MM HH:mm في MERCHANT رصيدك الحالي OMR B"
 *
 * Currency: OMR. Senders (unverified): SoharIntl / Sohar.
 */
class SoharInternationalParser : BaseOmaniBankParser() {

    override fun getBankName() = "Sohar International"

    override fun canHandle(sender: String): Boolean {
        val normalized = sender.uppercase()
        return normalized.contains("SOHAR") ||
                sender.contains("بنك صحار")
    }

    override fun parse(smsBody: String, sender: String, timestamp: Long): ParsedTransaction? {
        val lower = smsBody.lowercase()
        val knownShape = lower.contains("was debited from your card ending with") ||
                smsBody.contains("تنتهي بالرقم")
        if (!knownShape) return null
        return super.parse(smsBody, sender, timestamp)
    }
}
