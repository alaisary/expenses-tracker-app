package com.pennywiseai.parser.core.bank

import com.pennywiseai.parser.core.TransactionType
import com.pennywiseai.parser.core.test.ExpectedTransaction
import com.pennywiseai.parser.core.test.ParserTestCase
import com.pennywiseai.parser.core.test.ParserTestUtils
import com.pennywiseai.parser.core.test.SimpleTestCase
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import java.math.BigDecimal

class BankNizwaParserTest {

    @TestFactory
    fun `test Bank Nizwa parser`(): List<DynamicTest> {
        val parser = BankNizwaParser()

        ParserTestUtils.printTestHeader(
            parserName = "Bank Nizwa",
            bankName = parser.getBankName(),
            currency = parser.getCurrency()
        )

        val testCases = listOf(
            ParserTestCase(
                name = "Arabic deposit (hamza-less ايداع) with reference and balance",
                message = "تم ايداع ر.ع   30.000  في حسابك رقم 001 ###### 00610  الرقم المرجعي. 48950972 . الرصيد المتوفر هو 31.500   ر.ع .",
                sender = "BANK NIZWA",
                expected = ExpectedTransaction(
                    amount = BigDecimal("30.000"),
                    currency = "OMR",
                    type = TransactionType.INCOME,
                    reference = "48950972",
                    accountLast4 = "0610",
                    balance = BigDecimal("31.500"),
                    isFromCard = false
                )
            ),
            ParserTestCase(
                name = "Arabic withdrawal",
                message = "تم سحب ر.ع 30.000 من حساب رقم 001 ###### 00610 الى KHAL########################D AL الرصيد المتوفر هو 1.500 ر.ع",
                sender = "BANK NIZWA",
                expected = ExpectedTransaction(
                    amount = BigDecimal("30.000"),
                    currency = "OMR",
                    type = TransactionType.EXPENSE,
                    accountLast4 = "0610",
                    balance = BigDecimal("1.500"),
                    isFromCard = false
                )
            ),
            ParserTestCase(
                name = "Arabic debit",
                message = "تم خصم ر.ع 1.500 من حسابك رقم 00000000\\ALKHALEEJ MARKET LLC\\Adam SEP, 20:35:23-20 في الرصيد المتوفر هو 1.500 ر.ع",
                sender = "BANK NIZWA",
                expected = ExpectedTransaction(
                    amount = BigDecimal("1.500"),
                    currency = "OMR",
                    type = TransactionType.EXPENSE,
                    balance = BigDecimal("1.500"),
                    isFromCard = false
                )
            ),
            ParserTestCase(
                name = "OTP is not a transaction",
                message = "#Your OTP for transaction is: 790505 ZVh4Vexqyxv",
                sender = "BANK NIZWA",
                shouldParse = false
            )
        )

        val handleCases = listOf(
            "BANK NIZWA" to true,
            "BankNizwa" to true,
            "بنك نزوى" to true,
            "BankMuscat" to false,
            "" to false
        )

        return ParserTestUtils.runTestSuite(parser, testCases, handleCases, "Bank Nizwa Parser Tests")
    }

    @TestFactory
    fun `factory resolves bank nizwa`(): List<DynamicTest> {
        val cases = listOf(
            SimpleTestCase(
                bankName = "Bank Nizwa",
                sender = "BANK NIZWA",
                currency = "OMR",
                message = "تم ايداع ر.ع   30.000  في حسابك رقم 001 ###### 00610  الرقم المرجعي. 48950972 . الرصيد المتوفر هو 31.500   ر.ع .",
                expected = ExpectedTransaction(
                    amount = BigDecimal("30.000"),
                    currency = "OMR",
                    type = TransactionType.INCOME,
                    reference = "48950972",
                    accountLast4 = "0610",
                    balance = BigDecimal("31.500")
                )
            )
        )

        return ParserTestUtils.runFactoryTestSuite(cases, "Bank Nizwa factory tests")
    }
}
