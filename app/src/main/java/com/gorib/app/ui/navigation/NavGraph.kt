package com.gorib.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.gorib.app.ui.screens.analytics.AnalyticsScreen
import com.gorib.app.ui.screens.analytics.AnalyticsViewModel
import com.gorib.app.ui.screens.category.CategoryDetailScreen
import com.gorib.app.ui.screens.category.CategoryDetailViewModel
import com.gorib.app.ui.screens.groceries.GroceriesScreen
import com.gorib.app.ui.screens.groceries.GroceriesViewModel
import com.gorib.app.ui.screens.groceries.SessionDetailScreen
import com.gorib.app.ui.screens.groceries.ReceiptReviewScreen
import com.gorib.app.ui.screens.groceries.SessionDetailViewModel
import com.gorib.app.ui.screens.history.HistoryScreen
import com.gorib.app.ui.screens.history.HistoryViewModel
import com.gorib.app.ui.screens.home.HomeScreen
import com.gorib.app.ui.screens.home.HomeViewModel
import com.gorib.app.ui.screens.settings.SettingsScreen
import com.gorib.app.ui.screens.settings.SettingsViewModel
import com.gorib.app.ui.screens.settings.rent.RentSettingsScreen
import com.gorib.app.ui.screens.settings.rent.RentSettingsViewModel
import com.gorib.app.ui.screens.settings.utilities.UtilitiesScreen
import com.gorib.app.ui.screens.settings.utilities.UtilitiesViewModel
import com.gorib.app.ui.screens.settings.recurring.RecurringExpensesScreen
import com.gorib.app.ui.screens.settings.recurring.RecurringExpensesViewModel
import com.gorib.app.ui.wizard.SetupWizardScreen
import com.gorib.app.ui.wizard.SetupWizardViewModel
import com.gorib.app.ui.screens.features.FeaturesScreen

/**
 * Main application navigation graph coordinating Compose routes.
 */
@Composable
fun NavGraph(
    navController: NavHostController,
    startDestination: String
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // 1. Setup Onboarding Wizard
        composable(Screen.SetupWizard.route) {
            val viewModel: SetupWizardViewModel = hiltViewModel()
            SetupWizardScreen(
                viewModel = viewModel,
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.SetupWizard.route) { inclusive = true }
                    }
                }
            )
        }

        // 2. Home Destination
        composable(Screen.Home.route) {
            val viewModel: HomeViewModel = hiltViewModel()
            HomeScreen(
                viewModel = viewModel,
                onNavigateToCategory = { categoryId ->
                    navController.navigate(Screen.CategoryDetail.createRoute(categoryId))
                },
                onNavigateToRentSettings = {
                    navController.navigate(Screen.RentSettings.route)
                },
                onNavigateToUtilities = {
                    navController.navigate(Screen.Utilities.route)
                },
                onNavigateToHistory = {
                    navController.navigate(Screen.History.route)
                },
                onNavigateToReceiptReview = {
                    navController.navigate("groceries/receipt-review")
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                }
            )
        }

        // 3. Analytics Destination
        composable(Screen.Analytics.route) {
            AnalyticsScreen(navController = navController)
        }

        // 4. Groceries Destination
        composable(Screen.Groceries.route) {
            val viewModel: GroceriesViewModel = hiltViewModel()
            GroceriesScreen(
                viewModel = viewModel,
                onNavigateToSessionDetail = { sessionId ->
                    navController.navigate(Screen.GrocerySessionDetail.createRoute(sessionId))
                },
                onNavigateToReceiptReview = {
                    navController.navigate("groceries/receipt-review")
                }
            )
        }

        // 4b. Grocery Session Detail Destination
        composable(
            route = Screen.GrocerySessionDetail.route,
            arguments = listOf(navArgument("sessionId") { type = NavType.LongType })
        ) {
            val viewModel: SessionDetailViewModel = hiltViewModel()
            SessionDetailScreen(
                viewModel = viewModel,
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }

        // 5. Settings Destination
        composable(Screen.Settings.route) {
            val viewModel: SettingsViewModel = hiltViewModel()
            SettingsScreen(
                viewModel = viewModel,
                onNavigateToRentSettings = {
                    navController.navigate(Screen.RentSettings.route)
                },
                onNavigateToUtilities = {
                    navController.navigate(Screen.Utilities.route)
                },
                onNavigateToRecurringExpenses = {
                    navController.navigate(Screen.RecurringExpenses.route)
                }
            )
        }

        // 5b. Recurring Expenses Sub-Destination
        composable(Screen.RecurringExpenses.route) {
            val viewModel: RecurringExpensesViewModel = hiltViewModel()
            RecurringExpensesScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // 6. Rent Settings Sub-Destination
        composable(Screen.RentSettings.route) {
            val viewModel: RentSettingsViewModel = hiltViewModel()
            RentSettingsScreen(
                viewModel = viewModel,
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }

        // 7. Utilities Module Sub-Destination
        composable(Screen.Utilities.route) {
            val viewModel: UtilitiesViewModel = hiltViewModel()
            UtilitiesScreen(
                viewModel = viewModel,
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }

        // 8. Category Detail Sub-Destination
        composable(
            route = Screen.CategoryDetail.route,
            arguments = listOf(navArgument("categoryId") { type = NavType.LongType })
        ) {
            val viewModel: CategoryDetailViewModel = hiltViewModel()
            CategoryDetailScreen(
                viewModel = viewModel,
                onBackClick = {
                    navController.popBackStack()
                },
                onNavigateToReceiptReview = {
                    navController.navigate("groceries/receipt-review")
                }
            )
        }

        // 9. History Sub-Destination (decoupled from primary tabs)
        composable(Screen.History.route) {
            val viewModel: HistoryViewModel = hiltViewModel()
            HistoryScreen(
                viewModel = viewModel,
                onBackClick = {
                    navController.popBackStack()
                },
                onNavigateToReceiptReview = {
                    navController.navigate("groceries/receipt-review")
                }
            )
        }

        // 10. Receipt Review Destination
        composable("groceries/receipt-review") {
            ReceiptReviewScreen(navController = navController)
        }

        // 11. Features Destination
        composable(Screen.Features.route) {
            FeaturesScreen(navController = navController)
        }
    }
}
