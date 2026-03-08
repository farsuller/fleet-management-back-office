package org.solodev.fleet.mngt.features.accounting

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.koin.compose.viewmodel.koinViewModel
import org.solodev.fleet.mngt.api.dto.accounting.AccountType
import org.solodev.fleet.mngt.components.common.TableSkeleton
import org.solodev.fleet.mngt.theme.fleetColors
import org.solodev.fleet.mngt.ui.UiState

/** Formats centavo amount to ₱X,XXX.XX without String.format (unavailable in Kotlin/Wasm). */
private fun formatPhp(centavos: Long): String {
    val negative = centavos < 0
    val abs = if (negative) -centavos else centavos
    val pesos = abs / 100
    val cents = abs % 100
    val centsStr = cents.toString().padStart(2, '0')
    val pesosStr = pesos.toString()
    val withCommas = buildString {
        pesosStr.forEachIndexed { i, c ->
            val remaining = pesosStr.length - i
            if (i > 0 && remaining % 3 == 0) append(',')
            append(c)
        }
    }
    return "${if (negative) "-" else ""}\u20b1 $withCommas.$centsStr"
}

@Composable
fun AccountsTab() {
    val vm = koinViewModel<AccountsViewModel>()
    val state by vm.uiState.collectAsState()
    val isRefreshing by vm.isRefreshing.collectAsState()
    val colors = fleetColors

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            if (isRefreshing) CircularProgressIndicator(
                modifier = Modifier.width(20.dp).height(20.dp),
                strokeWidth = 2.dp,
            )
        }

        when (val s = state) {
            is UiState.Loading -> TableSkeleton(rows = 10)
            is UiState.Error -> Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(s.message, color = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(8.dp))
                Button(onClick = vm::refresh) { Text("Retry") }
            }
            is UiState.Success -> {
                val byType = s.data.groupBy { it.type ?: AccountType.UNKNOWN }
                val typeOrder = listOf(
                    AccountType.ASSET,
                    AccountType.LIABILITY,
                    AccountType.EQUITY,
                    AccountType.REVENUE,
                    AccountType.EXPENSE,
                    AccountType.UNKNOWN,
                )
                val expandedTypes = remember {
                    mutableStateMapOf<AccountType, Boolean>().also { map ->
                        typeOrder.forEach { map[it] = true }
                    }
                }

                // Table header row
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("Code", modifier = Modifier.width(80.dp), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = colors.text2)
                    Text("Account Name", modifier = Modifier.weight(2f), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = colors.text2)
                    Text("Type", modifier = Modifier.weight(1f), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = colors.text2)
                    Text("Balance (₱)", modifier = Modifier.weight(1f), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = colors.text2)
                }

                typeOrder.forEach { type ->
                    val accounts = byType[type] ?: return@forEach
                    val expanded = expandedTypes[type] != false

                    // Clickable section header
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { expandedTypes[type] = !expanded }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            type.name,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.primary,
                            letterSpacing = 0.5.sp,
                        )
                        Icon(
                            imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                            contentDescription = if (expanded) "Collapse" else "Expand",
                            tint = colors.primary,
                        )
                    }

                    if (expanded) {
                    accounts
                        .sortedBy { it.code ?: "" }
                        .forEach { account ->
                            val balance = account.balancePhp ?: 0L
                            val isNegative = balance < 0
                            Row(
                                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text(
                                    account.code ?: "—",
                                    modifier = Modifier.width(80.dp),
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = colors.text2,
                                )
                                Text(
                                    account.name ?: "—",
                                    modifier = Modifier.weight(2f),
                                    fontSize = 13.sp,
                                    color = colors.text1,
                                )
                                Text(
                                    account.type?.name ?: "—",
                                    modifier = Modifier.weight(1f),
                                    fontSize = 12.sp,
                                    color = colors.text2,
                                )
                                Text(
                                    formatPhp(balance),
                                    modifier = Modifier.weight(1f),
                                    fontSize = 13.sp,
                                    color = if (isNegative) MaterialTheme.colorScheme.error else colors.text1,
                                    fontWeight = if (isNegative) FontWeight.Medium else FontWeight.Normal,
                                )
                            }
                        }
                    } // end if (expanded)

                    if (expanded) Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}
