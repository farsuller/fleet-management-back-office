package org.solodev.fleet.mngt.domain.model

data class FinancialSummary(
    val totalAssetsPhp: Long,
    val totalRevenuePhp: Long,
    val cashBalancePhp: Long,
    val accountsReceivablePhp: Long,
)
