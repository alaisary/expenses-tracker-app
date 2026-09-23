package com.pennywiseai.parser.core.bank

import com.pennywiseai.parser.core.ParsedTransaction

/**
 * Parser for Oman Arab Bank (OAB, Oman) SMS messages. Extends
 * [BaseOmaniBankParser]; adds the OAB-specific merchant quirk.
 *
 * Bank attribution is LOW CONFIDENCE (round-1 open question: BIN 419291 owner,
 * families 12–17), so this parser is content-gated — it only claims its template
 * shapes and stays silent otherwise (a wrong attribution can cause a missed
 * parse, never a wrong transaction).
 *
 * Supported formats:
 * - Debit card (BIN 419291): "Dear Customer, Your Debit Card No 419291######NNNN
 *   for Acct MASK has been debited for OMRX.NNN on DATE for MERCHANT\LOC\….
 *   Available Bal OMR B" (amount glued to OMR; merchant after the second "for")
 * - Debit card used: "Your Debit Card No XXNNNN has been used at MERCHANT for an
 *   amount of OMR X on DATE. Your available balance is OMR B."
 * - ر.ع merchant-as-account: "Shortتم خصم ر.ع X من حسابك رقم MERCHANT\TOWN\ في
 *   DD-MMM , HH:mm:ss  الرصيد المتوفر هو B  ر.ع" (merchant injected as the
 *   account field; "Short" concat prefix stripped by the base class)
 * - Trailing-minus: "تم خصم مبلغ X- ر.ع. من حسابك رقم NNN*NNNN [عن طريق بطاقة
 *   الخصم المباشر المنتهية بـ NNNN في MERCHANT.] رصيدك الحالي هو: B . ر.ع."
 * - Savings debit: "تم خصم  مبلغ X ر.ع من حساب التوفير رقم MASK بتاريخ DATE
 *   للحساب باسم MERCHANT رصيدك الحالي B ر.ع"
 *
 * Currency: OMR. Senders (unverified): OMANAB / OAB / بنك عمان العربي.
 */
class OmanArabBankParser : BaseOmaniBankParser() {

    override fun getBankName() = "Oman Arab Bank"

    override fun canHandle(sender: String): Boolean {
        val normalized = sender.uppercase()
        return normalized.contains("OMANAB") ||
                normalized.contains("OAB") ||
                normalized.contains("OMAN ARAB") ||
                sender.contains("بنك عمان العربي")
    }

    override fun parse(smsBody: String, sender: String, timestamp: Long): ParsedTransaction? {
        val lower = smsBody.lowercase()
        val knownShape = lower.contains("debit card no 419291") ||
                lower.contains("has been used at") ||
                (smsBody.contains("ر.ع") &&
                        (smsBody.contains("حسابك رقم") || smsBody.contains("حساب التوفير")))
        if (!knownShape) return null
        return super.parse(smsBody, sender, timestamp)
    }

    override fun extractMerchant(message: String, sender: String): String? {
        // "… has been debited for OMR2.000 on 06/08/2026 23:16:57 for
        // MERCHANT\LOC\…." — the merchant follows the SECOND "for", so the
        // generic for-pattern would swallow the amount+date in between.
        DEBIT_CARD_MERCHANT.find(message)?.let { match ->
            val cleaned = cleanOmaniMerchant(match.groupValues[1])
            if (isValidOmaniMerchantName(cleaned)) return cleaned
        }
        return super.extractMerchant(message, sender)
    }

    private companion object {
        private val DEBIT_CARD_MERCHANT = Regex(
            """debited for\s+\S+\s+on\s+[0-9/]+\s+[0-9:]+\s+for\s+([^\s\\]+)"""
        )
    }
}
