package nl.asrr.core.generics.dto

import org.springframework.data.domain.Page

/**
 * Stable JSON shape for paginated responses. Avoids leaking Spring `Page` internals
 * (e.g. `pageable`, `sort`) which are unstable across Spring releases and verbose to consume.
 */
data class PageResponse<T>(
    val items: List<T>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
    val first: Boolean,
    val last: Boolean,
)

fun <T> Page<T>.toResponse(): PageResponse<T> = PageResponse(
    items = content,
    page = number,
    size = size,
    totalElements = totalElements,
    totalPages = totalPages,
    first = isFirst,
    last = isLast,
)
