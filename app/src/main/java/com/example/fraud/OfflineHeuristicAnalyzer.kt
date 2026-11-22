package com.example.fraud

import com.example.data.model.AnalysisEngine
import com.example.data.model.FraudAnalysisResult
import com.example.data.model.RiskLevel
import com.example.data.model.ScanType
import java.net.URI
import java.util.Locale

object OfflineHeuristicAnalyzer {

    private val cardPattern = Regex("""\b(?:\d[ -]*?){15,16}\b""")
    private val cvvPattern = Regex("""\b(?:cvv|pin)\s*[:=\-]?\s*\d{3,6}\b""", RegexOption.IGNORE_CASE)

    fun sanitizeSensitiveData(input: String): Pair<String, String?> {
        var sanitized = input
        var warning: String? = null
        if (cardPattern.containsMatchIn(sanitized)) {
            sanitized = cardPattern.replace(sanitized, "[REDACTED-NUMBER]")
            warning = "FinGuard detected a long card-like number and redacted it before scanning. Never enter real card numbers."
        }
        if (cvvPattern.containsMatchIn(sanitized)) {
            sanitized = cvvPattern.replace(sanitized, "[REDACTED-SECRET]")
            warning = "FinGuard detected and redacted a PIN/CVV sequence. Never share real PINs, OTPs, or CVVs."
        }
        return sanitized to warning
    }

    fun analyze(scanType: ScanType, rawInput: String): FraudAnalysisResult {
        val (cleanInput, privacyWarning) = sanitizeSensitiveData(rawInput.trim())
        val lower = cleanInput.lowercase(Locale.US)
        val indicators = mutableListOf<String>()
        var score = 4

        when (scanType) {
            ScanType.SMS -> {
                score += scoreUrgency(lower, indicators)
                score += scoreCredentialHarvest(lower, indicators)
                score += scoreLotteryAndRewards(lower, indicators)
                score += scoreImpersonation(lower, indicators)
                score += scoreEmbeddedLinks(cleanInput, indicators)
                if (lower.contains("never share otp") && !lower.contains("share otp to")) {
                    score = (score - 12).coerceAtLeast(2)
                    indicators.add("(-12 pts) Includes standard anti-fraud reminder ('never share OTP').")
                }
            }

            ScanType.URL -> {
                score += inspectUrl(cleanInput, indicators)
            }

            ScanType.UPI -> {
                score += inspectUpiRequest(cleanInput, lower, indicators)
            }
        }

        val clampedScore = score.coerceIn(0, 100)
        val riskLevel = RiskLevel.fromScore(clampedScore)

        if (indicators.isEmpty()) {
            indicators.add("(+4 pts baseline) No known phishing, urgency, or credential-harvesting rules matched.")
        }

        val headline = when (riskLevel) {
            RiskLevel.LOW -> "Low Risk Indicator — Routine Signals"
            RiskLevel.MEDIUM -> "Medium Risk — Verify Before Acting"
            RiskLevel.HIGH -> "High Risk — Strong Scam Signals"
            RiskLevel.CRITICAL -> "Critical Risk — Likely Financial Scam"
        }

        val explanation = when (riskLevel) {
            RiskLevel.LOW -> "Offline heuristic rules found no coercive urgency, deceptive URLs, or credential-harvesting cues. Always treat this score as an advisory indicator and confirm sender identity independently."
            RiskLevel.MEDIUM -> "Some cautionary signals (such as unverified links, urgency phrasing, or high-value/unfamiliar VPAs) were detected. Verify via your official bank or payment app before proceeding."
            RiskLevel.HIGH -> "Multiple high-risk scam patterns matched—including deceptive domains, artificial urgency, or suspicious payment phrasing. Treat as unsafe until verified through official channels."
            RiskLevel.CRITICAL -> "Severe fraud signatures matched (such as requesting a UPI PIN/OTP to receive money, raw IP/typosquatted phishing links, or impersonated KYC account-freeze threats)."
        }

        val recommendation = when (scanType) {
            ScanType.SMS -> if (clampedScore >= 55) {
                "Do not tap embedded links, call numbers inside the SMS, or share OTPs. Check your account directly inside your official bank app."
            } else {
                "If this message was unexpected, confirm independently inside your official banking app rather than replying."
            }
            ScanType.URL -> if (clampedScore >= 55) {
                "Do not open this link, sign in, or install downloaded APK files. Navigate manually by typing the institution's official web address."
            } else {
                "Double-check the exact root domain spelling and HTTPS padlock before signing into any financial portal."
            }
            ScanType.UPI -> if (clampedScore >= 55) {
                "Decline this UPI request immediately. Entering a UPI PIN ALWAYS debits money from your account—you never enter a PIN to receive money."
            } else {
                "Confirm the verified legal payee name shown by your payment app before approving any transfer."
            }
        }

        return FraudAnalysisResult(
            scanType = scanType,
            inputContent = cleanInput,
            riskScore = clampedScore,
            riskLevel = riskLevel,
            headline = headline,
            explanation = explanation,
            indicators = indicators,
            recommendation = recommendation,
            engineUsed = AnalysisEngine.OFFLINE_HEURISTIC,
            privacyNotice = privacyWarning
        )
    }

