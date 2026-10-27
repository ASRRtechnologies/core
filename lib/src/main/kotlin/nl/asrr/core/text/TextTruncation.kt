/* Copyright 2017-2026 ASRR B.V. */
package nl.asrr.core.text

import java.text.BreakIterator
import java.util.Locale

/**
 * Shortens [this] to at most [maxLength] UTF-16 code units, cutting only where a
 * user-perceived character ends.
 *
 * `take(n)` counts code UNITS, so it happily cuts a surrogate pair in half. The
 * lone surrogate that leaves behind has no UTF-8 encoding at all: the Mongo driver
 * writes it as CESU-8, and every strict UTF-8 reader afterwards — a pymongo cursor,
 * a JSON export — dies on the whole document rather than on the one field. A prod
 * Instagram conversation was unreadable that way, so no caller should be trusted to
 * remember the difference.
 *
 * Grapheme clusters, not just code points, because 💪🏻 is an emoji plus a separate
 * skin-tone modifier and a family emoji is three people joined by zero-width
 * joiners: cutting between them is valid UTF-8 but renders as a different emoji
 * than the one that was typed.
 *
 * The result can therefore be shorter than [maxLength], and text that already fits
 * still has a trailing lone surrogate stripped — the sender can hand us one it
 * truncated itself, and storing that breaks the same readers.
 */
fun String.truncateOnGraphemeBoundary(maxLength: Int): String {
    require(maxLength >= 0) { "maxLength must not be negative" }
    if (isEmpty()) return this

    val iterator = BreakIterator.getCharacterInstance(Locale.ROOT)
    iterator.setText(this)

    val limit = if (length <= maxLength) {
        length
    } else if (iterator.isBoundary(maxLength)) {
        maxLength
    } else {
        iterator.preceding(maxLength).takeIf { it != BreakIterator.DONE } ?: 0
    }

    // An unpaired surrogate is its own cluster, so a boundary can still sit right
    // after one that arrived broken from the sender.
    val end = if (limit > 0 && this[limit - 1].isSurrogate() && !(limit >= 2 && this[limit - 2].isHighSurrogate())) {
        limit - 1
    } else {
        limit
    }

    return if (end == length) this else substring(0, end)
}

/**
 * Splits [this] into consecutive pieces of at most [maxLength] UTF-16 code units,
 * breaking only between user-perceived characters.
 *
 * `chunked` cuts between code units for the same reason `take` does, so a payload
 * assembled from its pieces can carry a lone surrogate — Slack's Block Kit limits
 * are the case here, and an unencodable block rejects the whole message.
 *
 * A single cluster that is longer than [maxLength] on its own (a pathological ZWJ
 * chain) is cut on a code point boundary instead, so the walk always advances.
 */
fun String.chunkOnGraphemeBoundaries(maxLength: Int): List<String> {
    require(maxLength > 0) { "maxLength must be positive" }
    if (isEmpty()) return emptyList()
    if (length <= maxLength) return listOf(this)

    val iterator = BreakIterator.getCharacterInstance(Locale.ROOT)
    iterator.setText(this)

    val chunks = mutableListOf<String>()
    var start = 0
    while (start < length) {
        if (length - start <= maxLength) {
            chunks.add(substring(start))
            break
        }
        val cut = start + maxLength
        var end = if (iterator.isBoundary(cut)) cut else iterator.preceding(cut)
        if (end == BreakIterator.DONE || end <= start) {
            end = if (this[cut - 1].isHighSurrogate()) cut - 1 else cut
        }
        chunks.add(substring(start, end))
        start = end
    }
    return chunks
}
