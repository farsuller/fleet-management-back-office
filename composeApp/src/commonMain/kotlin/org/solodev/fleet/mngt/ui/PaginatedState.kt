package org.solodev.fleet.mngt.ui

data class PaginatedState<T>(
    val items: List<T> = emptyList(),
    val nextCursor: String? = null,
    val isLoadingMore: Boolean = false,
    val hasMore: Boolean = true,
)
