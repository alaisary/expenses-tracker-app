package com.pennywiseai.parser.core.bank

import com.pennywiseai.parser.core.TransactionType
import com.pennywiseai.parser.core.test.ExpectedTransaction
import com.pennywiseai.parser.core.test.ParserTestCase
import com.pennywiseai.parser.core.test.ParserTestUtils
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import java.math.BigDecimal

class MeethaqParserTest {

    @TestFactory
    fun `meethaq parser handles identified content only`(): List<DynamicTest> {
        val parser = MeethaqParser()

        val testCases = listOf(
            ParserTestCase(
                name = "Card payment towards Meethaq credit card",
                message = "Dear Customer, Your a/c no 0611XXXXXXXX0001 has been debited for an amt of OMR 141.475 as payment towards Meethaq Credit Card No. 4476XXXXXXXX9662",
                sender = "Meethaq",
                expected = ExpectedTransaction(
                    amount = BigDecimal("141.475"),
                    currency = "OMR",
                    type = TransactionType.TRANSFER,
                    accountLast4 = "0001"
                )
            ),
            ParserTestCase(
                name = "Mobile payment sent (MTHQ txn id)",
                message = "Dear Customer, You have sent OMR 17.000 to ALSAXXXXXXXXXXXXXXXXXXXXXXXXAIDI from your a/c 0611XXXXXXXX0019 on 07/08/2026 18:21:59 using Mobile Payment services. Txn Id MTHQ000210335614. Avl Bal OMR 89.467.",
                sender = "Meethaq",
                expected = ExpectedTransaction(
                    amount = BigDecimal("17.000"),
                    currency = "OMR",
                    type = TransactionType.TRANSFER,
                    merchant = "ALSAXXXXXXXXXXXXXXXXXXXXXXXXAIDI",
                    accountLast4 = "0019",
                    reference = "MTHQ000210335614",
                    balance = BigDecimal("89.467")
                )
            ),
            ParserTestCase(
                name = "Credit card spend on a Meethaq-issued BIN",
                message = "Card 447627******9662 used for OMR 1.000 at SOME SHOP on 07/08/2026 09:35:30. Available limit OMR 100.000.",
                sender = "Meethaq",
                expected = ExpectedTransaction(
                    amount = BigDecimal("1.000"),
                    currency = "OMR",
                    type = TransactionType.EXPENSE,
                    merchant = "SOME SHOP",
                    accountLast4 = "9662",
                    isFromCard = true
                )
            ),
            ParserTestCase(
                name = "Bank Muscat card payment is not claimed (no Meethaq marker)",
                message = "Dear Customer, Your a/c no 0383XXXXXXXX0039 has been debited for an amt of OMR 143.520 as payment towards bank muscat Credit Card No. 4132XXXXXXXX0580",
                sender = "Meethaq",
                shouldParse = false
            ),
            ParserTestCase(
                name = "Non-Meethaq card BIN is not claimed",
                message = "Card 420460******0326 used for OMR 1.850 at GROCERY SHOP on 07/08/2026 09:35:30. Available limit OMR 359.885.",
                sender = "Meethaq",
                shouldParse = false
            )
        )

        val handleCases = listOf(
            "Meethaq" to true,
            "MTHQ" to true,
            "ميثاق" to true,
            "HSBC" to false,
            "" to false
        )

        return ParserTestUtils.runTestSuite(parser, testCases, handleCases, "Meethaq Parser Tests")
    }
}
