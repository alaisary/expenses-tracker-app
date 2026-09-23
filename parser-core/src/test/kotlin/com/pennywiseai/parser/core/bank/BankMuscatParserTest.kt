package com.pennywiseai.parser.core.bank

import com.pennywiseai.parser.core.TransactionType
import com.pennywiseai.parser.core.test.ExpectedTransaction
import com.pennywiseai.parser.core.test.ParserTestCase
import com.pennywiseai.parser.core.test.ParserTestUtils
import com.pennywiseai.parser.core.test.SimpleTestCase
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import java.math.BigDecimal

class BankMuscatParserTest {

    /**
     * The generic (super) merchant patterns happily capture the account
     * fragment from "…debited from your account/a/c MASK…" phrasings. Those
     * must never surface as merchants (leaked as "your account 1600********2001",
     * "your A/C …" and post-cleanup "your A"/"your a" in real data).
     */
    @Test
    fun `generic account fragments never leak into merchant field`() {
        val alizz = AlizzIslamicBankParser()
        assertNull(
            alizz.parse(
                "OMR 140 has been debited from your account 0010********4005 on 2026-08-07 09:32:28.  Your available balance is OMR 701.504",
                "Alizz Bank", 0L
            )?.merchant
        )
        assertNull(
            alizz.parse(
                "OMR 0.100 has been debited from your account number 1600********4001 on 2025-03-27 09:52:08 Your available balance is OMR 2639.726",
                "Alizz Bank", 0L
            )?.merchant
        )

        val nbo = NBOParser()
        assertNull(
            nbo.parse(
                "OMR 0.525 is debited from your A/C 1047**001 on 01-09-2024 13:30. \nAvailable balance in your account is OMR 0.000.",
                "NBO", 0L
            )?.merchant
        )

        val bm = BankMuscatParser()
        assertNull(
            bm.parse(
                "OMR 5.000 is debited from your A/C 0322XXXXXXXX0027 and credited to your A/C 0322XXXXXXXX0011 on 01/08/2026 19:57:46.",
                "BankMuscat", 0L
            )?.merchant
        )
        assertNull(
            bm.parse(
                "OMR 9.000 is debited from your a/c 0430XXXXXXXX0017 on 30/01/2024 11:20:29. New Available Balance is OMR 45.214.",
                "BankMuscat", 0L
            )?.merchant
        )
    }

