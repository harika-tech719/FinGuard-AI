package com.example.budget

import android.util.Log
import com.example.data.local.SavingsGoalEntity
import com.example.data.model.AnalysisEngine
import com.example.data.model.BudgetAdviceResult
import com.example.data.model.BudgetSnapshotMetrics
import com.example.data.model.FinGuardFormatters
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import kotlin.math.roundToInt

private const val TAG = "GeminiBudgetAdvisor"

interface BudgetAiService {
    suspend fun advise(
        metrics: BudgetSnapshotMetrics,
        goals: List<SavingsGoalEntity>
    ): BudgetAdviceResult
}

class GeminiBudgetAdvisor : BudgetAiService {

    private val adviceSchema = Schema.obj(
        properties = mapOf(
            "summary" to Schema.string(
                description = "Friendly 2-sentence overview of the user's current monthly spending vs budget and savings rate"
            ),
            "topFocusArea" to Schema.string(
                description = "Short 2-4 word label naming the #1 area to optimize"
            ),
            "suggestions" to Schema.array(
                items = Schema.string(),
                description = "3 to 4 practical, concrete budgeting recommendations tailored to the user's categories and goals"
            )
        )
    )

    private val model by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = adviceSchema
                temperature = 0.25f
            },
            systemInstruction = content {
                text(
                    "You are FinGuard Budget Coach, a practical personal finance educator. " +
                        "Provide clear, friendly, actionable budgeting suggestions based only on the user-entered demo numbers provided. " +
                        "Your suggestions are educational and advisory only. " +
                        "Never ask for real bank passwords, account numbers, UPI PINs, card details, or OTPs."
                )
            }
        )
    }

    override suspend fun advise(
        metrics: BudgetSnapshotMetrics,
        goals: List<SavingsGoalEntity>
    ): BudgetAdviceResult {
        val categoryLines = if (metrics.categoryBreakdown.isEmpty()) {
            "No expenses logged yet."
        } else {
            metrics.categoryBreakdown.joinToString("\n") { item ->
                "- ${item.category.label}: ${FinGuardFormatters.formatPaise(item.totalInPaise)} " +
                    "(${item.sharePercent.roundToInt()}%, ${item.itemCount} entries)"
            }
        }

        val goalsLines = if (goals.isEmpty()) {
            "No savings goals created yet."
        } else {
            goals.joinToString("\n") { goal ->
                "- ${goal.title}: saved ${FinGuardFormatters.formatPaise(goal.savedAmountInPaise)} " +
                    "of ${FinGuardFormatters.formatPaise(goal.targetAmountInPaise)}"
            }
        }

        val prompt = buildString {
            appendLine("Analyze this user-entered personal demo budget snapshot and provide practical suggestions:")
            appendLine("Monthly Income: ${FinGuardFormatters.formatPaise(metrics.monthlyIncomeInPaise)}")
            appendLine("Monthly Budget Limit: ${FinGuardFormatters.formatPaise(metrics.monthlyBudgetInPaise)}")
            appendLine("Total Expenses (30D): ${FinGuardFormatters.formatPaise(metrics.totalExpensesInPaise)}")
            appendLine("Remaining Budget: ${FinGuardFormatters.formatPaise(metrics.remainingBudgetInPaise)}")
            appendLine("Budget Utilization: ${metrics.budgetUsedPercent}% (${metrics.alertLevel.badgeText})")
            appendLine("Net Monthly Savings: ${FinGuardFormatters.formatPaise(metrics.netSavingsInPaise)} (${metrics.savingsRatePercent}%)")
            appendLine("Category Breakdown:")
            appendLine(categoryLines)
            appendLine("Savings Goals:")
            appendLine(goalsLines)
            appendLine("Return strict JSON matching the schema.")
        }

        try {
            val response = withTimeout(45_000L) {
                model.generateContent(prompt)
            }
            val jsonText = response.text?.takeIf { it.isNotBlank() }
                ?: throw IllegalStateException("Gemini AI returned an empty budgeting response.")

            val json = JSONObject(jsonText)
            val summary = json.optString("summary").ifBlank {
                "Review of your monthly spending and savings pace is ready."
            }
            val focus = json.optString("topFocusArea").ifBlank {
                metrics.categoryBreakdown.firstOrNull()?.category?.label ?: "Budget Pacing"
            }
            val arr = json.optJSONArray("suggestions")
            val suggestions = if (arr != null) {
                (0 until arr.length()).mapNotNull { idx ->
                    arr.optString(idx).takeIf { it.isNotBlank() }
                }
            } else {
                emptyList()
            }

            return BudgetAdviceResult(
                summary = summary,
                suggestions = suggestions.ifEmpty {
                    listOf("Track daily expenses consistently and reserve 20% of income toward your savings goals.")
                },
                topFocusArea = focus,
                engineUsed = AnalysisEngine.GEMINI_AI,
                fallbackNotice = null
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Gemini budget advice timed out", e)
            throw IllegalStateException("Gemini AI request timed out after 45 seconds.", e)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Gemini budget advice failed", e)
            throw IllegalStateException(
                e.localizedMessage ?: "Gemini AI budget coach is currently unavailable.",
                e
            )
        }
    }
}