    private fun scoreUrgency(lower: String, indicators: MutableList<String>): Int {
        var pts = 0
        val urgencyTerms = listOf(
            "urgent", "immediately", "within 24", "within 2 hours", "within 3", "tonight",
            "blocked", "suspended", "deactivated", "disconnected", "frozen", "last warning", "expire"
        )
        val matched = urgencyTerms.filter { lower.contains(it) }
        if (matched.isNotEmpty()) {
            pts = (matched.size * 12).coerceAtMost(32)
            indicators.add("(+$pts pts) Urgency or account-threat phrasing: ${matched.take(3).joinToString(", ")}.")
        }
        return pts
    }

    private fun scoreCredentialHarvest(lower: String, indicators: MutableList<String>): Int {
        var pts = 0
        val harvestPhrases = listOf(
            "share otp", "send otp", "confirm otp", "enter upi pin", "upi pin to receive",
            "update kyc", "kyc expired", "kyc pending", "pan update", "download apk", "quicksupport", "anydesk"
        )
        val matched = harvestPhrases.filter { lower.contains(it) }
        if (matched.isNotEmpty()) {
            pts = (matched.size * 22).coerceAtMost(48)
            indicators.add("(+$pts pts) Credential / OTP / remote-access solicitation: ${matched.take(3).joinToString(", ")}.")
        }
        return pts
    }

    private fun scoreLotteryAndRewards(lower: String, indicators: MutableList<String>): Int {
        val lureTerms = listOf(
            "congratulations", "won", "winner", "lottery", "prize",
            "cashback", "free", "claim now", "bonus", "jackpot"
        )
        val matched = lureTerms.filter { lower.contains(it) }
        if (matched.size >= 3 || lower.contains("lottery") || (matched.size >= 2 && lower.contains("claim"))) {
            val pts = if (matched.size >= 3) 34 else 26
            indicators.add("(+$pts pts) Unsolicited lottery, prize, or urgent cashback lure (${matched.take(3).joinToString(", ")}).")
            return pts
        }
        return 0
    }

    private fun scoreImpersonation(lower: String, indicators: MutableList<String>): Int {
        var pts = 0
        val brands = listOf("sbi", "hdfc", "icici", "axis", "paytm", "phonepe", "gpay", "rbi", "income tax", "electricity")
        val matched = brands.filter { lower.contains(it) }
        if (matched.isNotEmpty() && (lower.contains("http") || lower.contains("kyc") || lower.contains("block") || lower.contains("call"))) {
            pts = 16
            indicators.add("(+$pts pts) Institution impersonation cue (${matched.joinToString(", ").uppercase(Locale.US)}) with action demand.")
        }
        return pts
    }

