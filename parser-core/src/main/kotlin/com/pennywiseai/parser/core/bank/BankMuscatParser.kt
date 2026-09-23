package com.pennywiseai.parser.core.bank

import com.pennywiseai.parser.core.ParsedTransaction

/**
 * Parser for Bank Muscat (Oman) SMS messages. Extends [BaseOmaniBankParser]
 * for the shared Omani mechanics (OMR/ر.ع currency, 3-decimal amounts, Arabic
 * normalization, account masks, balances); this class adds only the
 * Bank-Muscat-specific shapes.
 *
 * Supported formats:
 *
 * Arabic:
 * - Debit card: "تم خصم X OMR من حسابك رقم MASK بإستخدام بطاقة الخصم المباشر في MERCHANT بتاريخ DATE. رصيدك الحالي هو BAL OMR."
 *   (amount before or after OMR; dates YYYYMMDD, YYYY/MM/DD HH:mm:ss, YYYY-MM-DD HH:mm:ss)
 * - Deposit: "تم إيداع X OMR في حسابك رقم MASK بتاريخ DATE. …"
 * - Transfer/payment: "تم تحويل" / "تم سداد"
 * - ATM withdrawal: "تم سحب X OMR من حسابك رقم MASK من جهاز الصراف الآلي بتاريخ …"
 *
 * English:
 * - Debit card spend: "Card of a/c MASK used for OMR X at MERCHANT on DATE. Avl Bal OMR B."
 * - Own-account transfer: "OMR X is debited from your A/C A and credited to your A/C B on DATE." → TRANSFER
 * - Account debit: "OMR X is debited from your a/c MASK on DATE. New Available Balance is OMR B."
 * - ATM withdrawal: "OMR X withdrawn from your a/c MASK through ATM on DATE. …"
 * - Credit card spend: "Card BIN******NNNN used for OMR X at MERCHANT on DATE. Available limit OMR L."
 *   (Meethaq-issued BINs are skipped — shared wording with MeethaqParser)
 * - Card payment: "Dear Customer, Your a/c no MASK has been debited for an amt of OMR X as payment
 *   towards bank muscat Credit Card No. MASK" → TRANSFER
 * - mBanking: "Your SAVING A/C MASK has been debited OMR X on DATE by Mobile Payment. …" and the
 *   "… for ThawaniPay APP\ …" variant
 * - Mobile payment: "Dear Customer, You have sent OMR X to NAME from your a/c MASK … Txn Id BMCT….
 *   Avl Bal OMR B." → TRANSFER
 *
 * Currency: OMR (Omani Rial)
 * Senders: BankMuscat, BKMUSCAT, bank muscat, بنك مسقط
 */
class BankMuscatParser : BaseOmaniBankParser() {

    override fun getBankName() = "Bank Muscat"

    override fun canHandle(sender: String): Boolean {
        val normalized = sender.uppercase()
        return normalized.contains("MUSCAT") ||
                normalized.contains("BKMUSCAT") ||
                normalized.contains("BANKMUSCAT") ||
                normalized.contains("BK MUSCAT") ||
                sender.contains("بنك مسقط")
    }

    override fun parse(smsBody: String, sender: String, timestamp: Long): ParsedTransaction? {
        // "Card BIN******NNNN used for OMR …" is shared credit-card wording:
        // Meethaq-issued BINs route to MeethaqParser when a sender is shared.
        if (MEETHAQ_CARD_SPEND.containsMatchIn(smsBody)) return null
        return super.parse(smsBody, sender, timestamp)
    }

    private companion object {
        private val MEETHAQ_CARD_SPEND =
            Regex("""Card\s+(?:447627|483430)\*""", RegexOption.IGNORE_CASE)
    }
}
