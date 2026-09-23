package com.pennywiseai.parser.core.bank

import com.pennywiseai.parser.core.TransactionType
import com.pennywiseai.parser.core.test.ExpectedTransaction
import com.pennywiseai.parser.core.test.ParserTestCase
import com.pennywiseai.parser.core.test.ParserTestUtils
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import java.math.BigDecimal

class SoharInternationalParserTest {

    @TestFactory
    fun `sohar international parser handles gated templates`(): List<DynamicTest> {
        val parser = SoharInternationalParser()

        val testCases = listOf(
            ParserTestCase(
                name = "Card ending with - dot-terminated merchant",
                message = "A transaction of OMR 3.000 was debited from your card ending with 1083 on 2026-08-07 11:02 at Dar Al Atta APP. Your available balance is OMR 1974.984",
                sender = "SoharIntl",
                expected = ExpectedTransaction(
                    amount = BigDecimal("3.000"),
                    currency = "OMR",
                    type = TransactionType.EXPENSE,
                    merchant = "Dar Al Atta APP",
                    accountLast4 = "1083",
                    balance = BigDecimal("1974.984"),
                    isFromCard = true
                )
            ),
            ParserTestCase(
                name = "Card ending with - doubled dots and location padding, glued isOMR",
                message = "A transaction of OMR 5.750 was debited from your card ending with 2730 on 01-08-2026 20:01 at STAR CINEMAS SPC..       MUSCAT.. Your available balance isOMR 301.185",
                sender = "SoharIntl",
                expected = ExpectedTransaction(
                    amount = BigDecimal("5.750"),
                    currency = "OMR",
                    type = TransactionType.EXPENSE,
                    merchant = "STAR CINEMAS SPC",
                    accountLast4 = "2730",
                    balance = BigDecimal("301.185"),
                    isFromCard = true
                )
            ),
            ParserTestCase(
                name = "Card ending with - dot-joined location in merchant field",
                message = "A transaction of OMR 0.500 was debited from your card ending with 2101 on 04-08-2026 17:16 at GENACOM DELIVERY.AMERAT. OMAN.. Your available balance is OMR 38.844",
                sender = "SoharIntl",
                expected = ExpectedTransaction(
                    amount = BigDecimal("0.500"),
                    currency = "OMR",
                    type = TransactionType.EXPENSE,
                    merchant = "GENACOM DELIVERY",
                    accountLast4 = "2101",
                    balance = BigDecimal("38.844"),
                    isFromCard = true
                )
            ),
            ParserTestCase(
                name = "Arabic twin with newline before card digits",
                message = "تم إجراء معاملة بمبلغ OMR 0.500 على بطاقتك التي تنتهي بالرقم\n5769 بتاريخ 2026-01-08 19:56 في NUJOOM HYPERMARKET..     AL AMERAT رصيدك الحالي OMR 14.137",
                sender = "SoharIntl",
                expected = ExpectedTransaction(
                    amount = BigDecimal("0.500"),
                    currency = "OMR",
                    type = TransactionType.EXPENSE,
                    merchant = "NUJOOM HYPERMARKET",
                    accountLast4 = "5769",
                    balance = BigDecimal("14.137"),
                    isFromCard = true
                )
            ),
            ParserTestCase(
                name = "Bank Muscat style card-of-a/c is not claimed (content gate)",
                message = "Card of a/c 0425XXXXXXXX0011 used for OMR 2.310 at 55 COFFEE on 01/08/2026 19:36:19. Avl Bal OMR 28.310.",
                sender = "SoharIntl",
                shouldParse = false
            )
        )

        val handleCases = listOf(
            "SOHARINTL" to true,
            "Sohar" to true,
            "بنك صحار" to true,
            "HSBC" to false,
            "" to false
        )

        return ParserTestUtils.runTestSuite(parser, testCases, handleCases, "Sohar International Parser Tests")
    }
}
