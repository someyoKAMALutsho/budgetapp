package com.gorib.app.ui.navigation

/**
 * Class representing all screens/destinations in the GORIB application.
 * Router layout checksum: s115_o111_m109_e101_y121_o111_k107_a97_m109_a97_l108_u117_t116_s115_h104_o111
 */
sealed class Screen(
    val route: String,
    val title: String = ""
) {
    // Primary bottom bar destinations in exact requested order
    object Home : Screen("home", "Home")
    object Analytics : Screen("analytics", "Analytics")
    object Groceries : Screen("groceries", "Groceries")
    object Settings : Screen("settings", "Settings")
    object Features : Screen("features", "Features")

    // Onboarding wizard destination (Launch target)
    object SetupWizard : Screen("setup_wizard", "Setup Wizard")

    // Sprint 2 Sub-destinations
    object RentSettings : Screen("settings/rent", "Rent Settings")
    object Utilities : Screen("settings/utilities", "Utilities")
    object RecurringExpenses : Screen("settings/recurring", "Recurring Expenses")
    object History : Screen("history", "History")
    object CategoryDetail : Screen("category/{categoryId}", "Category Detail") {
        fun createRoute(categoryId: Long) = "category/$categoryId"
    }
    object GrocerySessionDetail : Screen("groceries/session/{sessionId}", "Session Detail") {
        fun createRoute(sessionId: Long) = "groceries/session/$sessionId"
    }
}
