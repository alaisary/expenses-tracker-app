package com.pennywiseai.parser.core.bank

import com.pennywiseai.parser.core.TransactionType
import com.pennywiseai.parser.core.test.ExpectedTransaction
import com.pennywiseai.parser.core.test.ParserTestCase
import com.pennywiseai.parser.core.test.ParserTestUtils
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import java.math.BigDecimal

class ThawaniParserTest {

    @TestFactory
    fun `thawani parser handles mojab card templates`(): List<DynamicTest> {
        val parser = ThawaniParser()

        val testCases = listOf(
            ParserTestCase(
                name = "USD cross-currency charge keeps USD tag",
                message = "عزيزي المستخدم ، تم استخدام بطاقة موجب الخاصة بك رقم 4027XXXXXXXX8069 لدفع مبلغ 4.99 USD  إلى Google YouTube Member   6 بتاريخ 07/08/2026. الرصيد المتاح لديك هو 8.031 OMR ر.ع.",
                sender = "Thawani",
                expected = ExpectedTransaction(
                    amount = BigDecimal("4.99"),
                    currency = "USD",
                    type = TransactionType.EXPENSE,
                    merchant = "Google YouTube Member",
                    accountLast4 = "8069",
                    balance = BigDecimal("8.031"),
                    isFromCard = true
                )
            ),
            ParserTestCase(
                name = "OMR top-up with padded merchant and zero balance",
                message = "عزيزي المستخدم ، تم استخدام بطاقة موجب الخاصة بك رقم 4027XXXXXXXX8723 لدفع مبلغ 4.073 OMR  إلى Thawani APP             > بتاريخ 07/08/2026. الرصيد المتاح لديك هو .000 OMR ر.ع.",
                sender = "Thawani",
                expected = ExpectedTransaction(
                    amount = BigDecimal("4.073"),
                    currency = "OMR",
                    type = TransactionType.EXPENSE,
                    merchant = "Thawani APP",
                    accountLast4 = "8723",
                    balance = BigDecimal("0.000"),
                    isFromCard = true
                )
            ),
            ParserTestCase(
                name = "Amount with missing leading zero",
                message = "عزيزي المستخدم ، تم استخدام بطاقة موجب الخاصة بك رقم 4027XXXXXXXX8959 لدفع مبلغ .200 OMR  إلى Thawani APP             > بتاريخ 07/08/2026. الرصيد المتاح لديك هو 61.482 OMR ر.ع.",
                sender = "Thawani",
                expected = ExpectedTransaction(
                    amount = BigDecimal("0.200"),
                    currency = "OMR",
                    type = TransactionType.EXPENSE,
                    merchant = "Thawani APP",
                    accountLast4 = "8959",
                    balance = BigDecimal("61.482"),
                    isFromCard = true
                )
            ),
            ParserTestCase(
                name = "Bank-owned ThawaniPay descriptor is not claimed (content gate)",
                message = "Your SAVING A/C 70702#######01 has been debited OMR 5.000 on 03/08/2026 20:21:25 for ThawaniPay APP. Your available balance is OMR 2.771",
                sender = "Thawani",
                shouldParse = false
            )
        )

        val handleCases = listOf(
            "Thawani" to true,
            "THAWANIPAY" to true,
            "Mojab" to true,
            "ثواني" to true,
            "HSBC" to false,
            "" to false
        )

        return ParserTestUtils.runTestSuite(parser, testCases, handleCases, "Thawani Parser Tests")
    }
}
