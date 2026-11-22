package com.example.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.FinGuardViewModel
import com.example.ui.components.FinGuardBackground
import com.example.ui.screens.BudgetPlannerScreen
import com.example.ui.screens.DemoWalletScreen
import com.example.ui.screens.FraudDetectorScreen
import com.example.ui.screens.HomeDashboardScreen
import com.example.ui.screens.ScanHistoryScreen
import com.example.ui.screens.SecurityEducationScreen
import com.example.ui.screens.UpiSimulationScreen

object FinGuardRoutes {
    const val HOME = "home_dashboard"
    const val WALLET = "demo_wallet"
    const val UPI_SIM = "upi_simulation"
    const val SHIELD = "fraud_shield"
    const val HISTORY = "scan_history"
    const val LEARN = "security_education"
    const val BUDGET = "budget_planner"
}

private data class BottomNavTab(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

private val topLevelTabs = listOf(
    BottomNavTab(
        route = FinGuardRoutes.HOME,
        label = "Home",
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home,
        testTag = "nav_home"
    ),
    BottomNavTab(
        route = FinGuardRoutes.WALLET,
        label = "Wallet",
        selectedIcon = Icons.Filled.AccountBalanceWallet,
        unselectedIcon = Icons.Outlined.AccountBalanceWallet,
        testTag = "nav_wallet"
    ),
    BottomNavTab(
        route = FinGuardRoutes.UPI_SIM,
        label = "UPI Sim",
        selectedIcon = Icons.Filled.SwapHoriz,
        unselectedIcon = Icons.Outlined.SwapHoriz,
        testTag = "nav_upi_sim"
    ),
    BottomNavTab(
        route = FinGuardRoutes.SHIELD,
        label = "Shield",
        selectedIcon = Icons.Filled.Security,
        unselectedIcon = Icons.Outlined.Security,
        testTag = "nav_shield"
    ),
    BottomNavTab(
        route = FinGuardRoutes.LEARN,
        label = "Learn",
        selectedIcon = Icons.Filled.MenuBook,
        unselectedIcon = Icons.Outlined.MenuBook,
        testTag = "nav_learn"
    )
)

@Composable
fun FinGuardAppShell(
    viewModel: FinGuardViewModel = viewModel()
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val isSubScreenWithoutBar = currentDestination?.route == FinGuardRoutes.HISTORY ||
        currentDestination?.route == FinGuardRoutes.BUDGET

    val homeState by viewModel.homeState.collectAsStateWithLifecycle()
    val walletState by viewModel.walletState.collectAsStateWithLifecycle()
    val upiState by viewModel.upiSimState.collectAsStateWithLifecycle()
    val detectorState by viewModel.detectorState.collectAsStateWithLifecycle()
    val historyState by viewModel.scanHistoryState.collectAsStateWithLifecycle()
    val educationState by viewModel.educationState.collectAsStateWithLifecycle()
    val budgetState by viewModel.budgetState.collectAsStateWithLifecycle()

    FinGuardBackground {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val useRail = maxWidth >= 600.dp

            Scaffold(
                containerColor = Color.Transparent,
                contentWindowInsets = WindowInsets.safeDrawing,
                bottomBar = {
                    if (!useRail && !isSubScreenWithoutBar) {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                            tonalElevation = 4.dp
                        ) {
                            topLevelTabs.forEach { tab ->
                                val selected = currentDestination?.hierarchy?.any { it.route == tab.route } == true
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { navController.navigateSingleTopTo(tab.route) },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = tab.label
                                        )
                                    },
                                    label = { Text(tab.label, maxLines = 1) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = MaterialTheme.colorScheme.tertiary,
                                        selectedTextColor = MaterialTheme.colorScheme.tertiary,
                                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    modifier = Modifier.testTag(tab.testTag)
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    if (useRail && !isSubScreenWithoutBar) {
                        NavigationRail(
                            containerColor = MaterialTheme.colorScheme.surface
                        ) {
                            topLevelTabs.forEach { tab ->
                                val selected = currentDestination?.hierarchy?.any { it.route == tab.route } == true
                                NavigationRailItem(
                                    selected = selected,
                                    onClick = { navController.navigateSingleTopTo(tab.route) },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = tab.label
                                        )
                                    },
                                    label = { Text(tab.label) },
                                    colors = NavigationRailItemDefaults.colors(
                                        selectedIconColor = MaterialTheme.colorScheme.tertiary,
                                        selectedTextColor = MaterialTheme.colorScheme.tertiary,
                                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    modifier = Modifier.testTag(tab.testTag)
                                )
                            }
                        }
                    }

                    Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                        NavHost(
                            navController = navController,
                            startDestination = FinGuardRoutes.HOME
                        ) {
                            composable(FinGuardRoutes.HOME) {
                                HomeDashboardScreen(
                                    state = homeState,
                                    onNavigateToWallet = { navController.navigateSingleTopTo(FinGuardRoutes.WALLET) },
                                    onNavigateToUpiSim = { navController.navigateSingleTopTo(FinGuardRoutes.UPI_SIM) },
                                    onNavigateToShield = { navController.navigateSingleTopTo(FinGuardRoutes.SHIELD) },
                                    onNavigateToHistory = { navController.navigate(FinGuardRoutes.HISTORY) },
                                    onNavigateToLearn = { navController.navigateSingleTopTo(FinGuardRoutes.LEARN) },
                                    onNavigateToBudget = { navController.navigate(FinGuardRoutes.BUDGET) }
                                )
                            }

                            composable(FinGuardRoutes.WALLET) {
                                DemoWalletScreen(
                                    state = walletState,
                                    onSelectAccount = viewModel::selectWalletAccount,
                                    onAddDemoMoney = viewModel::addDemoMoney,
                                    onTransferBetweenAccounts = viewModel::transferBetweenAccounts,
                                    onSelectFilter = viewModel::setWalletFilter,
                                    onSelectTransaction = viewModel::selectTransaction,
                                    onDismissReceipt = viewModel::dismissWalletReceipt,
                                    onResetWallet = viewModel::resetDemoWallet,
                                    onClearMessages = viewModel::clearWalletMessages
                                )
                            }

                            composable(FinGuardRoutes.UPI_SIM) {
                                UpiSimulationScreen(
                                    state = upiState,
                                    onSelectSender = viewModel::selectUpiSender,
                                    onSelectReceiver = viewModel::selectUpiReceiver,
                                    onToggleCustomReceiver = viewModel::setCustomReceiverMode,
                                    onUpdateCustomName = viewModel::updateCustomReceiverName,
                                    onUpdateCustomVpa = viewModel::updateCustomReceiverVpa,
                                    onUpdateAmount = viewModel::updateUpiAmount,
                                    onUpdateNote = viewModel::updateUpiNote,
                                    onSelectOutcomeMode = viewModel::selectSimulationOutcome,
                                    onExecuteSimulation = { viewModel.executeSimulatedUpiPayment() },
                                    onDismissReceipt = viewModel::dismissUpiReceipt
                                )
                            }

                            composable(FinGuardRoutes.SHIELD) {
                                FraudDetectorScreen(
                                    state = detectorState,
                                    onSelectScanType = viewModel::selectScanType,
                                    onUpdateInput = viewModel::updateScanInput,
                                    onToggleGeminiAi = viewModel::toggleGeminiEngine,
                                    onLoadSample = viewModel::loadSamplePrompt,
                                    onRunScan = viewModel::runFraudScan,
                                    onOpenHistory = { navController.navigate(FinGuardRoutes.HISTORY) }
                                )
                            }

                            composable(FinGuardRoutes.HISTORY) {
                                ScanHistoryScreen(
                                    state = historyState,
                                    onBack = { navController.popBackStack() },
                                    onFilterType = viewModel::setHistoryTypeFilter,
                                    onFilterRisk = viewModel::setHistoryRiskFilter,
                                    onSelectRecord = viewModel::selectHistoryRecord,
                                    onDeleteRecord = viewModel::deleteScanRecord,
                                    onClearAll = viewModel::clearAllScanHistory,
                                    onLaunchScanner = {
                                        navController.popBackStack()
                                        navController.navigateSingleTopTo(FinGuardRoutes.SHIELD)
                                    }
                                )
                            }

                            composable(FinGuardRoutes.LEARN) {
                                SecurityEducationScreen(
                                    state = educationState,
                                    onSelectCategory = viewModel::selectEducationCategory,
                                    onToggleLessonCompleted = viewModel::toggleLessonCompleted,
                                    onSelectQuizAnswer = viewModel::selectQuizAnswer
                                )
                            }

                            composable(FinGuardRoutes.BUDGET) {
                                BudgetPlannerScreen(
                                    state = budgetState,
                                    onBack = { navController.popBackStack() },
                                    onSaveBudgetSettings = viewModel::saveBudgetSettings,
                                    onSaveExpense = viewModel::saveExpense,
                                    onDeleteExpense = viewModel::deleteExpense,
                                    onSelectCategoryFilter = viewModel::selectBudgetCategoryFilter,
                                    onSelectReportPeriod = viewModel::selectReportPeriod,
                                    onCreateSavingsGoal = viewModel::createSavingsGoal,
                                    onContributeGoal = viewModel::contributeToGoal,
                                    onDeleteSavingsGoal = viewModel::deleteSavingsGoal,
                                    onToggleGemini = viewModel::toggleBudgetGemini,
                                    onGenerateAdvice = viewModel::generateBudgetAdvice,
                                    onResetToBlank = viewModel::resetBudgetToBlank,
                                    onRestoreSampleData = viewModel::restoreSampleBudgetData,
                                    onClearMessages = viewModel::clearBudgetMessages
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun NavHostController.navigateSingleTopTo(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}
