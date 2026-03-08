package org.solodev.fleet.mngt.features.accounting

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Tab
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import org.solodev.fleet.mngt.navigation.AppRouter
import org.solodev.fleet.mngt.theme.fleetColors

private enum class AccountingTab(val label: String) {
    INVOICES("Invoices"),
    PAYMENTS("Payments"),
    ACCOUNTS("Chart of Accounts"),
}

@Composable
fun AccountingScreen(router: AppRouter) {
    val colors = fleetColors
    var selectedTab by remember { mutableStateOf(AccountingTab.INVOICES) }

    Column(Modifier.fillMaxSize()) {
        Text(
            "Accounting",
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            color = colors.onBackground,
        )

        PrimaryTabRow(
            selectedTabIndex = selectedTab.ordinal,
            containerColor = colors.surface,
            contentColor = colors.primary,
        ) {
            AccountingTab.entries.forEach { tab ->
                Tab(
                    selected = selectedTab == tab,
                    onClick = { selectedTab = tab },
                    text = { Text(tab.label, fontSize = 13.sp) },
                )
            }
        }

        when (selectedTab) {
            AccountingTab.INVOICES -> InvoicesTab(router = router)
            AccountingTab.PAYMENTS -> PaymentsTab()
            AccountingTab.ACCOUNTS -> AccountsTab()
        }
    }
}
