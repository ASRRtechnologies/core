/* Copyright 2017-2026 ASRR B.V. */
package nl.asrr.core.generics.search

import java.lang.reflect.Modifier

/** A list as CSV, safe to open in a spreadsheet. */
object CsvExport {

    private val FORMULA_TRIGGERS = setOf('=', '+', '-', '@', '\t', '\r')

    /**
     * A cell that starts with `=`, `+`, `-`, `@`, tab or CR is executed as a formula by Excel,
     * LibreOffice and Sheets when the CSV is opened: exported user data (client names, notes) must
     * not become `=HYPERLINK(...)`/DDE payloads on a colleague's machine. Such a cell is prefixed with
     * a single quote, which spreadsheets render as text.
     *
     * A cell that parses as a number is left alone: `-` and `+` also start every negative amount and
     * every `+31…` phone number, and quoting those turns a numeric column into text, which silently
     * breaks sums on a financial export. A number cannot carry a formula.
     */
    fun neutralizeSpreadsheetFormula(value: String): String {
        if (value.isEmpty() || value[0] !in FORMULA_TRIGGERS) return value
        if (value.toBigDecimalOrNull() != null) return value
        return "'$value"
    }

    /** One header row of the field names of the first item (superclass fields first), then one fully quoted row per item. */
    fun toCsv(items: List<Any>): ByteArray {
        if (items.isEmpty()) return ByteArray(0)
        val fields = generateSequence(items.first().javaClass) { it.superclass.takeIf { s -> s != Any::class.java } }
            .toList()
            .asReversed()
            .flatMap { type -> type.declaredFields.filter { !Modifier.isStatic(it.modifiers) && !it.isSynthetic } }
            .distinctBy { it.name }
            .onEach { it.trySetAccessible() }
        val csv = StringBuilder()
        csv.appendLine(fields.joinToString(",") { it.name })
        for (item in items) {
            csv.appendLine(
                fields.joinToString(",") { field ->
                    val value = runCatching { field.get(item) }.getOrNull()?.toString()?.let(::neutralizeSpreadsheetFormula)?.replace("\"", "\"\"") ?: ""
                    "\"$value\""
                },
            )
        }
        return csv.toString().toByteArray(Charsets.UTF_8)
    }
}
