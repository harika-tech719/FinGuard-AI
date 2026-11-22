package com.example

import com.example.data.model.FinGuardFormatters
import com.example.data.model.RiskLevel
import com.example.data.model.ScanType
import com.example.fraud.OfflineHeuristicAnalyzer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun `sms analyzer detects urgency, fake rewards, otp requests, impersonation, and embedded links`() {
        val kycScam = OfflineHeuristicAnalyzer.analyze(
            ScanType.SMS,
            "URGENT: Your bank account will be BLOCKED tonight due to incomplete KYC. Update at http://sbi-kyc-verify.xyz/login and share OTP."
        )
        assertTrue("Expected score >= 65 but got ${kycScam.riskScore}", kycScam.riskScore >= 65)
        assertTrue(kycScam.riskLevel == RiskLevel.HIGH || kycScam.riskLevel == RiskLevel.CRITICAL)
        assertTrue(kycScam.indicators.isNotEmpty())
        assertTrue(kycScam.recommendation.isNotBlank())

        val lotteryScam = OfflineHeuristicAnalyzer.analyze(
            ScanType.SMS,
            "Congratulations! You won ₹50,000 bonus cashback jackpot! Claim now at https://bit.ly/win-cash-99"
        )
        assertTrue("Lottery scam should score >= 50, got ${lotteryScam.riskScore}", lotteryScam.riskScore >= 50)

        val legitimateAlert = OfflineHeuristicAnalyzer.analyze(
            ScanType.SMS,
            "Info: Your monthly e-statement for Demo Account XX4812 is ready. View inside your official banking app. Never share OTP or PIN with anyone."
        )
        assertEquals(RiskLevel.LOW, legitimateAlert.riskLevel)
        assertTrue(legitimateAlert.riskScore < 25)
    }

    @Test
    fun `url scanner detects typosquatting, shorteners, http, raw IP, and marks official portals low risk`() {
        val ipPhish = OfflineHeuristicAnalyzer.analyze(
            ScanType.URL,
            "http://192.168.45.12/paytm-cashback-claim/verify-kyc.php?token=free5000"
        )
        assertEquals(RiskLevel.CRITICAL, ipPhish.riskLevel)
        assertTrue(ipPhish.indicators.any { it.contains("IPv4", ignoreCase = true) })

        val typosquatShortlink = OfflineHeuristicAnalyzer.analyze(
            ScanType.URL,
            "https://bit.ly/sb1-onl1ne-kyc-update"
        )
        assertTrue("Lookalike shortlink should be High/Critical, got ${typosquatShortlink.riskScore}", typosquatShortlink.riskScore >= 55)
        assertTrue(typosquatShortlink.indicators.any { it.contains("lookalike", ignoreCase = true) || it.contains("shortener", ignoreCase = true) })

        val officialUrl = OfflineHeuristicAnalyzer.analyze(
            ScanType.URL,
            "https://www.rbi.org.in/commonman/English/Scripts/Home.aspx"
        )
        assertEquals(RiskLevel.LOW, officialUrl.riskLevel)
        assertTrue(officialUrl.riskScore < 25)
    }

    @Test
    fun `upi analyzer detects collect-to-receive pin traps, urgent collect requests, and allows routine peer payments`() {
        val scamCollect = OfflineHeuristicAnalyzer.analyze(
            ScanType.UPI,
            "Collect request from cashback.lottery@fakeupi for ₹4,999. Enter your UPI PIN within 3 mins to RECEIVE prize money!"
        )
        assertEquals(RiskLevel.CRITICAL, scamCollect.riskLevel)
        assertTrue(scamCollect.riskScore >= 80)

        val urgentCollect = OfflineHeuristicAnalyzer.analyze(
            ScanType.UPI,
            "URGENT collect request from unknown@okdemo for ₹10,000. Approve immediately."
        )
        assertTrue("Urgent high-value collect should be at least MEDIUM risk, got ${urgentCollect.riskScore}", urgentCollect.riskScore >= 25)

        val routineTransfer = OfflineHeuristicAnalyzer.analyze(
            ScanType.UPI,
            "Transfer ₹240 to priya.nair@okdemo for Friday coffee and snacks"
        )
        assertEquals(RiskLevel.LOW, routineTransfer.riskLevel)
        assertTrue(routineTransfer.riskScore < 25)
    }

    @Test
    fun `privacy sanitizer redacts card-like numbers and PINs`() {
        val (sanitized, notice) = OfflineHeuristicAnalyzer.sanitizeSensitiveData(
            "Check card 4111 2222 3333 4444 with PIN: 4829"
        )
        assertFalse(sanitized.contains("4111"))
        assertFalse(sanitized.contains("4829"))
        assertTrue(notice != null)
    }

    @Test
    fun `currency parser rejects zero, sub-paisa, scientific notation, overflow, and negative amounts`() {
        assertNull(FinGuardFormatters.parsePaiseInput("0"))
        assertNull(FinGuardFormatters.parsePaiseInput("0.00"))
        assertNull(FinGuardFormatters.parsePaiseInput("0.001"))
        assertNull(FinGuardFormatters.parsePaiseInput("-500"))
        assertNull(FinGuardFormatters.parsePaiseInput("1e5"))
        assertNull(FinGuardFormatters.parsePaiseInput("100.123"))
        assertNull(FinGuardFormatters.parsePaiseInput("9999999999999999"))
        assertNull(FinGuardFormatters.parsePaiseInput("abc"))
        assertEquals(125_050L, FinGuardFormatters.parsePaiseInput("1,250.50"))
        assertEquals(50L, FinGuardFormatters.parsePaiseInput("0.50"))
    }

    @Test
    fun `demo catalog covers all fraud types with risky and safe examples plus valid education quiz`() {
        ScanType.entries.forEach { type ->
            val samples = com.example.data.model.DemoCatalog.fraudSamples.filter { it.scanType == type }
            assertTrue("Missing risky sample for $type", samples.any { it.isRisky })
            assertTrue("Missing safe sample for $type", samples.any { !it.isRisky })
        }
        com.example.data.model.EducationCategory.entries.forEach { category ->
            assertTrue(
                "Missing lesson for $category",
                com.example.data.model.DemoCatalog.educationLessons.any { it.category == category }
            )
        }
        com.example.data.model.DemoCatalog.quizQuestions.forEach { q ->
            assertTrue(q.correctIndex in q.options.indices)
            assertTrue(q.explanation.isNotBlank())
        }
    }
}
