package org.solodev.fleet.mngt.features.accounting

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.solodev.fleet.mngt.navigation.AppRouter
import org.solodev.fleet.mngt.theme.fleetColors

private enum class AccountingTab(val label: String) {
    INVOICES("Invoices"),
    PAYMENTS("Payments"),
    FLOWS("Flows"),
    DRIVER_PAYMENTS("Driver Payments"),
    REMITTANCES("Remittances"),
    ACCOUNTS("Chart of Accounts"),
}

@Composable
fun AccountingScreen(router: AppRouter) {
    val colors = fleetColors
    var selectedTab by remember { mutableStateOf(AccountingTab.INVOICES) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
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
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
        ) {
            when (selectedTab) {
                AccountingTab.INVOICES -> InvoicesTab(router = router)
                AccountingTab.PAYMENTS -> PaymentsTab()
                AccountingTab.FLOWS -> FlowsTab()
                AccountingTab.DRIVER_PAYMENTS -> DriverPaymentsTab()
                AccountingTab.REMITTANCES -> RemittancesTab()
                AccountingTab.ACCOUNTS -> AccountsTab()
            }
        }
    }
}
