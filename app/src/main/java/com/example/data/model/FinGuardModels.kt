package com.example.data.model

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

const val INITIAL_DEMO_BALANCE_PAISE = 1_000_000L // ₹10,000.00
const val MAX_SINGLE_TOPUP_PAISE = 10_000_000L   // ₹1,00,000.00 max single demo top-up

enum class ScanType(val label: String, val shortLabel: String) {
    SMS("SMS Scam Check", "SMS"),
    URL("Suspicious URL Check", "URL"),
    UPI("UPI Request Check", "UPI")
}

enum class RiskLevel(val label: String) {
    LOW("Low Risk"),
    MEDIUM("Medium Risk"),
    HIGH("High Risk"),
    CRITICAL("Critical");

    companion object {
        fun fromScore(score: Int): RiskLevel = when {
            score < 25 -> LOW
            score < 55 -> MEDIUM
            score < 80 -> HIGH
            else -> CRITICAL
        }

        fun fromRaw(raw: String, score: Int): RiskLevel {
            return when (raw.trim().uppercase(Locale.US)) {
                "LOW", "SAFE" -> LOW
                "MEDIUM", "CAUTION" -> MEDIUM
                "HIGH", "HIGH_RISK" -> HIGH
                "CRITICAL" -> CRITICAL
                else -> fromScore(score)
            }
        }
    }
}

enum class AnalysisEngine(val label: String) {
    GEMINI_AI("Gemini AI Shield"),
    OFFLINE_HEURISTIC("Offline Heuristic Shield")
}

enum class TransactionType(val label: String) {
    CREDIT("Demo Top-Up"),
    DEBIT("Demo Transfer"),
    UPI_SIM_SUCCESS("Simulated UPI"),
    UPI_SIM_FAILED("Simulated UPI Failed")
}

enum class SimulationOutcome(val label: String, val description: String) {
    AUTO("Smart Auto Check", "Evaluates balance, VPA & fraud rules"),
    FORCE_SUCCESS("Simulate Success", "Forces an approved simulated receipt"),
    FORCE_NETWORK_FAILURE("Simulate Network Timeout", "Tests handling of dropped UPI connection"),
    FORCE_BANK_DECLINE("Simulate Bank Decline", "Tests simulated issuer bank rejection")
}

data class DemoProfile(
    val id: String,
    val displayName: String,
    val vpa: String,
    val bankLabel: String,
    val initials: String,
    val isSenderOption: Boolean = false,
    val isRiskySample: Boolean = false,
    val roleTag: String
)

data class FraudAnalysisResult(
    val scanType: ScanType,
    val inputContent: String,
    val riskScore: Int,
    val riskLevel: RiskLevel,
    val headline: String,
    val explanation: String,
    val indicators: List<String>,
    val recommendation: String,
    val engineUsed: AnalysisEngine,
    val privacyNotice: String? = null,
    val fallbackNotice: String? = null
)

data class FraudSamplePrompt(
    val title: String,
    val scanType: ScanType,
    val payload: String,
    val isRisky: Boolean
)

enum class EducationCategory(val title: String) {
    OTP("OTP Safety"),
    UPI("UPI Fraud"),
    PHISHING("Phishing & Links"),
    HYGIENE("Digital Hygiene")
}

data class EducationLesson(
    val id: String,
    val category: EducationCategory,
    val title: String,
    val summary: String,
    val redFlagExample: String,
    val safePractice: String,
    val keyTakeaway: String
)

data class SecurityQuizQuestion(
    val id: String,
    val category: EducationCategory,
    val question: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String
)

object DemoCatalog {
    val senderProfiles = listOf(
        DemoProfile(
            id = "sender_aarav",
            displayName = "Aarav Sharma (You)",
            vpa = "aarav.demo@finguard",
            bankLabel = "FinGuard Sandbox Vault",
            initials = "AS",
            isSenderOption = true,
            roleTag = "Primary Demo Wallet"
        ),
        DemoProfile(
            id = "sender_meera",
            displayName = "Meera Kapoor (Demo)",
            vpa = "meera.sim@okdemo",
            bankLabel = "FinGuard Joint Sandbox",
            initials = "MK",
            isSenderOption = true,
            roleTag = "Secondary Fictional Profile"
        )
    )

