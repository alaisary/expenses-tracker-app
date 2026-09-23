package com.pennywiseai.parser.core.bank

import com.pennywiseai.parser.core.TransactionType
import com.pennywiseai.parser.core.test.ExpectedTransaction
import com.pennywiseai.parser.core.test.ParserTestCase
import com.pennywiseai.parser.core.test.ParserTestUtils
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import java.math.BigDecimal

class NBOParserTest {

    @TestFactory
    fun `nbo parser handles known templates`(): List<DynamicTest> {
        val parser = NBOParser()

        val testCases = listOf(
            ParserTestCase(
                name = "NBO Wallet debit (no account number)",
                message = "OMR  9.000, is debited from your NBO Wallet on 09/08/2026 09:18:52 AM. Your new Available balance is 0.000. Thank you for Banking with NBO.",
                sender = "NBO",
                expected = ExpectedTransaction(
                    amount = BigDecimal("9.000"),
                    currency = "OMR",
                    type = TransactionType.EXPENSE,
                    balance = BigDecimal("0.000"),
                    expectNullAccountLast4 = true
                )
            ),
            ParserTestCase(
                name = "NBO Wallet debit with balance",
                message = "OMR  10.000, is debited from your NBO Wallet on 18/07/2026 02:07:34 AM. Your new Available balance is 455.600. Thank you for Banking with NBO.",
                sender = "NBO",
                expected = ExpectedTransaction(
                    amount = BigDecimal("10.000"),
                    currency = "OMR",
                    type = TransactionType.EXPENSE,
                    balance = BigDecimal("455.600"),
                    expectNullAccountLast4 = true
                )
            ),
            ParserTestCase(
                name = "Monthly loan installment",
                message = "OMR 408.059 has been debited from your A/C 1019XXX017 for your monthly loan installment. Your available balance is OMR 869.600.For more info download theapp  www.nbo.om/mb",
                sender = "NBO",
                expected = ExpectedTransaction(
                    amount = BigDecimal("408.059"),
                    currency = "OMR",
                    type = TransactionType.EXPENSE,
                    accountLast4 = "9017",
                    balance = BigDecimal("869.600")
                )
            ),
            ParserTestCase(
                name = "Plain account debit with 'balance in your account' phrasing",
                message = "OMR 1.050 is debited from your A/C 1047**001 on 22-08-2024 06:02. \nAvailable balance in your account is OMR 33.898.",
                sender = "NBO",
                expected = ExpectedTransaction(
                    amount = BigDecimal("1.050"),
                    currency = "OMR",
                    type = TransactionType.EXPENSE,
                    accountLast4 = "7001",
                    balance = BigDecimal("33.898")
                )
            ),
            ParserTestCase(
                name = "Own account transfer (English)",
                message = "Account XXXXXXX74039 is debited OMR 1.8 on 17-08-2026 as Own Account Transfer To XXXX7587. Available balance OMR 28.15.",
                sender = "NBO",
                expected = ExpectedTransaction(
                    amount = BigDecimal("1.8"),
                    currency = "OMR",
                    type = TransactionType.TRANSFER,
                    accountLast4 = "4039",
                    balance = BigDecimal("28.15")
                )
            ),
            ParserTestCase(
                name = "Domestic transfer (Arabic) with beneficiary",
                message = "تم سحب OMR 350 من الحساب XXXXXXX05696 بتاريخ 06-08-2026 عن طريق Domestic Transfer To NAKHIL FANJAA. الرصيد الحالي OMR2926.029",
                sender = "NBO",
                expected = ExpectedTransaction(
                    amount = BigDecimal("350"),
                    currency = "OMR",
                    type = TransactionType.TRANSFER,
                    merchant = "NAKHIL FANJAA",
                    accountLast4 = "5696",
                    balance = BigDecimal("2926.029")
                )
            ),
            ParserTestCase(
                name = "Own account deposit (Arabic) stays a transfer",
                message = "تم إيداع OMR 5 في الحساب XXXXXXX87912 بتاريخ 01-08-2026 عن طريق Own Account Transfer From XXXX6838. الرصيد الحالي OMR5.249",
                sender = "NBO",
                expected = ExpectedTransaction(
                    amount = BigDecimal("5"),
                    currency = "OMR",
                    type = TransactionType.TRANSFER,
                    accountLast4 = "7912",
                    balance = BigDecimal("5.249")
                )
            )
        )

        val handleCases = listOf(
            "NBO" to true,
            "NBOALERT" to true,
            "بنك عمان الوطني" to true,
            "HSBC" to false,
            "" to false
        )

        return ParserTestUtils.runTestSuite(parser, testCases, handleCases, "NBO Parser Tests")
    }
}
