@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package app.injaz.starter.ui.more

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.injaz.starter.R
import app.injaz.starter.ui.kit.AppTopBar
import app.injaz.starter.ui.kit.ListRow

/** The fifth tab: the two places that are not part of the daily loop. */
@Composable
fun MoreScreen(onOpenProducts: () -> Unit, onOpenSettings: () -> Unit) {
    Scaffold(topBar = { AppTopBar(title = stringResource(R.string.more_title)) }) { padding ->
        Column(Modifier.padding(padding)) {
            ListRow(
                title = stringResource(R.string.more_products),
                supporting = stringResource(R.string.more_products_hint),
                leadingIcon = Icons.Filled.ShoppingCart,
                onClick = onOpenProducts,
                trailing = { Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null) },
            )
            ListRow(
                title = stringResource(R.string.more_settings),
                supporting = stringResource(R.string.more_settings_hint),
                leadingIcon = Icons.Filled.Settings,
                onClick = onOpenSettings,
                trailing = { Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null) },
            )
        }
    }
}
