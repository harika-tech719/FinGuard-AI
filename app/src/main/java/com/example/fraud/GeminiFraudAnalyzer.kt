package com.example.fraud

import android.util.Log
import com.example.data.model.AnalysisEngine
import com.example.data.model.FraudAnalysisResult
import com.example.data.model.RiskLevel
import com.example.data.model.ScanType
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

private const val TAG = "GeminiFraudAnalyzer"

interface FraudAiService {
    suspend fun analyze(scanType: ScanType, rawInput: String): FraudAnalysisResult
}

class GeminiFraudAnalyzer : FraudAiService {

    private val fraudSchema = Schema.obj(
        properties = mapOf(
            "riskScore" to Schema.integer(
                description = "Advisory heuristic risk score from 0 (low risk) to 100 (critical scam indicators)"
            ),
            "riskLevel" to Schema.enumeration(
                values = listOf("LOW", "MEDIUM", "HIGH", "CRITICAL"),
                description = "Overall heuristic risk tier (LOW, MEDIUM, HIGH, or CRITICAL)"
            ),
            "headline" to Schema.string(description = "Short 3-6 word summary headline"),
            "explanation" to Schema.string(
                description = "Simple, plain-language 2-3 sentence explanation of why this may be dangerous or routine, without claiming verified fraud probability"
            ),
            "indicators" to Schema.array(
                items = Schema.string(),
                description = "2 to 5 specific red flags or safety signals noticed in the text, URL, or UPI request"
            ),
            "recommendation" to Schema.string(
                description = "Clear, practical safety steps the user should take next"
            )
        )
    )

    private val model by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = fraudSchema
                temperature = 0.2f
            },
            systemInstruction = content {
                text(
                    "You are FinGuard AI, a helpful financial safety assistant. Explain suspicious SMS messages, URLs, " +
                        "and simulated UPI requests in simple, understandable language anyone can follow. " +
                        "Highlight why something may be dangerous and provide practical safety recommendations. " +
                        "Your assessment is advisory only—never claim a score is a verified fraud probability. " +
                        "Never ask for real OTPs, UPI PINs, bank passwords, or card details. " +
                        "Remind users when relevant that a UPI PIN is only for sending money out, never for receiving refunds or prizes."
                )
            }
        )
    }

    override suspend fun analyze(scanType: ScanType, rawInput: String): FraudAnalysisResult {
        val (cleanInput, privacyNotice) = OfflineHeuristicAnalyzer.sanitizeSensitiveData(rawInput.trim())
        val prompt = buildString {
            appendLine("Analyze this ${scanType.label} submission for potential financial scam signals.")
            appendLine("Category: ${scanType.name}")
            appendLine("Submission:")
            appendLine("\"\"\"")
            appendLine(cleanInput)
            appendLine("\"\"\"")
            appendLine(
                "Respond with strict JSON matching the schema: riskScore (0-100), riskLevel (LOW/MEDIUM/HIGH/CRITICAL), " +
                    "headline, simple plain-language explanation, specific indicators, and practical safety recommendation."
            )
        }

        try {
            val response = withTimeout(45_000L) {
                model.generateContent(prompt)
            }
            val jsonText = response.text?.takeIf { it.isNotBlank() }
                ?: throw IllegalStateException("Gemini AI returned an empty response.")

            val json = JSONObject(jsonText)
            val score = json.optInt("riskScore", 50).coerceIn(0, 100)
            val level = RiskLevel.fromRaw(json.optString("riskLevel"), score)
            val headline = json.optString("headline").ifBlank { level.label }
            val explanation = json.optString("explanation").ifBlank {
                "Advisory AI review completed. Verify through official banking channels."
            }
            val indicatorsJson = json.optJSONArray("indicators")
            val indicators = if (indicatorsJson != null) {
                (0 until indicatorsJson.length()).mapNotNull { idx ->
                    indicatorsJson.optString(idx).takeIf { it.isNotBlank() }
                }
            } else {
                emptyList()
            }
            val recommendation = json.optString("recommendation").ifBlank {
                "Verify directly through your official bank or payment app before taking any action."
            }

            return FraudAnalysisResult(
                scanType = scanType,
                inputContent = cleanInput,
                riskScore = score,
                riskLevel = level,
                headline = headline,
                explanation = explanation,
                indicators = indicators.ifEmpty { listOf("Analyzed by Gemini AI Financial Shield.") },
                recommendation = recommendation,
                engineUsed = AnalysisEngine.GEMINI_AI,
                privacyNotice = privacyNotice,
                fallbackNotice = null
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Gemini fraud scan timed out", e)
            throw IllegalStateException("Gemini AI request timed out after 45 seconds.", e)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Gemini fraud scan failed", e)
            throw IllegalStateException(
                e.localizedMessage ?: "Gemini AI service is currently unavailable.",
                e
            )
        }
    }
}
