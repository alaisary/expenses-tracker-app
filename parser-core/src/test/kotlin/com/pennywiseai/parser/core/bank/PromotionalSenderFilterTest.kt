package com.pennywiseai.parser.core.bank

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.math.BigDecimal

/**
 * Gateways route marketing SMS from senders ending in "-P" while account alerts
 * arrive on the transactional route ("-T"). A campaign body can quote a figure —
 * "Minimum spend OMR 5.000" — that the generic amount/type extraction would book
 * as a real spend, so the sender route, not the body, has to decide.
 *
 * Regression: a Bank Muscat campaign from "BANKMUSCAT-P" produced a phantom
 * OMR 5.000 expense before the suffix check was added.
 */
class PromotionalSenderFilterTest {

    private val campaignBody =
        "Enjoy 10% back when you shop used at any partner outlet. Minimum spend OMR 5.000. Valid till 30/09/2026."

    @Test
    fun `promotional sender route is never parsed as a transaction`() {
        val parser = BankMuscatParser()

        assertNull(parser.parse(campaignBody, "BANKMUSCAT-P", 0L))

        // The suffix is matched case-insensitively and survives padding.
        assertNull(parser.parse(campaignBody, "bankmuscat-p ", 0L))
    }

    @Test
    fun `identical body from the transactional route still parses`() {
        val parser = BankMuscatParser()

        val parsed = parser.parse(
            "OMR 140 has been debited from your account 0010********4005 on 2026-08-07 09:32:28." +
                "  Your available balance is OMR 701.504",
            "BANKMUSCAT",
            0L
        )

        assertNotNull(parsed)
        // Scale-insensitive compare: the parser may render 140 as 140.000.
        assertEquals(0, BigDecimal("140").compareTo(parsed!!.amount))
    }
}
