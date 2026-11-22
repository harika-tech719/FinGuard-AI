package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.FinGuardDatabase
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class FinGuardComposeQaTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun resetSandboxState() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = FinGuardDatabase.getInstance(context)
        runBlocking {
            db.walletDao().resetDemoWallet()
            db.scanHistoryDao().clearAllScans()
            db.budgetDao().restoreSampleBudgetData()
        }
        composeRule.waitForIdle()
    }

    @Test
    fun qa_01_homeDashboard_navigation_and_backNavigation_workAcrossAllModules() {
        composeRule.waitForIdle()
        composeRule.onNodeWithText("FIN GUARD AI").assertIsDisplayed()
        composeRule.onNodeWithTag("home_wallet_balance").assertIsDisplayed()

        // Scroll Home LazyColumn to Quick Access -> Scan History -> TopAppBar Back returns to Home
        composeRule.onNodeWithTag("home_lazy_list").performScrollToNode(hasTestTag("quick_scan_history"))
        composeRule.onNodeWithTag("quick_scan_history").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Scan History").assertIsDisplayed()
        composeRule.onNodeWithTag("history_back_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("home_lazy_list").performScrollToIndex(0)
        composeRule.onNodeWithText("FIN GUARD AI").assertIsDisplayed()

        // Also verify system BackDispatcher works from Scan History
        composeRule.onNodeWithTag("home_lazy_list").performScrollToNode(hasTestTag("quick_scan_history"))
        composeRule.onNodeWithTag("quick_scan_history").performClick()
        composeRule.waitForIdle()
        composeRule.activityRule.scenario.onActivity { activity ->
            activity.onBackPressedDispatcher.onBackPressed()
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("home_lazy_list").performScrollToIndex(0)
        composeRule.onNodeWithText("FIN GUARD AI").assertIsDisplayed()

        // Bottom navigation tabs -> Wallet, UPI Sim, Shield, Learn, Home
        composeRule.onNodeWithTag("nav_wallet").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Demo Wallet").assertIsDisplayed()

        composeRule.onNodeWithTag("nav_upi_sim").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("UPI Simulation Lab").assertIsDisplayed()

        composeRule.onNodeWithTag("nav_shield").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Fraud Shield AI").assertIsDisplayed()

        composeRule.onNodeWithTag("nav_learn").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Fraud Defense Playbook").assertIsDisplayed()

        composeRule.onNodeWithTag("nav_home").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("FIN GUARD AI").assertIsDisplayed()
    }

    @Test
    fun qa_02_demoWallet_addMoney_transfer_receipt_and_reset_updateBalancesProperly() {
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("nav_wallet").performClick()
        composeRule.waitForIdle()

        // Verify initial ₹10,000.00 balance
        composeRule.onNodeWithTag("wallet_balance_text").assertIsDisplayed()
        composeRule.waitUntil(timeoutMillis = 5_000L) {
            composeRule.onAllNodesWithText("10,000.00", substring = true).fetchSemanticsNodes().isNotEmpty()
        }

        // Add ₹500 demo money
        composeRule.onNodeWithTag("wallet_add_money_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("add_money_amount_input").performTextClearance()
        composeRule.onNodeWithTag("add_money_amount_input").performTextInput("500")
        composeRule.onNodeWithTag("confirm_add_money").performClick()

        // Verify receipt dialog appears and dismiss it
        composeRule.waitUntil(timeoutMillis = 5_000L) {
            composeRule.onAllNodesWithText("Demo Funds Added").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("dismiss_wallet_receipt").performClick()
        composeRule.waitForIdle()

        // Balance should now be ₹10,500.00
        composeRule.waitUntil(timeoutMillis = 5_000L) {
            composeRule.onAllNodesWithText("10,500.00", substring = true).fetchSemanticsNodes().isNotEmpty()
        }

        // Transfer ₹1,500 to another fictional account -> ₹9,000.00
        composeRule.onNodeWithTag("wallet_transfer_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("transfer_amount_input").performTextClearance()
        composeRule.onNodeWithTag("transfer_amount_input").performTextInput("1500")
        composeRule.onNodeWithTag("confirm_demo_transfer").performClick()

        composeRule.waitUntil(timeoutMillis = 5_000L) {
            composeRule.onAllNodesWithText("Demo Transfer Completed").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("dismiss_wallet_receipt").performClick()
        composeRule.waitForIdle()

        composeRule.waitUntil(timeoutMillis = 5_000L) {
            composeRule.onAllNodesWithText("9,000.00", substring = true).fetchSemanticsNodes().isNotEmpty()
        }

        // Reset demo wallet via confirmation dialog -> ₹10,000.00 restored
        composeRule.onNodeWithTag("wallet_reset_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("confirm_reset_wallet").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000L) {
            composeRule.onAllNodesWithText("10,000.00", substring = true).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun qa_03_upiSimulator_runsFailedAndSuccessfulSimulatedTransfersWithReceipts() {
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("nav_upi_sim").performClick()
        composeRule.waitForIdle()

        // Scroll outer LazyColumn to Outcome row, then LazyRow to Force Bank Decline
        composeRule.onNodeWithTag("upi_lazy_list").performScrollToNode(hasTestTag("upi_outcome_row"))
        composeRule.onNodeWithTag("upi_outcome_row").performScrollToNode(hasTestTag("upi_outcome_force_bank_decline"))
        composeRule.onNodeWithTag("upi_outcome_force_bank_decline").performClick()

        composeRule.onNodeWithTag("upi_lazy_list").performScrollToNode(hasTestTag("upi_simulate_submit_button"))
        composeRule.onNodeWithTag("upi_simulate_submit_button").performClick()

        composeRule.waitUntil(timeoutMillis = 5_000L) {
            composeRule.onAllNodesWithText("Simulated Transfer Failed").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("dismiss_upi_receipt").performClick()
        composeRule.waitForIdle()

        // Force Success -> Simulated Transfer Approved receipt, balance drops by ₹500 to ₹9,500.00
        composeRule.onNodeWithTag("upi_lazy_list").performScrollToNode(hasTestTag("upi_outcome_row"))
        composeRule.onNodeWithTag("upi_outcome_row").performScrollToNode(hasTestTag("upi_outcome_force_success"))
        composeRule.onNodeWithTag("upi_outcome_force_success").performClick()

        composeRule.onNodeWithTag("upi_lazy_list").performScrollToNode(hasTestTag("upi_simulate_submit_button"))
        composeRule.onNodeWithTag("upi_simulate_submit_button").performClick()

        composeRule.waitUntil(timeoutMillis = 5_000L) {
            composeRule.onAllNodesWithText("Simulated Transfer Approved").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("dismiss_upi_receipt").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("upi_lazy_list").performScrollToIndex(0)
        composeRule.waitUntil(timeoutMillis = 5_000L) {
            composeRule.onAllNodesWithText("9,500.00", substring = true).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun qa_04_fraudShield_and_scanHistory_scanSmsUrlUpi_and_manageHistory() {
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("nav_shield").performClick()
        composeRule.waitForIdle()

        // Validate empty input notice
        composeRule.onNodeWithTag("fraud_lazy_list").performScrollToNode(hasTestTag("fraud_scan_button"))
        composeRule.onNodeWithTag("fraud_scan_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("fraud_lazy_list").performScrollToNode(hasText("Scan Notice"))
        composeRule.onNodeWithText("Scan Notice").assertIsDisplayed()

        // Switch off Gemini AI to test Offline Local Rules Shield directly via UI
        composeRule.onNodeWithTag("fraud_lazy_list").performScrollToNode(hasTestTag("shield_engine_switch"))
        composeRule.onNodeWithTag("shield_engine_switch").performClick()
        composeRule.waitForIdle()

        // Enter suspicious SMS (error clears automatically on input edit)
        composeRule.onNodeWithTag("fraud_lazy_list").performScrollToNode(hasTestTag("fraud_input_field"))
        composeRule.onNodeWithTag("fraud_input_field")
            .performTextInput("URGENT: Bank account BLOCKED tonight. Update KYC at http://sbi-kyc.xyz and share OTP.")
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("fraud_scan_button").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000L) {
            composeRule.onAllNodesWithText("Why This Score Was Assigned").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("fraud_lazy_list").performScrollToIndex(7)
        composeRule.onNodeWithText("Practical Safety Recommendation").assertIsDisplayed()

        // Clear input, switch to URL, and scan an IP phishing URL
        composeRule.onNodeWithTag("fraud_lazy_list").performScrollToIndex(0)
        composeRule.onNodeWithTag("scan_type_url").performClick()
        composeRule.onNodeWithTag("fraud_lazy_list").performScrollToNode(hasTestTag("fraud_clear_button"))
        composeRule.onNodeWithTag("fraud_clear_button").performClick()
        composeRule.onNodeWithTag("fraud_input_field")
            .performTextInput("http://192.168.45.12/paytm-cashback-claim/verify-kyc.php")
        composeRule.onNodeWithTag("fraud_scan_button").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000L) {
            composeRule.onAllNodesWithText("Why This Score Was Assigned").fetchSemanticsNodes().isNotEmpty()
        }

        // Clear input, switch to UPI, and scan a collect-to-receive PIN trap
        composeRule.onNodeWithTag("fraud_lazy_list").performScrollToIndex(0)
        composeRule.onNodeWithTag("scan_type_upi").performClick()
        composeRule.onNodeWithTag("fraud_lazy_list").performScrollToNode(hasTestTag("fraud_clear_button"))
        composeRule.onNodeWithTag("fraud_clear_button").performClick()
        composeRule.onNodeWithTag("fraud_input_field")
            .performTextInput("Collect request from cashback.lottery@fakeupi: enter UPI PIN to RECEIVE ₹4,999")
        composeRule.onNodeWithTag("fraud_scan_button").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000L) {
            composeRule.onAllNodesWithText("Why This Score Was Assigned").fetchSemanticsNodes().isNotEmpty()
        }

        // Open Scan History and verify all 3 reports + filter + Clear All dialog
        composeRule.onNodeWithTag("fraud_lazy_list").performScrollToIndex(0)
        composeRule.onNodeWithTag("shield_open_history_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("3 saved threat reports").assertIsDisplayed()

        // Filter by URL -> 1 report
        composeRule.onNodeWithTag("history_filter_type_url").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("1 saved threat reports").assertIsDisplayed()

        // Clear filter and clear all history via confirmation dialog
        composeRule.onNodeWithTag("history_filter_type_url").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("history_clear_all_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("confirm_clear_history").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000L) {
            composeRule.onAllNodesWithText("No matching scan records").fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun qa_05_securityAcademy_lessons_progress_filters_and_quizWorkEndToEnd() {
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("nav_learn").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("0 of 7 Playbooks Reviewed").assertIsDisplayed()

        // Expand lesson and mark reviewed
        composeRule.onNodeWithTag("education_lazy_list").performScrollToNode(hasTestTag("lesson_card_otp_1"))
        composeRule.onNodeWithTag("lesson_card_otp_1").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("education_lazy_list").performScrollToNode(hasTestTag("toggle_lesson_otp_1"))
        composeRule.onNodeWithTag("toggle_lesson_otp_1").performClick()
        composeRule.waitForIdle()

        // Scroll back to top banner to verify updated progress text
        composeRule.onNodeWithTag("education_lazy_list").performScrollToIndex(0)
        composeRule.onNodeWithText("1 of 7 Playbooks Reviewed").assertIsDisplayed()

        // Filter by UPI category
        composeRule.onNodeWithTag("edu_cat_upi").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("education_lazy_list").performScrollToNode(hasText("The Golden Rule of UPI PIN"))
        composeRule.onNodeWithText("The Golden Rule of UPI PIN").assertIsDisplayed()

        // Scroll to Quiz Q1 and pick correct option (index 1)
        composeRule.onNodeWithTag("education_lazy_list").performScrollToNode(hasTestTag("quiz_q1_opt_1"))
        composeRule.onNodeWithTag("quiz_q1_opt_1").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("education_lazy_list").performScrollToNode(hasText("Correct:", substring = true))
        composeRule.onNodeWithText("Correct:", substring = true).assertIsDisplayed()
    }

    @Test
    fun qa_06_budgetPlanner_charts_alerts_expenses_goals_aiCoach_and_resetWorkEndToEnd() {
        composeRule.waitForIdle()

        // Navigate from Home -> Smart Budget Planner
        composeRule.onNodeWithTag("home_lazy_list").performScrollToNode(hasTestTag("home_open_budget_button"))
        composeRule.onNodeWithTag("home_open_budget_button").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Smart Budget Planner").assertIsDisplayed()
        composeRule.waitUntil(timeoutMillis = 5_000L) {
            composeRule.onAllNodesWithText("45,000.00", substring = true).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("budget_income_value").assertIsDisplayed()

        // Verify 75% Budget Alert banner on sample data (19,250 / 25,000 = 77%)
        composeRule.onNodeWithTag("budget_lazy_list").performScrollToNode(hasTestTag("budget_alert_banner"))
        composeRule.onNodeWithText("75% ALERT").assertIsDisplayed()

        // Add ₹3,750 Travel expense -> pushes spend to ₹23,000 (92% -> 90% CRITICAL alert)
        composeRule.onNodeWithTag("budget_lazy_list").performScrollToNode(hasTestTag("budget_add_expense_button"))
        composeRule.onNodeWithTag("budget_add_expense_button").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("expense_title_input").performTextInput("Express Train Ticket")
        composeRule.onNodeWithTag("expense_amount_input").performTextInput("3750")
        composeRule.onNodeWithTag("expense_cat_travel").performClick()
        composeRule.onNodeWithTag("confirm_save_expense").performClick()

        composeRule.waitUntil(timeoutMillis = 5_000L) {
            composeRule.onAllNodesWithText("90% CRITICAL").fetchSemanticsNodes().isNotEmpty()
        }

        // Verify Category Donut filter toggles cleanly
        composeRule.onNodeWithTag("budget_lazy_list").performScrollToNode(hasTestTag("budget_cat_slice_food"))
        composeRule.onNodeWithTag("budget_cat_slice_food").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("budget_lazy_list").performScrollToNode(hasTestTag("budget_clear_cat_filter"))
        composeRule.onNodeWithTag("budget_clear_cat_filter").performClick()
        composeRule.waitForIdle()

        // Toggle Financial Reports between Monthly (30D) and Weekly (7D)
        composeRule.onNodeWithTag("budget_lazy_list").performScrollToNode(hasTestTag("report_period_monthly"))
        composeRule.onNodeWithTag("report_period_monthly").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Last 30 Days Summary").assertIsDisplayed()
        composeRule.onNodeWithTag("report_period_weekly").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Last 7 Days Summary").assertIsDisplayed()

        // Create a Savings Goal and verify it appears in the LazyColumn
        composeRule.onNodeWithTag("budget_lazy_list").performScrollToNode(hasTestTag("budget_add_goal_button"))
        composeRule.onNodeWithTag("budget_add_goal_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("goal_title_input").performTextInput("Travel Backup")
        composeRule.onNodeWithTag("goal_target_input").performTextInput("15000")
        composeRule.onNodeWithTag("confirm_create_goal").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("budget_lazy_list").performScrollToNode(hasText("Travel Backup"))
        composeRule.onNodeWithText("Travel Backup").assertIsDisplayed()

        // Toggle Offline Budget Coach and generate advice
        composeRule.onNodeWithTag("budget_lazy_list").performScrollToNode(hasTestTag("budget_ai_switch"))
        composeRule.onNodeWithTag("budget_ai_switch").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("budget_lazy_list").performScrollToNode(hasTestTag("budget_get_advice_button"))
        composeRule.onNodeWithTag("budget_get_advice_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("budget_lazy_list").performScrollToNode(hasText("OFFLINE COACH"))
        composeRule.onNodeWithText("OFFLINE COACH").assertIsDisplayed()

        // Reset to blank (Clear All) -> Restore Sample -> Back to Home
        composeRule.onNodeWithTag("budget_lazy_list").performScrollToNode(hasTestTag("budget_reset_button"))
        composeRule.onNodeWithTag("budget_reset_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("confirm_clear_budget").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("budget_lazy_list").performScrollToIndex(0)
        composeRule.waitUntil(timeoutMillis = 5_000L) {
            composeRule.onAllNodesWithText("0.00", substring = true).fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithTag("budget_lazy_list").performScrollToNode(hasTestTag("budget_reset_button"))
        composeRule.onNodeWithTag("budget_reset_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("confirm_restore_sample_budget").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("budget_lazy_list").performScrollToIndex(0)
        composeRule.waitUntil(timeoutMillis = 5_000L) {
            composeRule.onAllNodesWithText("45,000.00", substring = true).fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithTag("budget_back_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("home_lazy_list").performScrollToIndex(0)
        composeRule.onNodeWithText("FIN GUARD AI").assertIsDisplayed()
    }
}
