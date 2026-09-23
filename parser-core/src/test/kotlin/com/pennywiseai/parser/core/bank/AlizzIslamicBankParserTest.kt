package com.pennywiseai.parser.core.bank

import com.pennywiseai.parser.core.TransactionType
import com.pennywiseai.parser.core.test.ExpectedTransaction
import com.pennywiseai.parser.core.test.ParserTestCase
import com.pennywiseai.parser.core.test.ParserTestUtils
import com.pennywiseai.parser.core.test.SimpleTestCase
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import java.math.BigDecimal

class AlizzIslamicBankParserTest {

    @TestFactory
    fun `alizz parser handles confirmed templates`(): List<DynamicTest> {
        val parser = AlizzIslamicBankParser()

        val testCases = listOf(
            ParserTestCase(
                name = "Debit with merchant (space-padded terminal id)",
                message = "OMR 67.190 has been debited from your account 0010********1001 at Khedmah          112   on 2026-08-07 07:46:58. Your available balance is OMR 295.863",
                sender = "Alizz Bank",
                expected = ExpectedTransaction(
                    amount = BigDecimal("67.190"),
                    currency = "OMR",
                    type = TransactionType.EXPENSE,
                    merchant = "Khedmah",
                    accountLast4 = "1001",
                    balance = BigDecimal("295.863")
                )
            ),
            ParserTestCase(
                name = "Debit without merchant",
                message = "OMR 140 has been debited from your account 0010********4005 on 2026-08-07 09:32:28.  Your available balance is OMR 701.504",
                sender = "Alizz Bank",
                expected = ExpectedTransaction(
                    amount = BigDecimal("140"),
                    currency = "OMR",
                    type = TransactionType.EXPENSE,
                    accountLast4 = "4005",
                    balance = BigDecimal("701.504")
                )
            ),
            ParserTestCase(
                name = "Debit without merchant, no period before balance",
                message = "OMR 28.265 has been debited from your account 0070********4001 on 2026-08-20 15:12:21 Your available balance is OMR 0",
                sender = "Alizz Bank",
                expected = ExpectedTransaction(
                    amount = BigDecimal("28.265"),
                    currency = "OMR",
                    type = TransactionType.EXPENSE,
                    accountLast4 = "4001",
                    balance = BigDecimal("0")
                )
            ),
            ParserTestCase(
                name = "Card-not-present debit - merchant carried in the account-number field",
                message = "OMR 38.060 has been debited from your account number  talabat.com            muscat        OMN on 01-AUG , 20:32:09.Available Balance is  OMR 95.963.",
                sender = "Alizz Bank",
                expected = ExpectedTransaction(
                    amount = BigDecimal("38.060"),
                    currency = "OMR",
                    type = TransactionType.EXPENSE,
                    merchant = "talabat.com",
                    balance = BigDecimal("95.963"),
                    expectNullAccountLast4 = true
                )
            ),
            ParserTestCase(
                name = "Debit without 'from' - mask directly after debited",
                message = "OMR 30 has been debited 1600********4001 on 2025-06-28 12:33:41. Your available balance is OMR 967.881",
                sender = "Alizz Bank",
                expected = ExpectedTransaction(
                    amount = BigDecimal("30"),
                    currency = "OMR",
                    type = TransactionType.EXPENSE,
                    accountLast4 = "4001",
                    balance = BigDecimal("967.881")
                )
            ),
            ParserTestCase(
                name = "Salary credit - merchant labelled Salary",
                message = "Your salary of OMR 1431.091 has been credited into your account number 1600********4001 on 2026-08-26 10:17:12.  Your available balance is OMR 2960.514",
                sender = "Alizz Bank",
                expected = ExpectedTransaction(
                    amount = BigDecimal("1431.091"),
                    currency = "OMR",
                    type = TransactionType.INCOME,
                    merchant = "Salary",
                    accountLast4 = "4001",
                    balance = BigDecimal("2960.514")
                )
            ),
            ParserTestCase(
                name = "Reported credit form (bank's balans typo)",
                message = "OMR 500 has been credited to your account 0010********1001 on 2026-08-19 12:36:04 Your available balans is OMR 456.468",
                sender = "Alizz Bank",
                expected = ExpectedTransaction(
                    amount = BigDecimal("500"),
                    currency = "OMR",
                    type = TransactionType.INCOME,
                    accountLast4 = "1001",
                    balance = BigDecimal("456.468")
                )
            ),
            ParserTestCase(
                name = "Monthly installment debit (customer name not extracted)",
                message = "عميلنا العزيز، تم خصم مبلغ القسط الشهري وقدره OMR 1.165 ريال عماني من حسابك رقم 0302######001.",
                sender = "Alizz Bank",
                expected = ExpectedTransaction(
                    amount = BigDecimal("1.165"),
                    currency = "OMR",
                    type = TransactionType.EXPENSE,
                    accountLast4 = "2001"
                )
            )
        )

        val handleCases = listOf(
            "Alizz Bank" to true,
            "ALIZZ BANK" to true,
            "ALIZZBANK" to true,
            "AlizzBank" to true,
            "Alizz" to true,
            "بنك العز" to true,
            "HSBC" to false,
            "" to false
        )

        return ParserTestUtils.runTestSuite(parser, testCases, handleCases, "Alizz Islamic Bank Parser Tests")
    }

    @TestFactory
    fun `factory resolves alizz`(): List<DynamicTest> {
        val cases = listOf(
            SimpleTestCase(
                bankName = "Alizz Islamic Bank",
                sender = "Alizz Bank",
                currency = "OMR",
                message = "OMR 12.100 has been debited from your account 0800********4001 at SHELL OMAN - BIDBID SS on 2026-08-07 09:30:02. Your available balance is OMR 336.691",
                expected = ExpectedTransaction(
                    amount = BigDecimal("12.100"),
                    currency = "OMR",
                    type = TransactionType.EXPENSE,
                    merchant = "SHELL OMAN - BIDBID SS",
                    accountLast4 = "4001",
                    balance = BigDecimal("336.691")
                )
            )
        )

        return ParserTestUtils.runFactoryTestSuite(cases, "Alizz Islamic Bank factory tests")
    }
}