    val receiverProfiles = listOf(
        DemoProfile(
            id = "recv_priya",
            displayName = "Priya Nair",
            vpa = "priya.nair@okdemo",
            bankLabel = "Demo Metro Bank",
            initials = "PN",
            roleTag = "Verified Demo Contact"
        ),
        DemoProfile(
            id = "recv_rohan",
            displayName = "Rohan Verma",
            vpa = "rohan.v@finguard",
            bankLabel = "Horizon Sandbox Bank",
            initials = "RV",
            roleTag = "Verified Demo Contact"
        ),
        DemoProfile(
            id = "recv_mart",
            displayName = "Metro Fresh Mart",
            vpa = "metro.mart@demopay",
            bankLabel = "Merchant Sandbox",
            initials = "MF",
            roleTag = "Demo Merchant"
        ),
        DemoProfile(
            id = "recv_utilities",
            displayName = "QuickFix Utilities",
            vpa = "quickfix.bill@simupi",
            bankLabel = "Utility Sandbox",
            initials = "QF",
            roleTag = "Demo Biller"
        ),
        DemoProfile(
            id = "recv_suspicious",
            displayName = "KYC Reward Desk (Test)",
            vpa = "reward.claim.kyc@fakebank",
            bankLabel = "High-Risk Test Profile",
            initials = "KR",
            isRiskySample = true,
            roleTag = "Simulated Scam VPA"
        )
    )

    val fraudSamples = listOf(
        // SMS Examples (Suspicious + Legitimate)
        FraudSamplePrompt(
            title = "KYC Expiry Threat",
            scanType = ScanType.SMS,
            payload = "URGENT: Your bank account will be BLOCKED tonight due to incomplete KYC. Update immediately at http://sbi-kyc-verify.xyz/login or call +91-9800112233. Share OTP to confirm.",
            isRisky = true
        ),
        FraudSamplePrompt(
            title = "Fake Lottery Prize",
            scanType = ScanType.SMS,
            payload = "Congratulations! You won ₹50,000 bonus cashback jackpot! Claim now at https://bit.ly/win-cash-99 and enter UPI PIN to receive funds.",
            isRisky = true
        ),
        FraudSamplePrompt(
            title = "Legitimate Statement",
            scanType = ScanType.SMS,
            payload = "Info: Your monthly e-statement for Demo Account XX4812 is ready. View inside your official banking app. Never share OTP or PIN with anyone.",
            isRisky = false
        ),
        FraudSamplePrompt(
            title = "Legitimate Card Alert",
            scanType = ScanType.SMS,
            payload = "Demo Alert: ₹450.00 spent at Metro Fresh Mart on 20 Nov. If not authorized by you, freeze card inside your official banking app.",
            isRisky = false
        ),
        // URL Examples (Suspicious + Legitimate)
        FraudSamplePrompt(
            title = "IP Phishing Link",
            scanType = ScanType.URL,
            payload = "http://192.168.45.12/paytm-cashback-claim/verify-kyc.php?token=free5000",
            isRisky = true
        ),
        FraudSamplePrompt(
            title = "Lookalike Shortlink",
            scanType = ScanType.URL,
            payload = "https://bit.ly/sb1-onl1ne-kyc-update",
            isRisky = true
        ),
        FraudSamplePrompt(
            title = "Fake Bank APK",
            scanType = ScanType.URL,
            payload = "http://hdfc-secure-login.top/kyc-update.apk",
            isRisky = true
        ),
        FraudSamplePrompt(
            title = "Official RBI Portal",
            scanType = ScanType.URL,
            payload = "https://www.rbi.org.in/commonman/English/Scripts/Home.aspx",
            isRisky = false
        ),
        FraudSamplePrompt(
            title = "Official NPCI Site",
            scanType = ScanType.URL,
            payload = "https://www.npci.org.in/what-we-do/upi/product-overview",
            isRisky = false
        ),
        // UPI Request Examples (Suspicious + Legitimate)
        FraudSamplePrompt(
            title = "Fake Cashback Collect",
            scanType = ScanType.UPI,
            payload = "Collect request from cashback.lottery@fakeupi for ₹4,999. Message: Enter your UPI PIN within 3 mins to RECEIVE ₹4,999 lottery prize money!",
            isRisky = true
        ),
        FraudSamplePrompt(
            title = "Buyer Refund Trap",
            scanType = ScanType.UPI,
            payload = "Collect request from buyer.refund.desk@okdemo for ₹8,500. Approve immediately and enter PIN to receive marketplace advance.",
            isRisky = true
        ),
        FraudSamplePrompt(
            title = "Friend Coffee Split",
            scanType = ScanType.UPI,
            payload = "Transfer ₹240 to priya.nair@okdemo for Friday coffee and snacks",
            isRisky = false
        ),
        FraudSamplePrompt(
            title = "Utility Bill Payment",
            scanType = ScanType.UPI,
            payload = "Pay ₹1,150 to quickfix.bill@simupi for monthly broadband bill",
            isRisky = false
        )
    )