    private fun scoreEmbeddedLinks(raw: String, indicators: MutableList<String>): Int {
        val urlRegex = Regex("""https?://[^\s]+|(?:bit\.ly|tinyurl\.com|t\.co|rb\.gy)/[^\s]+""", RegexOption.IGNORE_CASE)
        val urls = urlRegex.findAll(raw).map { it.value }.toList()
        if (urls.isEmpty()) return 0
        var maxUrlRisk = 0
        urls.forEach { link ->
            val urlNotes = mutableListOf<String>()
            val urlPts = inspectUrl(link, urlNotes)
            if (urlPts > maxUrlRisk) maxUrlRisk = urlPts
            indicators.addAll(urlNotes.take(2))
        }
        return (maxUrlRisk * 0.75).toInt().coerceAtMost(40)
    }

    private fun inspectUrl(rawUrl: String, indicators: MutableList<String>): Int {
        var pts = 0
        val candidate = if (rawUrl.startsWith("http://", true) || rawUrl.startsWith("https://", true)) {
            rawUrl
        } else {
            "https://$rawUrl"
        }
        val lower = rawUrl.lowercase(Locale.US)

        if (lower.startsWith("http://")) {
            pts += 20
            indicators.add("(+20 pts) Unencrypted HTTP link instead of HTTPS.")
        }

        val host = try {
            URI(candidate).host?.lowercase(Locale.US).orEmpty()
        } catch (_: Exception) {
            ""
        }

        if (host.isBlank()) {
            pts += 26
            indicators.add("(+26 pts) Malformed or obfuscated URL structure.")
            return pts
        }

        val ipRegex = Regex("""^\d{1,3}(?:\.\d{1,3}){3}$""")
        if (ipRegex.matches(host)) {
            pts += 42
            indicators.add("(+42 pts) Raw IPv4 host ($host) used instead of a registered domain.")
        }

        val shorteners = listOf("bit.ly", "tinyurl.com", "t.co", "rb.gy", "is.gd", "cutt.ly", "ngrok.io", "ngrok-free.app")
        if (shorteners.any { host == it || host.endsWith(".$it") }) {
            pts += 28
            indicators.add("(+28 pts) URL shortener or tunnel ($host) hides the final destination.")
        }

        val riskyTlds = listOf(".xyz", ".top", ".tk", ".ml", ".ga", ".cf", ".click", ".loan", ".buzz", ".work")
        if (riskyTlds.any { host.endsWith(it) }) {
            pts += 26
            indicators.add("(+26 pts) High-abuse top-level domain detected on '$host'.")
        }

        if (host.contains("xn--")) {
            pts += 30
            indicators.add("(+30 pts) Punycode/IDN domain detected — possible homograph lookalike.")
        }

        // Misleading spelling / digit substitution typosquatting check
        val typosquatPatterns = listOf(
            "sb1", "onl1ne", "hdfc0", "payt1m", "paytrn", "ic1ci", "ph0nepe", "g00gle", "amaz0n", "rb1", "npcl"
        )
        val typoHits = typosquatPatterns.filter { lower.contains(it) }
        if (typoHits.isNotEmpty()) {
            pts += 34
            indicators.add("(+34 pts) Misleading lookalike spelling / character substitution (${typoHits.joinToString(", ")}).")
        }

        if (host.count { it == '-' } >= 2 || host.split(".").size >= 4) {
            pts += 14
            indicators.add("(+14 pts) Multi-hyphen or deep-subdomain pattern often used in phishing URLs.")
        }

        val spoofBrands = listOf("sbi", "hdfc", "icici", "paytm", "phonepe", "gpay", "upi", "npci", "rbi", "kyc")
        val officialDomains = listOf("sbi.co.in", "onlinesbi.sbi", "hdfcbank.com", "icicibank.com", "rbi.org.in", "npci.org.in", "paytm.com")
        val isOfficial = officialDomains.any { host == it || host.endsWith(".$it") }
        if (!isOfficial && spoofBrands.any { host.contains(it) || lower.contains(it) }) {
            pts += 26
            indicators.add("(+26 pts) Financial brand or KYC keyword paired with non-official domain ($host).")
        }

        val suspiciousPathTerms = listOf("verify", "kyc", "login", "claim", "reward", "cashback", ".apk", "suspend", "free")
        val pathHits = suspiciousPathTerms.filter { lower.contains(it) }
        if (pathHits.isNotEmpty()) {
            val pathPts = (pathHits.size * 10).coerceAtMost(25)
            pts += pathPts
            indicators.add("(+$pathPts pts) Phishing path/download indicators: ${pathHits.take(3).joinToString(", ")}.")
        }

        if (isOfficial && lower.startsWith("https://") && pathHits.isEmpty() && typoHits.isEmpty()) {
            indicators.add("(-10 pts) Matches verified official financial domain ($host) over HTTPS.")
            pts = (pts - 10).coerceAtLeast(2)
        }

        return pts
    }

