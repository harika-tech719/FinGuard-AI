package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.DuplicateTransactionException
import com.example.data.local.FinGuardDatabase
import com.example.data.model.INITIAL_DEMO_BALANCE_PAISE
import com.example.data.model.TransactionType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    private lateinit var db: FinGuardDatabase

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, FinGuardDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `app_name matches FinGuard AI`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("FinGuard AI", appName)
    }

    @Test
    fun `multi-account demo wallet updates sender and receiver balances accurately and prevents overdrafts`() = runTest {
        val dao = db.walletDao()
        dao.ensureSeeded()

        val senderBefore = dao.getAccountByVpa("aarav.demo@finguard")!!
        val receiverBefore = dao.getAccountByVpa("priya.nair@okdemo")!!
        assertEquals(INITIAL_DEMO_BALANCE_PAISE, senderBefore.balanceInPaise) // ₹10,000.00
        assertEquals(350_000L, receiverBefore.balanceInPaise) // ₹3,500.00

        // 1. Add ₹2,500.00 demo funds to Aarav -> ₹12,500.00
        val topUpReceipt = dao.addDemoMoney(
            targetVpa = senderBefore.vpa,
            amountInPaise = 250_000L,
            note = "Test top-up",
            idempotencyKey = "req-topup-1"
        )
        assertEquals(1_250_000L, topUpReceipt.balanceAfterInPaise)
        assertEquals(1_250_000L, dao.getAccountByVpa(senderBefore.vpa)!!.balanceInPaise)
        assertTrue(topUpReceipt.isSimulated)

        // 2. Transfer ₹4,000.00 from Aarav to Priya -> Aarav: ₹8,500.00, Priya: ₹7,500.00
        val transferReceipt = dao.executeTransfer(
            type = TransactionType.DEBIT,
            txPrefix = "FG-WAL",
            title = "Transfer to Priya",
            senderVpa = senderBefore.vpa,
            counterpartyName = receiverBefore.ownerName,
            counterpartyVpa = receiverBefore.vpa,
            amountInPaise = 400_000L,
            note = "Rent split",
            idempotencyKey = "req-xfer-1"
        )
        val senderAfter = dao.getAccountByVpa(senderBefore.vpa)!!
        val receiverAfter = dao.getAccountByVpa(receiverBefore.vpa)!!
        assertEquals(850_000L, senderAfter.balanceInPaise)
        assertEquals(750_000L, receiverAfter.balanceInPaise)
        assertEquals(1_250_000L, transferReceipt.balanceBeforeInPaise)
        assertEquals(850_000L, transferReceipt.balanceAfterInPaise)
        assertEquals(350_000L, transferReceipt.receiverBalanceBeforeInPaise)
        assertEquals(750_000L, transferReceipt.receiverBalanceAfterInPaise)

        // Conservation of balances across both fictional accounts
        assertEquals(1_250_000L + 350_000L, senderAfter.balanceInPaise + receiverAfter.balanceInPaise)

        // 3. Overdraft check: ₹20,000.00 > ₹8,500.00 -> blocked, balances unchanged
        var overdraftBlocked = false
        try {
            dao.executeTransfer(
                type = TransactionType.DEBIT,
                txPrefix = "FG-WAL",
                title = "Overdraft test",
                senderVpa = senderBefore.vpa,
                counterpartyName = receiverBefore.ownerName,
                counterpartyVpa = receiverBefore.vpa,
                amountInPaise = 2_000_000L,
                note = "Should fail",
                idempotencyKey = "req-overdraft-1"
            )
        } catch (_: IllegalStateException) {
            overdraftBlocked = true
        }
        assertTrue("Overdraft must be rejected", overdraftBlocked)
        assertEquals(850_000L, dao.getAccountByVpa(senderBefore.vpa)!!.balanceInPaise)
        assertEquals(750_000L, dao.getAccountByVpa(receiverBefore.vpa)!!.balanceInPaise)

        // 4. Failed simulated UPI transfer leaves both balances unchanged and saves failed receipt
        val failedReceipt = dao.executeTransfer(
            type = TransactionType.UPI_SIM_FAILED,
            txPrefix = "UPI-SIM",
            title = "Simulated Failure",
            senderVpa = senderBefore.vpa,
            counterpartyName = receiverBefore.ownerName,
            counterpartyVpa = receiverBefore.vpa,
            amountInPaise = 100_000L,
            note = "Network test",
            idempotencyKey = "req-fail-1",
            forceFailureReason = "SIMULATED NETWORK TIMEOUT"
        )
        assertEquals("SIMULATED_FAILED", failedReceipt.status)
        assertEquals(850_000L, dao.getAccountByVpa(senderBefore.vpa)!!.balanceInPaise)
        assertEquals(750_000L, dao.getAccountByVpa(receiverBefore.vpa)!!.balanceInPaise)

        // 5. Reset restores all fictional accounts to initial balances & resets ledger
        dao.resetDemoWallet()
        assertEquals(INITIAL_DEMO_BALANCE_PAISE, dao.getAccountByVpa("aarav.demo@finguard")!!.balanceInPaise)
        assertEquals(350_000L, dao.getAccountByVpa("priya.nair@okdemo")!!.balanceInPaise)
        assertEquals(1, dao.observeTransactions().first().size)
    }

    @Test
    fun `duplicate transactions are blocked by idempotency key and rapid duplicate window`() = runTest {
        val dao = db.walletDao()
        dao.ensureSeeded()

        val first = dao.executeTransfer(
            type = TransactionType.DEBIT,
            txPrefix = "FG-WAL",
            title = "Coffee split",
            senderVpa = "aarav.demo@finguard",
            counterpartyName = "Rohan Verma (Demo)",
            counterpartyVpa = "rohan.v@finguard",
            amountInPaise = 50_000L, // ₹500.00
            note = "Coffee",
            idempotencyKey = "idem-key-unique-1"
        )
        assertNotNull(first)
        assertEquals(950_000L, dao.getAccountByVpa("aarav.demo@finguard")!!.balanceInPaise)
        assertEquals(300_000L, dao.getAccountByVpa("rohan.v@finguard")!!.balanceInPaise)

        // Same idempotency key must throw DuplicateTransactionException
        var sameKeyBlocked = false
        try {
            dao.executeTransfer(
                type = TransactionType.DEBIT,
                txPrefix = "FG-WAL",
                title = "Coffee split",
                senderVpa = "aarav.demo@finguard",
                counterpartyName = "Rohan Verma (Demo)",
                counterpartyVpa = "rohan.v@finguard",
                amountInPaise = 50_000L,
                note = "Coffee",
                idempotencyKey = "idem-key-unique-1"
            )
        } catch (_: DuplicateTransactionException) {
            sameKeyBlocked = true
        }
        assertTrue("Replay with same idempotencyKey must be blocked", sameKeyBlocked)

        // Different key but identical parameters within rapid duplicate window must also be blocked
        var rapidDuplicateBlocked = false
        try {
            dao.executeTransfer(
                type = TransactionType.DEBIT,
                txPrefix = "FG-WAL",
                title = "Coffee split",
                senderVpa = "aarav.demo@finguard",
                counterpartyName = "Rohan Verma (Demo)",
                counterpartyVpa = "rohan.v@finguard",
                amountInPaise = 50_000L,
                note = "Coffee",
                idempotencyKey = "idem-key-unique-2"
            )
        } catch (_: DuplicateTransactionException) {
            rapidDuplicateBlocked = true
        }
        assertTrue("Rapid identical transfer must be blocked", rapidDuplicateBlocked)

        // Balances remain unchanged after blocked duplicates
        assertEquals(950_000L, dao.getAccountByVpa("aarav.demo@finguard")!!.balanceInPaise)
        assertEquals(300_000L, dao.getAccountByVpa("rohan.v@finguard")!!.balanceInPaise)
    }

    @Test
    fun `offline sms, url, and upi scans persist in Room scan history with timestamp, riskLevel, and explanation`() = runTest {
        val repo = com.example.data.FinGuardRepository(db.walletDao(), db.scanHistoryDao())

        val smsResult = repo.runFraudScan(
            com.example.data.model.ScanType.SMS,
            "URGENT: Account suspended tonight. Update KYC at http://sbi-kyc.xyz and share OTP.",
            useGeminiAi = false
        )
        val urlResult = repo.runFraudScan(
            com.example.data.model.ScanType.URL,
            "https://www.rbi.org.in/commonman/English/Scripts/Home.aspx",
            useGeminiAi = false
        )
        val upiResult = repo.runFraudScan(
            com.example.data.model.ScanType.UPI,
            "Collect request from cashback.lottery@fakeupi: enter UPI PIN to receive ₹4,999",
            useGeminiAi = false
        )

        val saved = db.scanHistoryDao().observeAllScans().first()
        assertEquals(3, saved.size)
        assertTrue(saved.all { it.createdAt > 0L && it.explanation.isNotBlank() && it.indicatorsJoined.isNotBlank() })
        assertEquals(com.example.data.model.RiskLevel.CRITICAL.name, upiResult.riskLevel.name)
        assertEquals(com.example.data.model.RiskLevel.LOW.name, urlResult.riskLevel.name)
        assertTrue(smsResult.riskScore >= 55)
    }

    @Test
    fun `gemini ai success, api failure fallback, and offline fallback persist scans accurately`() = runTest {
        val successAi = object : com.example.fraud.FraudAiService {
            override suspend fun analyze(
                scanType: com.example.data.model.ScanType,
                rawInput: String
            ): com.example.data.model.FraudAnalysisResult {
                return com.example.data.model.FraudAnalysisResult(
                    scanType = scanType,
                    inputContent = rawInput,
                    riskScore = 88,
                    riskLevel = com.example.data.model.RiskLevel.CRITICAL,
                    headline = "Urgency & Fake KYC Link",
                    explanation = "This SMS pressures you with an account suspension threat and points to an unverified domain.",
                    indicators = listOf("Account freeze threat", "Lookalike domain"),
                    recommendation = "Open your official bank app directly instead of clicking links.",
                    engineUsed = com.example.data.model.AnalysisEngine.GEMINI_AI
                )
            }
        }
        val onlineRepo = com.example.data.FinGuardRepository(
            walletDao = db.walletDao(),
            scanHistoryDao = db.scanHistoryDao(),
            aiService = successAi,
            isNetworkAvailable = { true }
        )
        val aiRes = onlineRepo.runFraudScan(
            com.example.data.model.ScanType.SMS,
            "URGENT: Update KYC at http://sbi-kyc.xyz",
            useGeminiAi = true
        )
        assertEquals(com.example.data.model.AnalysisEngine.GEMINI_AI, aiRes.engineUsed)
        assertEquals(88, aiRes.riskScore)
        assertEquals(null, aiRes.fallbackNotice)

        // API error automatically falls back to local rule detector
        val failingAi = object : com.example.fraud.FraudAiService {
            override suspend fun analyze(
                scanType: com.example.data.model.ScanType,
                rawInput: String
            ): com.example.data.model.FraudAnalysisResult {
                throw IllegalStateException("Service quota exceeded")
            }
        }
        val fallbackRepo = com.example.data.FinGuardRepository(
            walletDao = db.walletDao(),
            scanHistoryDao = db.scanHistoryDao(),
            aiService = failingAi,
            isNetworkAvailable = { true }
        )
        val errorFallbackRes = fallbackRepo.runFraudScan(
            com.example.data.model.ScanType.URL,
            "http://192.168.45.12/paytm-cashback-claim/verify-kyc.php",
            useGeminiAi = true
        )
        assertEquals(com.example.data.model.AnalysisEngine.OFFLINE_HEURISTIC, errorFallbackRes.engineUsed)
        assertTrue(errorFallbackRes.fallbackNotice?.contains("Automatically analyzed using the Offline Local Rules Shield") == true)
        assertTrue(errorFallbackRes.riskScore >= 55)

        // No internet automatically uses local detector without calling AI
        val offlineRepo = com.example.data.FinGuardRepository(
            walletDao = db.walletDao(),
            scanHistoryDao = db.scanHistoryDao(),
            aiService = failingAi,
            isNetworkAvailable = { false }
        )
        val offlineRes = offlineRepo.runFraudScan(
            com.example.data.model.ScanType.UPI,
            "Collect request from cashback.lottery@fakeupi: enter UPI PIN to receive ₹4,999",
            useGeminiAi = true
        )
        assertEquals(com.example.data.model.AnalysisEngine.OFFLINE_HEURISTIC, offlineRes.engineUsed)
        assertTrue(offlineRes.fallbackNotice?.contains("No internet connection detected") == true)
        assertEquals(com.example.data.model.RiskLevel.CRITICAL, offlineRes.riskLevel)

        val allSaved = db.scanHistoryDao().observeAllScans().first()
        assertEquals(3, allSaved.size)
    }

    @Test
    fun `wallet balances, transactions, and scan history persist across app restarts`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbName = "qa_restart_persistence_test.db"
        context.deleteDatabase(dbName)

        // Session 1: Create DB, perform wallet mutations, UPI simulation, and fraud scan
        val dbSession1 = Room.databaseBuilder(context, FinGuardDatabase::class.java, dbName)
            .allowMainThreadQueries()
            .build()
        val repo1 = com.example.data.FinGuardRepository(dbSession1.walletDao(), dbSession1.scanHistoryDao())
        repo1.initializeSandbox()
        repo1.addDemoMoney("aarav.demo@finguard", 150_000L, "Persist test", "persist-add-1") // +₹1,500 -> ₹11,500
        repo1.transferBetweenAccounts(
            senderVpa = "aarav.demo@finguard",
            receiverName = "Priya Nair (Demo)",
            receiverVpa = "priya.nair@okdemo",
            amountInPaise = 50_000L, // -₹500 -> Aarav ₹11,000, Priya ₹4,000
            note = "Persist transfer",
            idempotencyKey = "persist-xfer-1"
        )
        repo1.runFraudScan(
            com.example.data.model.ScanType.URL,
            "http://192.168.45.12/verify-kyc.php",
            useGeminiAi = false
        )
        dbSession1.close()

        // Session 2: Re-open DB from disk (simulating cold app restart)
        val dbSession2 = Room.databaseBuilder(context, FinGuardDatabase::class.java, dbName)
            .allowMainThreadQueries()
            .build()
        val repo2 = com.example.data.FinGuardRepository(dbSession2.walletDao(), dbSession2.scanHistoryDao())
        repo2.initializeSandbox() // Must NOT overwrite existing accounts!

        assertEquals(1_100_000L, dbSession2.walletDao().getAccountByVpa("aarav.demo@finguard")!!.balanceInPaise)
        assertEquals(400_000L, dbSession2.walletDao().getAccountByVpa("priya.nair@okdemo")!!.balanceInPaise)
        assertEquals(3, dbSession2.walletDao().observeTransactions().first().size)
        assertEquals(1, dbSession2.scanHistoryDao().observeAllScans().first().size)

        dbSession2.close()
        context.deleteDatabase(dbName)
    }

    @Test
    fun `upi simulator handles success, network failure, bank decline, and auto-shield risk block`() = runTest {
        val repo = com.example.data.FinGuardRepository(db.walletDao(), db.scanHistoryDao())
        repo.initializeSandbox()
        val sender = com.example.data.model.DemoCatalog.senderProfiles.first()
        val safeReceiver = com.example.data.model.DemoCatalog.receiverProfiles.first()
        val riskyReceiver = com.example.data.model.DemoCatalog.receiverProfiles.last()

        // Force Network Failure
        val netFail = repo.simulateUpiTransfer(
            sender = sender,
            receiverName = safeReceiver.displayName,
            receiverVpa = safeReceiver.vpa,
            amountInPaise = 50_000L,
            note = "Timeout check",
            outcomeMode = com.example.data.model.SimulationOutcome.FORCE_NETWORK_FAILURE,
            idempotencyKey = "upi-net-fail-1"
        )
        assertEquals("SIMULATED_FAILED", netFail.status)
        assertTrue(netFail.txRef.startsWith("UPI-SIM-"))
        assertEquals(INITIAL_DEMO_BALANCE_PAISE, db.walletDao().getAccountByVpa(sender.vpa)!!.balanceInPaise)

        // Force Bank Decline
        val bankDecline = repo.simulateUpiTransfer(
            sender = sender,
            receiverName = safeReceiver.displayName,
            receiverVpa = safeReceiver.vpa,
            amountInPaise = 50_000L,
            note = "Bank decline check",
            outcomeMode = com.example.data.model.SimulationOutcome.FORCE_BANK_DECLINE,
            idempotencyKey = "upi-bank-fail-1"
        )
        assertEquals("SIMULATED_FAILED", bankDecline.status)
        assertEquals(INITIAL_DEMO_BALANCE_PAISE, db.walletDao().getAccountByVpa(sender.vpa)!!.balanceInPaise)

        // Auto mode blocks risky scam VPA automatically
        val autoBlocked = repo.simulateUpiTransfer(
            sender = sender,
            receiverName = riskyReceiver.displayName,
            receiverVpa = riskyReceiver.vpa,
            amountInPaise = 50_000L,
            note = "Enter PIN to claim lottery reward",
            outcomeMode = com.example.data.model.SimulationOutcome.AUTO,
            idempotencyKey = "upi-auto-block-1"
        )
        assertEquals("SIMULATED_FAILED", autoBlocked.status)
        assertTrue(autoBlocked.failureReason?.contains("BLOCKED BY FINGUARD SHIELD") == true)
        assertEquals(INITIAL_DEMO_BALANCE_PAISE, db.walletDao().getAccountByVpa(sender.vpa)!!.balanceInPaise)

        // Force Success updates both balances
        val success = repo.simulateUpiTransfer(
            sender = sender,
            receiverName = safeReceiver.displayName,
            receiverVpa = safeReceiver.vpa,
            amountInPaise = 75_000L, // ₹750
            note = "Coffee split",
            outcomeMode = com.example.data.model.SimulationOutcome.FORCE_SUCCESS,
            idempotencyKey = "upi-success-1"
        )
        assertEquals("SIMULATED_SUCCESS", success.status)
        assertEquals(925_000L, db.walletDao().getAccountByVpa(sender.vpa)!!.balanceInPaise)
        assertEquals(425_000L, db.walletDao().getAccountByVpa(safeReceiver.vpa)!!.balanceInPaise)
    }
}