    @TestFactory
    fun `test Bank Muscat parser`(): List<DynamicTest> {
        val parser = BankMuscatParser()

        ParserTestUtils.printTestHeader(
            parserName = "Bank Muscat",
            bankName = parser.getBankName(),
            currency = parser.getCurrency()
        )

        val testCases = listOf(
            ParserTestCase(
                name = "Debit card purchase - merchant with leading ID",
                message = "تم خصم 0.650 OMR من حسابك رقم XXXXX9999 بإستخدام بطاقة الخصم المباشر في 757487-MASAKEN AL RAHA LLC KHOOM بتاريخ 2026/03/02 14:43:57. رصيدك الحالي هو 9999.740 OMR.",
                sender = "BankMuscat",
                expected = ExpectedTransaction(
                    amount = BigDecimal("0.650"),
                    currency = "OMR",
                    type = TransactionType.EXPENSE,
                    merchant = "MASAKEN AL RAHA LLC KHOOM",
                    accountLast4 = "9999",
                    balance = BigDecimal("9999.740"),
                    isFromCard = true
                )
            ),
            ParserTestCase(
                name = "Debit card purchase - merchant with trailing ID",
                message = "تم خصم OMR 0.100 من حسابك رقم XXXXXXX9999 بإستخدام بطاقة الخصم المباشر في Break Point QURU-650068 بتاريخ 2026/04/01 17:54:40. رصيدك الحالي هو 9999.740 OMR.",
                sender = "BankMuscat",
                expected = ExpectedTransaction(
                    amount = BigDecimal("0.100"),
                    currency = "OMR",
                    type = TransactionType.EXPENSE,
                    merchant = "Break Point QURU",
                    accountLast4 = "9999",
                    balance = BigDecimal("9999.740"),
                    isFromCard = true
                )
            ),
            ParserTestCase(
                name = "Debit card purchase - merchant with middle ID",
                message = "تم خصم OMR 0.300 من حسابك رقم XXXXXXX9999 بإستخدام بطاقة الخصم المباشر في MASAKEN AL RAHA LLC-833468 KHOOM بتاريخ 2026/04/02 16:15:38. رصيدك الحالي هو 9999.740 OMR.",
                sender = "BankMuscat",
                expected = ExpectedTransaction(
                    amount = BigDecimal("0.300"),
                    currency = "OMR",
                    type = TransactionType.EXPENSE,
                    merchant = "MASAKEN AL RAHA LLC KHOOM",
                    accountLast4 = "9999",
                    balance = BigDecimal("9999.740"),
                    isFromCard = true
                )
            ),
            ParserTestCase(
                name = "Debit card purchase - compact date + trailing terminal id and OM noise",
                message = "تم خصم 50.000 OMR من حسابك رقم 0301XXXXXXXX0013 بإستخدام بطاقة الخصم المباشر في OMANTEL MOBILE APP 24241438 OM بتاريخ 20260811. رصيدك الحالي هو 23.118 OMR.",
                sender = "BankMuscat",
                expected = ExpectedTransaction(
                    amount = BigDecimal("50.000"),
                    currency = "OMR",
                    type = TransactionType.EXPENSE,
                    merchant = "OMANTEL MOBILE APP",
                    accountLast4 = "0013",
                    balance = BigDecimal("23.118"),
                    isFromCard = true
                )
            ),
            ParserTestCase(
                name = "Arabic ATM withdrawal",
                message = "تم سحب 5.000 OMR من حسابك رقم 0440XXXXXXXX0017 من جهاز الصراف الآلي  بتاريخ 2026/08/01 19:42:17. رصيدك الحالي هو 119.498 OMR.",
                sender = "BankMuscat",
                expected = ExpectedTransaction(
                    amount = BigDecimal("5.000"),
                    currency = "OMR",
                    type = TransactionType.EXPENSE,
                    accountLast4 = "0017",
                    balance = BigDecimal("119.498"),
                    isFromCard = false
                )
            ),
            ParserTestCase(
                name = "English card spend - PO BOX noise merchant",
                message = "Card of a/c 0425XXXXXXXX0011 used for OMR 2.310 at 55 COFFEE PO BOX 89 PC on 01/08/2026 19:36:19. Avl Bal OMR 28.310.",
                sender = "BankMuscat",
                expected = ExpectedTransaction(
                    amount = BigDecimal("2.310"),
                    currency = "OMR",
                    type = TransactionType.EXPENSE,
                    merchant = "55 COFFEE",
                    accountLast4 = "0011",
                    balance = BigDecimal("28.310"),
                    isFromCard = true
                )
            ),
            ParserTestCase(
                name = "English card spend - OM0 terminal noise",
                message = "Card of a/c 0316XXXXXXXX0011 used for OMR 11.605 at OMANTEL OM000000000000 on 01/08/2026 19:46:37. Avl Bal OMR 306.877.",
                sender = "BankMuscat",
                expected = ExpectedTransaction(
                    amount = BigDecimal("11.605"),
                    currency = "OMR",
                    type = TransactionType.EXPENSE,
                    merchant = "OMANTEL",
                    accountLast4 = "0011",
                    balance = BigDecimal("306.877"),
                    isFromCard = true
                )
            ),
            ParserTestCase(
                name = "English own-account transfer",
                message = "OMR 5.000 is debited from your A/C 0322XXXXXXXX0027 and credited to your A/C 0322XXXXXXXX0011 on 01/08/2026 19:57:46.",
                sender = "BankMuscat",
                expected = ExpectedTransaction(
                    amount = BigDecimal("5.000"),
                    currency = "OMR",
                    type = TransactionType.TRANSFER,
                    accountLast4 = "0027"
                )
            ),
            ParserTestCase(
                name = "English account debit with New Available Balance",
                message = "OMR 110.000 is debited from your a/c 0304XXXXXXXX0016 on 15/08/2026 13:23:18. New Available Balance is OMR 33.839.",
                sender = "BankMuscat",
                expected = ExpectedTransaction(
                    amount = BigDecimal("110.000"),
                    currency = "OMR",
                    type = TransactionType.EXPENSE,
                    accountLast4 = "0016",
                    balance = BigDecimal("33.839")
                )
            ),
            ParserTestCase(
                name = "English ATM withdrawal",
                message = "OMR 15.000 withdrawn from your a/c 0301XXXXXXXX0012 through  ATM on 07/08/2026 11:12:30. New Available Balance is OMR 484.307.",
                sender = "BankMuscat",
                expected = ExpectedTransaction(
                    amount = BigDecimal("15.000"),
                    currency = "OMR",
                    type = TransactionType.EXPENSE,
                    accountLast4 = "0012",
                    balance = BigDecimal("484.307")
                )
            ),
            ParserTestCase(
                name = "English credit card spend with available limit",
                message = "Card 420460******0326 used for OMR 1.850 at GROCERY SHOP  on 07/08/2026 09:35:30. Available limit OMR 359.885.",
                sender = "BankMuscat",
                expected = ExpectedTransaction(
                    amount = BigDecimal("1.850"),
                    currency = "OMR",
                    type = TransactionType.EXPENSE,
                    merchant = "GROCERY SHOP",
                    accountLast4 = "0326",
                    isFromCard = true
                )
            ),
            ParserTestCase(
                name = "English card payment towards Bank Muscat credit card",
                message = "Dear Customer, Your a/c no 0383XXXXXXXX0039 has been debited for an amt of OMR 143.520 as payment towards bank muscat Credit Card No. 4132XXXXXXXX0580",
                sender = "BankMuscat",
                expected = ExpectedTransaction(
                    amount = BigDecimal("143.520"),
                    currency = "OMR",
                    type = TransactionType.TRANSFER,
                    accountLast4 = "0039"
                )
            ),
            ParserTestCase(
                name = "mBanking SAVING account mobile payment",
                message = "Your SAVING A/C 70102#######01 has been debited OMR 25.000 on 01/08/2026 by Mobile Payment. Your available balance is OMR 116.921",
                sender = "BankMuscat",
                expected = ExpectedTransaction(
                    amount = BigDecimal("25.000"),
                    currency = "OMR",
                    type = TransactionType.EXPENSE,
                    accountLast4 = "0201",
                    balance = BigDecimal("116.921")
                )
            ),
            ParserTestCase(
                name = "mBanking payment for ThawaniPay with backslash merchant field",
                message = "Your SAVING A/C 70702#######01 has been debited OMR 5.000 on 03/08/2026 20:21:25 for ThawaniPay APP\\\\\\       133   OMN. Your available balance is OMR 2.771",
                sender = "BankMuscat",
                expected = ExpectedTransaction(
                    amount = BigDecimal("5.000"),
                    currency = "OMR",
                    type = TransactionType.EXPENSE,
                    merchant = "ThawaniPay APP",
                    accountLast4 = "0201",
                    balance = BigDecimal("2.771")
                )
            ),
            ParserTestCase(
                name = "Mobile payment sent to masked beneficiary",
                message = "Dear Customer, You have sent OMR 1.400 to AIMA########################MISI from your a/c 0304XXXXXXXX0018 on 13/08/2026 09:04:13 using Mobile Payment services. Txn Id BMCT014986288247. Avl Bal OMR 198.577.",
                sender = "BankMuscat",
                expected = ExpectedTransaction(
                    amount = BigDecimal("1.400"),
                    currency = "OMR",
                    type = TransactionType.TRANSFER,
                    merchant = "AIMA########################MISI",
                    accountLast4 = "0018",
                    reference = "BMCT014986288247",
                    balance = BigDecimal("198.577")
                )
            ),
            ParserTestCase(
                name = "Arabic debit - PO BOX + MCT noise merchant",
                message = "تم خصم 2.537 OMR من حسابك رقم 0430XXXXXXXX0017 بإستخدام بطاقة الخصم المباشر في 164484-SPAR PO BOX 19 PC 100 MCT بتاريخ 2023/07/18 13:25:04. رصيدك الحالي هو 3834.374 OMR.",
                sender = "BankMuscat",
                expected = ExpectedTransaction(
                    amount = BigDecimal("2.537"),
                    currency = "OMR",
                    type = TransactionType.EXPENSE,
                    merchant = "SPAR",
                    accountLast4 = "0017",
                    balance = BigDecimal("3834.374"),
                    isFromCard = true
                )
            ),
            ParserTestCase(
                name = "Credit naming the payer",
                message = "OMR 300.000 has been credited to your a/c 0430XXXXXXXX0017 by KHALID SULAIMAN KHALID AL HART on 03/02/2024 18:54:48. New Available Balance is OMR 309.689.",
                sender = "BankMuscat",
                expected = ExpectedTransaction(
                    amount = BigDecimal("300.000"),
                    currency = "OMR",
                    type = TransactionType.INCOME,
                    merchant = "KHALID SULAIMAN KHALID AL HART",
                    accountLast4 = "0017",
                    balance = BigDecimal("309.689")
                )
            ),
            ParserTestCase(
                name = "ATM withdrawal with terminal id and location",
                message = "OMR 20.000 withdrawn from your a/c 0430XXXXXXXX0017 through ATM-11751203 Safalat  ATM on 13/02/2024 12:18:01. New Available Balance is OMR 258.989.",
                sender = "BankMuscat",
                expected = ExpectedTransaction(
                    amount = BigDecimal("20.000"),
                    currency = "OMR",
                    type = TransactionType.EXPENSE,
                    accountLast4 = "0017",
                    balance = BigDecimal("258.989")
                )
            ),
            ParserTestCase(
                name = "Meethaq-issued card BIN is not claimed by Bank Muscat",
                message = "Card 447627******9662 used for OMR 1.000 at SOME SHOP on 07/08/2026 09:35:30. Available limit OMR 100.000.",
                sender = "BankMuscat",
                shouldParse = false
            )
        )

        val handleCases = listOf(
            "BankMuscat" to true,
            "BKMUSCAT" to true,
            "bank muscat" to true,
            "بنك مسقط" to true,
            "HSBC" to false,
            "" to false
        )

        return ParserTestUtils.runTestSuite(parser, testCases, handleCases, "Bank Muscat Parser Tests")
    }

    @TestFactory
    fun `factory resolves bank muscat`(): List<DynamicTest> {
        val cases = listOf(
            SimpleTestCase(
                bankName = "Bank Muscat",
                sender = "BankMuscat",
                currency = "OMR",
                message = "Card of a/c 0359XXXXXXXX0011 used for OMR 13.560 at Majid Al Futtaim Cinem on 01 AUG 2026 20:05. Avl Bal OMR 40.277.",
                expected = ExpectedTransaction(
                    amount = BigDecimal("13.560"),
                    currency = "OMR",
                    type = TransactionType.EXPENSE,
                    merchant = "Majid Al Futtaim Cinem",
                    accountLast4 = "0011",
                    balance = BigDecimal("40.277"),
                    isFromCard = true
                )
            )
        )

        return ParserTestUtils.runFactoryTestSuite(cases, "Bank Muscat factory tests")
    }
}
