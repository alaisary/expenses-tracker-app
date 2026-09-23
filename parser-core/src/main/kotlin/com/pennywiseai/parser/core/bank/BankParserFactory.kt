package com.pennywiseai.parser.core.bank

import com.pennywiseai.parser.core.ParsedTransaction

/**
 * Factory for creating bank-specific parsers based on SMS sender.
 *
 * This build targets Omani banks (and Omani wallets) only, so the registry
 * below is intentionally limited to those parsers. Keeping the scan set small
 * keeps SMS parsing fast — the factory is consulted for every message.
 */
object BankParserFactory {

    private val parsers = listOf(
        AlizzIslamicBankParser(),  // Alizz Islamic Bank (Oman) — before BankMuscat: Alizz's English debit wording is generic
        BankMuscatParser(),  // Bank Muscat (Oman)
        BankDhofarParser(),  // Bank Dhofar (Oman)
        BankNizwaParser(),  // Bank Nizwa (Oman) — Arabic-only alerts; own sender, no overlap
        OmanArabBankParser(),  // Oman Arab Bank (Oman)
        SoharInternationalParser(),  // Sohar International (Oman)
        NBOParser(),  // National Bank of Oman (NBO)
        MeethaqParser(),  // Meethaq (Oman) — after BM: shared alert wordings, claims only Meethaq-identified content
        ThawaniParser()  // Thawani / Mojab wallet (Oman) — after bank parsers: bank messages mention "Thawani APP" as merchant
    )

    /**
     * Returns the appropriate bank parser for the given sender.
     * Returns null if no specific parser is found.
     */
    fun getParser(sender: String): BankParser? {
        return parsers.firstOrNull { it.canHandle(sender) }
    }

    /**
     * Returns every parser whose canHandle matches the sender.
     * Multiple parsers can share a sender (e.g. M-Pesa Kenya/Tanzania/Mozambique);
     * content-aware dispatch in [parse] picks the right one.
     */
    fun getParsers(sender: String): List<BankParser> = parsers.filter { it.canHandle(sender) }

    /**
     * Content-aware dispatch: tries every canHandle-matching parser and returns the
     * first non-null parse(). This un-shadows parsers that share a sender ID — each
     * parser gates its own parse() by message content and returns null otherwise.
     */
    fun parse(smsBody: String, sender: String, timestamp: Long): ParsedTransaction? =
        getParsers(sender).firstNotNullOfOrNull { it.parse(smsBody, sender, timestamp) }

    /**
     * Returns the bank parser for the given bank name.
     * Returns null if no specific parser is found.
     */
    fun getParserByName(bankName: String): BankParser? {
        return parsers.firstOrNull { it.getBankName() == bankName }
    }

    /**
     * Returns all available bank parsers.
     */
    fun getAllParsers(): List<BankParser> = parsers

    /**
     * Checks if the sender belongs to any known bank.
     */
    fun isKnownBankSender(sender: String): Boolean {
        return parsers.any { it.canHandle(sender) }
    }
}
