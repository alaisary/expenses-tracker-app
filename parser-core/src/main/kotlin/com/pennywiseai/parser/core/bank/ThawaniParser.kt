package com.pennywiseai.parser.core.bank

import com.pennywiseai.parser.core.ParsedTransaction

/**
 * Parser for Thawani / Mojab wallet (Oman) SMS messages. Extends
 * [BaseOmaniBankParser] for the shared Omani mechanics — the Mojab prepaid card
 * is Bank-Dhofar+Visa powered and its alerts are ordinary Omani card SMS.
 *
 * Supported format — Mojab prepaid card spend (top-ups and purchases):
 * "عزيزي المستخدم ، تم استخدام بطاقة موجب الخاصة بك رقم MASK لدفع مبلغ X OMR|USD
 *  إلى MERCHANT بتاريخ DATE. الرصيد المتاح لديك هو B OMR ر.ع."
 *
 * - Cross-currency charges (e.g. "4.99 USD") keep their own currency tag; the
 *   OMR balance at the end is excluded from amount/currency detection by the
 *   base class's transaction-portion cut.
 * - Balance amounts can lose their leading zero (".000" → 0.000).
 * - The card mask supplies the account (4027XXXXXXXXNNNN); isMobileWallet stays
 *   false — this is a card with a last-4, not a bare balance wallet.
 *
 * Currency: OMR (per-message; USD occurs). Senders: Thawani / Mojab / ثواني.
 * Registered after the bank parsers so bank-owned "Thawani APP" descriptors
 * inside bank messages can never be claimed here.
 */
class ThawaniParser : BaseOmaniBankParser() {

    override fun getBankName() = "Thawani Wallet"

    override fun canHandle(sender: String): Boolean {
        val normalized = sender.uppercase()
        return normalized.contains("THAWANI") ||
                normalized.contains("MOJAB") ||
                sender.contains("ثواني")
    }

    override fun parse(smsBody: String, sender: String, timestamp: Long): ParsedTransaction? {
        // Only the Mojab prepaid-card template is Thawani's own; "Thawani APP"
        // merchant descriptors inside bank messages must not be claimed here.
        if (!smsBody.contains("بطاقة موجب")) return null
        return super.parse(smsBody, sender, timestamp)
    }
}
