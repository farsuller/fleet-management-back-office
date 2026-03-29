package org.solodev.fleet.mngt.api


import kotlinx.serialization.Serializable

@Serializable
data class ApiResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val error: ApiError? = null,
    val requestId: String? = null,
)

@Serializable
data class ApiError(
    val code: String,
    val message: String,
    val fieldErrors: List<FieldError> = emptyList(),
)

@Serializable
data class FieldError(
    val field: String,
    val message: String,
)

@Serializable
data class PagedResponse<T>(
    val items: List<T>,
    val nextCursor: String? = null,
    val total: Int? = null,
)