    val educationLessons = listOf(
        EducationLesson(
            id = "otp_1",
            category = EducationCategory.OTP,
            title = "Why OTPs Are Strictly One-Way",
            summary = "An OTP authorizes a debit, login, or SIM/device binding. No bank, merchant, or government agency ever needs your OTP to send you a refund or verify KYC.",
            redFlagExample = "\"Tell me the 6-digit verification code on your phone so I can reverse the double charge.\"",
            safePractice = "Never read, forward, or type an OTP for an incoming refund, prize, or courier delivery.",
            keyTakeaway = "Receiving money NEVER requires sharing an OTP."
        ),
        EducationLesson(
            id = "otp_2",
            category = EducationCategory.OTP,
            title = "Screen-Sharing & Remote Access Traps",
            summary = "Scammers pose as support agents and ask you to install remote-control apps so they can silently view incoming OTP notifications.",
            redFlagExample = "\"Download QuickSupport and grant accessibility access to unblock your card.\"",
            safePractice = "Deny screen-sharing requests during any financial or support call and uninstall unknown support tools.",
            keyTakeaway = "Screen sharing gives attackers live eyes on every OTP."
        ),
        EducationLesson(
            id = "upi_1",
            category = EducationCategory.UPI,
            title = "The Golden Rule of UPI PIN",
            summary = "A UPI PIN is only ever required when money LEAVES your account—whether scanning a QR code or approving a collect request.",
            redFlagExample = "\"Approve this ₹5,000 collect request and enter your PIN to claim cashback.\"",
            safePractice = "Decline any collect request you did not expect, especially from buyers on classifieds marketplaces.",
            keyTakeaway = "PIN = Pay Out. Receiving money requires ZERO PIN entry."
        ),
        EducationLesson(
            id = "upi_2",
            category = EducationCategory.UPI,
            title = "Inspecting VPA Handles & Lookalikes",
            summary = "Fraudsters create VPAs containing words like 'refund', 'support', or 'kyc' to impersonate official brands.",
            redFlagExample = "VPA: helpdesk.refund@okicici-verify requesting urgent settlement.",
            safePractice = "Verify the exact recipient name displayed by the payment rail before confirming any transfer.",
            keyTakeaway = "Always match the verified payee name, not the display message."
        ),
        EducationLesson(
            id = "phish_1",
            category = EducationCategory.PHISHING,
            title = "Smishing & Artificial Urgency",
            summary = "Scam SMS messages manufacture panic—power disconnection, tax penalties, or account suspension within 2 hours—to stop you from verifying.",
            redFlagExample = "\"Electricity will be cut at 9:30 PM tonight. Click bit.ly/pay-now-99 immediately.\"",
            safePractice = "Open your official utility or bank app directly instead of tapping links inside SMS messages.",
            keyTakeaway = "Panic + shortened URL = classic smishing signature."
        ),
        EducationLesson(
            id = "phish_2",
            category = EducationCategory.PHISHING,
            title = "Domain Typosquatting & APK Side-Loading",
            summary = "Phishing sites swap characters (e.g., 'paytrn' or '.xyz' domains) or push malicious '.apk' installers disguised as bank KYC updates.",
            redFlagExample = "http://hdfc-kyc-update.top/app-update.apk",
            safePractice = "Check the exact root domain, require HTTPS, and never install banking updates from browser APK downloads.",
            keyTakeaway = "Banks distribute updates exclusively via official app stores."
        ),
        EducationLesson(
            id = "hyg_1",
            category = EducationCategory.HYGIENE,
            title = "Layered Financial Hygiene",
            summary = "Separating daily UPI wallet limits from primary savings accounts dramatically limits potential loss even if a credential is compromised.",
            redFlagExample = "Keeping unlimited daily transfer thresholds active on your main salary account.",
            safePractice = "Enable per-transaction alerts, set tight daily UPI/card limits, and use dedicated virtual cards for online trials.",
            keyTakeaway = "Low daily limits act as an automatic circuit breaker."
        )
    )

