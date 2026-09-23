package com.pennywiseai.parser.core.bank

import com.pennywiseai.parser.core.TransactionType
import com.pennywiseai.parser.core.test.ExpectedTransaction
import com.pennywiseai.parser.core.test.ParserTestCase
import com.pennywiseai.parser.core.test.ParserTestUtils
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import java.math.BigDecimal

class BankDhofarParserTest {

    @TestFactory
    fun `bank dhofar parser handles possale`(): List<DynamicTest> {
        val parser = BankDhofarParser()

        val testCases = listOf(
            ParserTestCase(
                name = "POS sale with RRN and auth code",
                message = "Thank you for using Bank Dhofar POSSale made at ZAHRAT ALHAILA TRADING EST for OMR. 4.000 with CardNo X6189 RRN 621907952044  AuthCode 845004 on 07-08-2026 at 12:40:57",
                sender = "BKDhofar",
                expected = ExpectedTransaction(
                    amount = BigDecimal("4.000"),
                    currency = "OMR",
                    type = TransactionType.EXPENSE,
                    merchant = "ZAHRAT ALHAILA TRADING EST",
                    accountLast4 = "6189",
                    reference = "621907952044",
                    isFromCard = true
                )
            )
        )

        val handleCases = listOf(
            "BKDhofar" to true,
            "BankDhofar" to true,
            "بنك ظفار" to true,
            "HSBC" to false,
            "" to false
        )

        return ParserTestUtils.runTestSuite(parser, testCases, handleCases, "Bank Dhofar Parser Tests")
    }
}
