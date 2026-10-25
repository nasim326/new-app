package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.AppScreen
import com.example.ui.MainViewModel
import com.example.ui.screens.add.AddTransactionScreen
import com.example.ui.screens.auth.PinLockScreen
import com.example.ui.screens.budget.BudgetScreen
import com.example.ui.screens.dashboard.DashboardScreen
import com.example.ui.screens.reports.ReportsScreen
import com.example.ui.screens.savings.AccountDetailScreen
import com.example.ui.screens.savings.SavingsScreen
import com.example.ui.screens.settings.SettingsScreen
import com.example.ui.screens.transactions.TransactionsScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
            val isDark = when (themeMode) {
                "DARK" -> true
                "LIGHT" -> false
                else -> isSystemInDarkTheme()
            }

            MyApplicationTheme(darkTheme = isDark) {
                MainAppContent(viewModel = viewModel)
            }
        }
    }
}

data class NavItem(
    val screen: AppScreen,
    val title: String,
    val icon: ImageVector,
    val testTag: String
)

@Composable
fun MainAppContent(viewModel: MainViewModel) {
    val isAppUnlocked by viewModel.isAppUnlocked.collectAsStateWithLifecycle()
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()

    if (!isAppUnlocked) {
        PinLockScreen(viewModel = viewModel)
        return
    }

    val navItems = listOf(
        NavItem(AppScreen.DASHBOARD, "Dashboard", Icons.Default.Dashboard, "nav_dashboard"),
        NavItem(AppScreen.TRANSACTIONS, "History", Icons.Default.ReceiptLong, "nav_transactions"),
        NavItem(AppScreen.ADD_TRANSACTION, "Add", Icons.Default.Add, "nav_add"),
        NavItem(AppScreen.SAVINGS, "Savings", Icons.Default.Savings, "nav_savings"),
        NavItem(AppScreen.REPORTS, "Reports", Icons.Default.Assessment, "nav_reports")
    )

    val showBottomBar = currentScreen in listOf(
        AppScreen.DASHBOARD,
        AppScreen.TRANSACTIONS,
        AppScreen.SAVINGS,
        AppScreen.REPORTS
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    modifier = Modifier.testTag("bottom_nav_bar"),
                    tonalElevation = 3.dp
                ) {
                    navItems.forEach { item ->
                        val isSelected = currentScreen == item.screen
                        val isAddButton = item.screen == AppScreen.ADD_TRANSACTION

                        if (isAddButton) {
                            NavigationBarItem(
                                selected = false,
                                onClick = { viewModel.openAddTransaction() },
                                icon = {
                                    FloatingActionButton(
                                        onClick = { viewModel.openAddTransaction() },
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary,
                                        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 2.dp),
                                        shape = CircleShape,
                                        modifier = Modifier
                                            .size(42.dp)
                                            .testTag(item.testTag)
                                    ) {
                                        Icon(imageVector = Icons.Default.Add, contentDescription = "Add Transaction")
                                    }
                                },
                                label = { Text("Add", fontSize = 11.sp) }
                            )
                        } else {
                            NavigationBarItem(
                                selected = isSelected,
                                onClick = { viewModel.navigateTo(item.screen) },
                                icon = {
                                    Icon(
                                        imageVector = item.icon,
                                        contentDescription = item.title
                                    )
                                },
                                label = { Text(item.title, fontSize = 11.sp) },
                                modifier = Modifier.testTag(item.testTag)
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Crossfade(targetState = currentScreen, label = "screen_crossfade") { screen ->
                when (screen) {
                    AppScreen.DASHBOARD -> DashboardScreen(viewModel = viewModel)
                    AppScreen.TRANSACTIONS -> TransactionsScreen(viewModel = viewModel)
                    AppScreen.ADD_TRANSACTION -> AddTransactionScreen(viewModel = viewModel)
                    AppScreen.SAVINGS -> SavingsScreen(viewModel = viewModel)
                    AppScreen.ACCOUNT_DETAIL -> AccountDetailScreen(viewModel = viewModel)
                    AppScreen.BUDGET_PLANNER -> BudgetScreen(viewModel = viewModel)
                    AppScreen.REPORTS -> ReportsScreen(viewModel = viewModel)
                    AppScreen.SETTINGS -> SettingsScreen(viewModel = viewModel)
                }
            }
        }
    }
}