    val quizQuestions = listOf(
        SecurityQuizQuestion(
            id = "q1",
            category = EducationCategory.UPI,
            question = "A buyer on a marketplace sends a UPI Collect Request saying 'Enter your PIN to receive ₹8,000 advance.' What happens if you enter your PIN?",
            options = listOf(
                "You receive ₹8,000 into your bank account",
                "₹8,000 is debited FROM your account to the buyer",
                "The bank verifies your identity with zero money movement"
            ),
            correctIndex = 1,
            explanation = "Entering a UPI PIN on a collect request always authorizes a debit FROM your account. Receiving money never requires a PIN."
        ),
        SecurityQuizQuestion(
            id = "q2",
            category = EducationCategory.PHISHING,
            question = "You receive an SMS: 'Your bank KYC expires in 1 hour. Update at http://bank-verify-kyc.xyz'. What is the safest action?",
            options = listOf(
                "Open the link quickly to avoid account freeze",
                "Reply 'STOP' with your customer ID",
                "Ignore the link and check KYC status directly inside your official bank app"
            ),
            correctIndex = 2,
            explanation = "Never tap urgent SMS links or reply with personal identifiers. Always verify account status directly in the official app or branch."
        ),
        SecurityQuizQuestion(
            id = "q3",
            category = EducationCategory.OTP,
            question = "Someone claiming to be from your bank's fraud team asks for the 6-digit OTP just sent to your phone to 'block a suspicious transaction.' Should you share it?",
            options = listOf(
                "No — banks never ask for OTPs, PINs, or passwords",
                "Yes, if they already know the last 4 digits of your card",
                "Yes, if the call comes from a toll-free 1800 number"
            ),
            correctIndex = 0,
            explanation = "Scammers frequently spoof caller IDs and already have leaked partial card digits. Sharing the OTP completes the unauthorized transaction."
        )
    )
}

object FinGuardFormatters {
    private val inrFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN")).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }

    private val dateTimeFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US)
    private val shortTimeFormat = SimpleDateFormat("dd MMM • hh:mm a", Locale.US)
    private val shortDateFormat = SimpleDateFormat("dd MMM", Locale.US)
    private val compactDateFormat = SimpleDateFormat("yyyyMMdd", Locale.US)

    fun formatPaise(paise: Long): String = inrFormat.format(paise / 100.0)

    fun formatTimestamp(epochMillis: Long): String = dateTimeFormat.format(Date(epochMillis))

    fun formatShortTimestamp(epochMillis: Long): String = shortTimeFormat.format(Date(epochMillis))

    fun formatShortDate(epochMillis: Long): String = shortDateFormat.format(Date(epochMillis))

    fun generateLocalTxRef(prefix: String): String {
        val datePart = compactDateFormat.format(Date())
        val suffix = Random.nextInt(100_000, 999_999)
        return "$prefix-$datePart-$suffix"
    }

    fun parsePaiseInput(raw: String): Long? {
        val paise = parseNonNegativePaiseInput(raw) ?: return null
        return if (paise <= 0L) null else paise
    }

    fun parseNonNegativePaiseInput(raw: String): Long? {
        val cleaned = raw.trim().replace(",", "")
        if (cleaned.isEmpty()) return null
        if (!Regex("""^\d+(?:\.\d{1,2})?$""").matches(cleaned)) return null
        return try {
            val paise = java.math.BigDecimal(cleaned).movePointRight(2).longValueExact()
            if (paise < 0L || paise > 1_000_000_000L) null else paise
        } catch (_: Exception) {
            null
        }
    }
}
