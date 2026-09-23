package com.pennywiseai.parser.core.bank

import com.pennywiseai.parser.core.TransactionType
import com.pennywiseai.parser.core.test.ExpectedTransaction
import com.pennywiseai.parser.core.test.ParserTestCase
import com.pennywiseai.parser.core.test.ParserTestUtils
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import java.math.BigDecimal

class OmanArabBankParserTest {

    @TestFactory
    fun `oman arab bank parser handles gated templates`(): List<DynamicTest> {
        val parser = OmanArabBankParser()

        val testCases = listOf(
            ParserTestCase(
                name = "Debit card BIN 419291 - merchant with backslash location",
                message = "Dear Customer, Your Debit Card No 419291######8055 for Acct 0112######001 has been debited for OMR2.000 on 06/08/2026 23:16:57 for AMALALAZIZIATRADE\\ALSB\\OM\\1234. Available Bal OMR 0.938",
                sender = "OMANAB",
                expected = ExpectedTransaction(
                    amount = BigDecimal("2.000"),
                    currency = "OMR",
                    type = TransactionType.EXPENSE,
                    merchant = "AMALALAZIZIATRADE",
                    accountLast4 = "2001",
                    balance = BigDecimal("0.938"),
                    isFromCard = true
                )
            ),
            ParserTestCase(
                name = "Debit card - merchant with glued numeric terminal prefix",
                message = "Dear Customer, Your Debit Card No 419291######5377 for Acct 0105######001 has been debited for OMR11.201 on 07/08/2026 17:47:05 for 7006OMANOILRUMAISNEBARKA\\. Available Bal OMR 35.013",
                sender = "OMANAB",
                expected = ExpectedTransaction(
                    amount = BigDecimal("11.201"),
                    currency = "OMR",
                    type = TransactionType.EXPENSE,
                    merchant = "OMANOILRUMAISNEBARKA",
                    accountLast4 = "5001",
                    balance = BigDecimal("35.013"),
                    isFromCard = true
                )
            ),
            ParserTestCase(
                name = "Debit card used at merchant",
                message = "Your Debit Card No XX3502 has been used at ROAST CCX HYDERABAD IN for an amount of OMR 8.534 on 07/08/2026. Your available balance is OMR 183.984.",
                sender = "OMANAB",
                expected = ExpectedTransaction(
                    amount = BigDecimal("8.534"),
                    currency = "OMR",
                    type = TransactionType.EXPENSE,
                    merchant = "ROAST CCX HYDERABAD IN",
                    accountLast4 = "3502",
                    balance = BigDecimal("183.984"),
                    isFromCard = true
                )
            ),
            ParserTestCase(
                name = "Arabic rial token with merchant-as-account (Short prefix)",
                message = "Shortتم خصم ر.ع 0.550 من حسابك رقم EXPRESS LINE HYPERMARKE\\\\ALFALAJ\\      0  في 01-AUG , 19:18:08  الرصيد المتوفر هو 180.954  ر.ع",
                sender = "OMANAB",
                expected = ExpectedTransaction(
                    amount = BigDecimal("0.550"),
                    currency = "OMR",
                    type = TransactionType.EXPENSE,
                    merchant = "EXPRESS LINE HYPERMARKE",
                    balance = BigDecimal("180.954"),
                    expectNullAccountLast4 = true
                )
            ),
            ParserTestCase(
                name = "Trailing-minus phone-payment debit",
                message = "تم خصم مبلغ 0.300- ر.ع. من حسابك رقم 500*3101 بإستخدام الدفع عبر رقم الهاتف عن طريق التطبيق إلى الرقم 0096896396046 بتاريخ 2026-08-17 . رصيدك الحالي هو: 0.185 . ر.ع.",
                sender = "OMANAB",
                expected = ExpectedTransaction(
                    amount = BigDecimal("0.300"),
                    currency = "OMR",
                    type = TransactionType.EXPENSE,
                    accountLast4 = "3101",
                    balance = BigDecimal("0.185")
                )
            ),
            ParserTestCase(
                name = "Trailing-minus card debit with merchant",
                message = "تم خصم مبلغ 0.468- ر.ع. من حسابك رقم 3146*700 عن طريق بطاقة الخصم المباشر المنتهية بـ 8890 في ISFAHAN SWEETS. رصيدك الحالي هو: 4.092. ر.ع.",
                sender = "OMANAB",
                expected = ExpectedTransaction(
                    amount = BigDecimal("0.468"),
                    currency = "OMR",
                    type = TransactionType.EXPENSE,
                    merchant = "ISFAHAN SWEETS",
                    accountLast4 = "6700",
                    balance = BigDecimal("4.092"),
                    isFromCard = true
                )
            ),
            ParserTestCase(
                name = "Savings account debit with merchant name",
                message = "تم خصم  مبلغ 5.270 ر.ع من حساب التوفير رقم 01#######72102 بتاريخ 11/08/2026 00:51:54 للحساب باسم NAMA E PORTAL PREPAID   24251011     O. رصيدك الحالي 7,839.615 ر.ع",
                sender = "OMANAB",
                expected = ExpectedTransaction(
                    amount = BigDecimal("5.270"),
                    currency = "OMR",
                    type = TransactionType.EXPENSE,
                    merchant = "NAMA E PORTAL PREPAID",
                    accountLast4 = "2102",
                    balance = BigDecimal("7839.615")
                )
            ),
            ParserTestCase(
                name = "Bank Muscat style OMR debit is not claimed (content gate)",
                message = "تم خصم OMR 0.650 من حسابك رقم XXXXX9999 بإستخدام بطاقة الخصم المباشر في TEST MERCHANT بتاريخ 2026/08/01 10:00:00. رصيدك الحالي هو 9.000 OMR.",
                sender = "OMANAB",
                shouldParse = false
            )
        )

        val handleCases = listOf(
            "OMANAB" to true,
            "OAB" to true,
            "بنك عمان العربي" to true,
            "HSBC" to false,
            "" to false
        )

        return ParserTestUtils.runTestSuite(parser, testCases, handleCases, "Oman Arab Bank Parser Tests")
    }
}
