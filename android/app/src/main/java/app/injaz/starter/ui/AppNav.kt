package app.injaz.starter.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import app.injaz.starter.R
import app.injaz.starter.ui.customers.CustomerDetailScreen
import app.injaz.starter.ui.customers.CustomersScreen
import app.injaz.starter.ui.home.HomeScreen
import app.injaz.starter.ui.more.MoreScreen
import app.injaz.starter.ui.products.ProductsScreen
import app.injaz.starter.ui.products.StockLogScreen
import app.injaz.starter.ui.reports.ReportsScreen
import app.injaz.starter.ui.search.SearchScreen
import app.injaz.starter.ui.sales.NewSaleScreen
import app.injaz.starter.ui.sales.SalesScreen
import app.injaz.starter.ui.settings.SettingsScreen

private object Routes {
    const val HOME = "home"
    const val SALES = "sales"
    const val CUSTOMERS = "customers"
    const val REPORTS = "reports"
    const val MORE = "more"
    const val NEW_SALE = "new_sale"
    const val CUSTOMER = "customer/{id}"
    const val PRODUCTS = "products"
    const val STOCK = "stock/{id}"
    const val SETTINGS = "settings"
    const val SEARCH = "search"
}

private data class Tab(val route: String, val label: Int, val icon: ImageVector)

private val tabs = listOf(
    Tab(Routes.HOME, R.string.nav_home, Icons.Filled.Home),
    Tab(Routes.SALES, R.string.nav_sales, Icons.AutoMirrored.Filled.List),
    Tab(Routes.CUSTOMERS, R.string.nav_customers, Icons.Filled.Person),
    Tab(Routes.REPORTS, R.string.nav_reports, Icons.Filled.DateRange),
    Tab(Routes.MORE, R.string.nav_more, Icons.Filled.Menu),
)

/**
 * The app's navigation: five tabs in a bottom bar, and detail screens that cover it.
 * The bar's height is consumed as insets, so each screen's own Scaffold does not add the system bar twice.
 */
@Composable
fun AppNav() {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    val showBar = tabs.any { it.route == route }

    fun openTab(target: String) {
        nav.navigate(target) {
            popUpTo(nav.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (showBar) {
                NavigationBar {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = route == tab.route,
                            onClick = { openTab(tab.route) },
                            icon = { Icon(tab.icon, contentDescription = null) },
                            label = { Text(stringResource(tab.label)) },
                        )
                    }
                }
            }
        },
    ) { inner ->
        NavHost(
            navController = nav,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(inner).consumeWindowInsets(inner),
        ) {
            composable(Routes.HOME) {
                HomeScreen(
                    onNewSale = { nav.navigate(Routes.NEW_SALE) },
                    onOpenProducts = { nav.navigate(Routes.PRODUCTS) },
                    onOpenCustomers = { openTab(Routes.CUSTOMERS) },
                    onOpenSales = { openTab(Routes.SALES) },
                    onOpenSearch = { nav.navigate(Routes.SEARCH) },
                )
            }
            composable(Routes.SALES) {
                SalesScreen(onNewSale = { nav.navigate(Routes.NEW_SALE) }, onOpenSearch = { nav.navigate(Routes.SEARCH) })
            }
            composable(Routes.CUSTOMERS) {
                CustomersScreen(
                    onOpenCustomer = { id -> nav.navigate("customer/$id") },
                    onOpenSearch = { nav.navigate(Routes.SEARCH) },
                )
            }
            composable(Routes.REPORTS) { ReportsScreen() }
            composable(Routes.MORE) {
                MoreScreen(
                    onOpenProducts = { nav.navigate(Routes.PRODUCTS) },
                    onOpenSettings = { nav.navigate(Routes.SETTINGS) },
                )
            }
            composable(Routes.NEW_SALE) { NewSaleScreen(onBack = { nav.popBackStack() }) }
            composable(
                Routes.CUSTOMER,
                arguments = listOf(navArgument("id") { type = NavType.StringType }),
            ) { e ->
                CustomerDetailScreen(customerId = e.arguments?.getString("id").orEmpty(), onBack = { nav.popBackStack() })
            }
            composable(Routes.PRODUCTS) {
                ProductsScreen(onBack = { nav.popBackStack() }, onOpenStock = { id -> nav.navigate("stock/$id") })
            }
            composable(
                Routes.STOCK,
                arguments = listOf(navArgument("id") { type = NavType.StringType }),
            ) { e ->
                StockLogScreen(productId = e.arguments?.getString("id").orEmpty(), onBack = { nav.popBackStack() })
            }
            composable(Routes.SETTINGS) { SettingsScreen(onBack = { nav.popBackStack() }) }
            composable(Routes.SEARCH) {
                SearchScreen(
                    onBack = { nav.popBackStack() },
                    onOpenCustomer = { id -> nav.navigate("customer/$id") },
                    onOpenStock = { id -> nav.navigate("stock/$id") },
                )
            }
        }
    }
}
