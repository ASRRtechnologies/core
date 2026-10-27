/* Copyright 2017-2026 ASRR B.V. */
package nl.asrr.core.text

import org.springframework.web.util.HtmlUtils
import java.nio.charset.StandardCharsets

/**
 * Encodes untrusted text for safe insertion into HTML content.
 */
fun escapeHtml(value: String?): String = HtmlUtils.htmlEscape(value.orEmpty(), StandardCharsets.UTF_8.name())

/**
 * Turns text rendered from [escapeHtml]-ed variables back into plain text. Mail subjects are not
 * HTML, so a subject built from the same escaped variables as the body would otherwise show
 * `&amp;` or `&#39;` to the recipient.
 */
fun plainTextSubject(value: String): String = HtmlUtils.htmlUnescape(value)
