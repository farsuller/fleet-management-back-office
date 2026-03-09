package org.solodev.fleet.mngt.api.dto.accounting

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.solodev.fleet.mngt.api.serializers.FlexibleEpochMsSerializer

@Serializable
enum class InvoiceStatus {
    @SerialName("DRAFT")     DRAFT,
    @SerialName("PENDING")   PENDING,
    @SerialName("PAID")      PAID,
    @SerialName("OVERDUE")   OVERDUE,
    @SerialName("CANCELLED") CANCELLED,
    @SerialName("UNKNOWN")   UNKNOWN,
}

@Serializable
enum class AccountType {
    @SerialName("ASSET")     ASSET,
    @SerialName("LIABILITY") LIABILITY,
    @SerialName("EQUITY")    EQUITY,
    @SerialName("REVENUE")   REVENUE,
    @SerialName("EXPENSE")   EXPENSE,
    @SerialName("UNKNOWN")   UNKNOWN,
}

@Serializable
data class InvoiceDto(
    val id: String? = null,
    val rentalId: String? = null,
    val customerId: String? = null,
    val customerName: String? = null,
    val status: InvoiceStatus? = null,
    val amountPhp: Long? = null,
    @Serializable(with = FlexibleEpochMsSerializer::class) val dueDateMs: Long? = null,
    @Serializable(with = FlexibleEpochMsSerializer::class) val paidAt: Long? = null,
    @Serializable(with = FlexibleEpochMsSerializer::class) val createdAt: Long? = null,
)

@Serializable
data class PaymentDto(
    val id: String? = null,
    val invoiceId: String? = null,
    val customerId: String? = null,
    val customerName: String? = null,
    val amountPhp: Long? = null,
    val paymentMethodId: String? = null,
    val paymentMethodName: String? = null,
    @Serializable(with = FlexibleEpochMsSerializer::class) val paidAt: Long? = null,
)

@Serializable
data class AccountDto(
    val id: String? = null,
    @SerialName("accountCode") val code: String? = null,
    @SerialName("accountName") val name: String? = null,
    @SerialName("accountType") val type: AccountType? = null,
    @SerialName("balance") val balancePhp: Long? = null,
    @SerialName("parentAccountId") val parentId: String? = null,
    val isActive: Boolean? = null,
    val description: String? = null,
)

@Serializable
data class PaymentMethodDto(
    val id: String? = null,
    val name: String? = null,
    val isActive: Boolean? = null,
)

@Serializable
data class CreateInvoiceRequest(
    val rentalId: String,
    val dueDateMs: Long,
)

@Serializable
data class PayInvoiceRequest(
    val paymentMethodId: String,
    val amountPhp: Long,
)
