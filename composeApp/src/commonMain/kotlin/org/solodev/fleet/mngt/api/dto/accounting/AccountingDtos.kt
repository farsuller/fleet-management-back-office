package org.solodev.fleet.mngt.api.dto.accounting

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.solodev.fleet.mngt.api.serializers.FlexibleEpochMsSerializer

@Serializable
enum class InvoiceStatus {
    @SerialName("DRAFT")
    DRAFT,

    @SerialName("ISSUED")
    ISSUED,

    @SerialName("PENDING")
    PENDING,

    @SerialName("PAID")
    PAID,

    @SerialName("OVERDUE")
    OVERDUE,

    @SerialName("CANCELLED")
    CANCELLED,

    @SerialName("UNKNOWN")
    UNKNOWN,
}

@Serializable
enum class AccountType {
    @SerialName("ASSET")
    ASSET,

    @SerialName("LIABILITY")
    LIABILITY,

    @SerialName("EQUITY")
    EQUITY,

    @SerialName("REVENUE")
    REVENUE,

    @SerialName("EXPENSE")
    EXPENSE,

    @SerialName("UNKNOWN")
    UNKNOWN,
}

@Serializable
enum class CollectionType {
    @SerialName("DIRECT")
    DIRECT,

    @SerialName("DRIVER_COLLECTED")
    DRIVER_COLLECTED,

    @SerialName("UNKNOWN")
    UNKNOWN,
}

@Serializable
enum class RemittanceStatus {
    @SerialName("PENDING")
    PENDING,

    @SerialName("SUBMITTED")
    SUBMITTED,

    @SerialName("VERIFIED")
    VERIFIED,

    @SerialName("DISCREPANCY")
    DISCREPANCY,

    @SerialName("UNKNOWN")
    UNKNOWN,
}

@Serializable
data class CustomerSummaryDto(
    val id: String? = null,
    val fullName: String? = null,
    val email: String? = null,
    val phoneNumber: String? = null,
)

@Serializable
data class InvoiceDto(
    val id: String? = null,
    val invoiceNumber: String? = null,
    val rentalId: String? = null,
    val customer: CustomerSummaryDto? = null,
    val status: InvoiceStatus? = null,
    val subtotal: Long? = null,
    val tax: Long? = null,
    val total: Long? = null,
    val paidAmount: Long? = null,
    val balance: Long? = null,
    val currencyCode: String? = null,
    @Serializable(with = FlexibleEpochMsSerializer::class) val issueDate: Long? = null,
    @Serializable(with = FlexibleEpochMsSerializer::class) val dueDate: Long? = null,
    @Serializable(with = FlexibleEpochMsSerializer::class) val paidDate: Long? = null,
    val notes: String? = null,
)

@Serializable
data class PaymentDto(
    val id: String? = null,
    val paymentNumber: String? = null,
    val invoiceId: String? = null,
    val customerId: String? = null,
    val driverId: String? = null,
    val amount: Long? = null,
    val paymentMethod: String? = null,
    val transactionReference: String? = null,
    val status: String? = null,
    val collectionType: CollectionType? = null,
    @Serializable(with = FlexibleEpochMsSerializer::class) val paymentDate: Long? = null,
    val notes: String? = null,
)

@Serializable
data class DriverRemittanceDto(
    val id: String? = null,
    val remittanceNumber: String? = null,
    val driverId: String? = null,
    val totalAmount: Long? = null,
    val status: RemittanceStatus? = null,
    val paymentIds: List<String> = emptyList(),
    @Serializable(with = FlexibleEpochMsSerializer::class) val remittanceDate: Long? = null,
    val notes: String? = null,
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
    val customerId: String,
    val rentalId: String? = null,
    val subtotal: Long,
    val tax: Long = 0,
    val dueDate: String,
)

@Serializable
data class PayInvoiceRequest(
    val amount: Long,
    val paymentMethod: String,
    val notes: String? = null,
    val transactionReference: String? = null,
)

@Serializable
data class DriverCollectionRequest(
    val driverId: String,
    val customerId: String,
    val invoiceId: String,
    val amount: Long,
    val paymentMethod: String,
    val transactionReference: String? = null,
    val collectedAt: String? = null,
)

@Serializable
data class DriverRemittanceRequest(
    val driverId: String,
    val paymentIds: List<String>,
    val remittanceDate: String? = null,
    val notes: String? = null,
)