    private fun inspectUpiRequest(raw: String, lower: String, indicators: MutableList<String>): Int {
        var pts = 0
        val vpaRegex = Regex("""[a-zA-Z0-9.\-_]{2,}@[a-zA-Z0-9]{2,}""")
        val vpas = vpaRegex.findAll(raw).map { it.value.lowercase(Locale.US) }.toList()

        if (vpas.isEmpty() && !lower.contains("collect") && !lower.contains("upi")) {
            pts += 18
            indicators.add("(+18 pts) No standard UPI VPA (name@handle) found in request text.")
        }

        val riskyVpaTokens = listOf("reward", "cashback", "lottery", "prize", "kyc", "refund", "fake", "claim", "helpdesk", "support", "winner", "verify")
        vpas.forEach { vpa ->
            val hits = riskyVpaTokens.filter { vpa.contains(it) }
            if (hits.isNotEmpty()) {
                pts += 40
                indicators.add("(+40 pts) VPA '$vpa' contains high-risk lure keywords (${hits.joinToString(", ")}).")
            } else {
                indicators.add("(+0 pts) VPA syntax valid ($vpa); verify payee name matches intended recipient.")
            }
        }

        val receivePinTraps = listOf("receive", "claim", "refund", "cashback", "prize", "credit", "advance")
        val hasPinOrCollect = lower.contains("pin") || lower.contains("collect") || lower.contains("approve")
        if (hasPinOrCollect && receivePinTraps.any { lower.contains(it) }) {
            pts += 46
            indicators.add("(+46 pts) Collect-to-receive PIN trap: implies approving a request or entering UPI PIN to RECEIVE money.")
        } else if (lower.contains("collect")) {
            pts += 14
            indicators.add("(+14 pts) UPI Collect Request pulls funds OUT of your account—confirm you initiated it.")
        }

        if (lower.contains("urgent") || lower.contains("within") || lower.contains("expire") || lower.contains("immediately")) {
            pts += 16
            indicators.add("(+16 pts) Artificial time pressure to rush payment approval.")
        }

        val amountRegex = Regex("""(?:₹|rs\.?|inr)\s*([0-9,]+)""", RegexOption.IGNORE_CASE)
        val maxAmount = amountRegex.findAll(raw)
            .mapNotNull { it.groupValues[1].replace(",", "").toLongOrNull() }
            .maxOrNull() ?: 0L
        if (maxAmount >= 5_000L && pts >= 20) {
            pts += 10
            indicators.add("(+10 pts) Elevated transaction amount (₹$maxAmount) on unverified request.")
        }

        return pts
    }

    fun quickAssessUpiTransfer(vpa: String, amountInPaise: Long, note: String): Pair<Int, String?> {
        val combined = "UPI transfer of ₹${amountInPaise / 100} to $vpa. Note: $note"
        val res = analyze(ScanType.UPI, combined)
        val extraHighAmount = if (amountInPaise >= 500_000L && res.riskScore < 40) {
            "High demo transfer amount (₹${amountInPaise / 100}). Verify recipient VPA carefully."
        } else null
        return if (res.riskScore >= 50) {
            res.riskScore to "${res.riskLevel.label}: ${res.indicators.firstOrNull() ?: res.headline}"
        } else {
            res.riskScore to extraHighAmount
        }
    }
}
