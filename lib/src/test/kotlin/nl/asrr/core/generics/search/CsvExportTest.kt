/* Copyright 2017-2026 ASRR B.V. */
package nl.asrr.core.generics.search

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class CsvExportTest {

    @Test
    fun `formula cells are quoted, numbers and phone numbers are not`() {
        assertEquals("'=HYPERLINK(\"x\")", CsvExport.neutralizeSpreadsheetFormula("=HYPERLINK(\"x\")"))
        assertEquals("'@SUM(A1)", CsvExport.neutralizeSpreadsheetFormula("@SUM(A1)"))
        assertEquals("-12.50", CsvExport.neutralizeSpreadsheetFormula("-12.50"))
        assertEquals("+31612345678", CsvExport.neutralizeSpreadsheetFormula("+31612345678"))
        assertEquals("Jansen", CsvExport.neutralizeSpreadsheetFormula("Jansen"))
    }

    data class Row(val name: String, val note: String?)

    @Test
    fun `rows are fully quoted with escaped quotes`() {
        val csv = CsvExport.toCsv(listOf(Row("Jan \"JJ\"", "=1+1"), Row("Piet", null))).toString(Charsets.UTF_8)
        assertEquals("name,note\n\"Jan \"\"JJ\"\"\",\"'=1+1\"\n\"Piet\",\"\"\n", csv)
    }
}
