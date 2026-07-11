package io.tolgee.unit.xlsx.out

import io.tolgee.component.CurrentDateProvider
import io.tolgee.dtos.request.export.ExportParams
import io.tolgee.formats.ExportFormat
import io.tolgee.formats.xlsx.out.XlsxFileExporter
import io.tolgee.service.export.ExportFilePathProvider
import io.tolgee.service.export.ExportFileStructureTemplateProvider
import io.tolgee.service.export.dataProvider.ExportTranslationView
import io.tolgee.unit.util.assertFile
import io.tolgee.unit.util.getExportedCompressed
import io.tolgee.util.buildExportTranslationList
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import java.util.Calendar
import java.util.Date

class XlsxFileExporterTest {
  private val currentDateProvider = Mockito.mock(CurrentDateProvider::class.java)

  @BeforeEach
  fun setup() {
    val now = Date(Date.UTC(2025 - 1900, Calendar.JANUARY, 10, 0, 0, 0))
    Mockito.`when`(currentDateProvider.date).thenReturn(now)
  }

  @AfterEach
  fun teardown() {
    Mockito.reset(currentDateProvider)
  }

  @Test
  fun `exports with placeholders (ICU placeholders disabled)`() {
    val exporter = getIcuPlaceholdersDisabledExporter()
    val data = getExportedCompressed(exporter)
    data.assertFile(
      "all.xlsx",
      """
    |====================
    |[Content_Types].xml
    |--------------------
    |<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types"><Default ContentType="application/vnd.openxmlformats-package.relationships+xml" Extension="rels"/><Default ContentType="application/xml" Extension="xml"/><Override ContentType="application/vnd.openxmlformats-officedocument.extended-properties+xml" PartName="/docProps/app.xml"/><Override ContentType="application/vnd.openxmlformats-package.core-properties+xml" PartName="/docProps/core.xml"/><Override ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sharedStrings+xml" PartName="/xl/sharedStrings.xml"/><Override ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml" PartName="/xl/styles.xml"/><Override ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml" PartName="/xl/workbook.xml"/><Override ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml" PartName="/xl/worksheets/sheet1.xml"/></Types>
    |====================
    |_rels/.rels
    |--------------------
    |<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Target="xl/workbook.xml" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument"/><Relationship Id="rId2" Target="docProps/app.xml" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/extended-properties"/><Relationship Id="rId3" Target="docProps/core.xml" Type="http://schemas.openxmlformats.org/package/2006/relationships/metadata/core-properties"/></Relationships>
    |====================
    |docProps/app.xml
    |--------------------
    |<?xml version="1.0" encoding="UTF-8"?>
    |<Properties xmlns="http://schemas.openxmlformats.org/officeDocument/2006/extended-properties"><Application>Apache POI</Application></Properties>
    |====================
    |docProps/core.xml
    |--------------------
    |<?xml version="1.0" encoding="UTF-8" standalone="yes"?><cp:coreProperties xmlns:cp="http://schemas.openxmlformats.org/package/2006/metadata/core-properties" xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns:dcterms="http://purl.org/dc/terms/" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"><dcterms:created xsi:type="dcterms:W3CDTF">2025-01-10T00:00:00Z</dcterms:created><dc:creator>Apache POI</dc:creator></cp:coreProperties>
    |====================
    |xl/sharedStrings.xml
    |--------------------
    |<?xml version="1.0" encoding="UTF-8"?>
    |<sst count="8" uniqueCount="7" xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><si><t>key</t></si><si><t>cs</t></si><si><t>key3</t></si><si><t>{count, plural, one {# den {icuParam}} few {# dny} other {# dní}}</t></si><si><t>item</t></si><si><t>I will be first {icuParam, number}</t></si><si><t xml:space="preserve">Text with multiple lines
    |and , commas and "quotes" </t></si></sst>
    |====================
    |xl/styles.xml
    |--------------------
    |<?xml version="1.0" encoding="UTF-8"?>
    |<styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><numFmts count="0"/><fonts count="1"><font><sz val="11.0"/><color indexed="8"/><name val="Calibri"/><family val="2"/><scheme val="minor"/></font></fonts><fills count="2"><fill><patternFill patternType="none"/></fill><fill><patternFill patternType="darkGray"/></fill></fills><borders count="1"><border><left/><right/><top/><bottom/><diagonal/></border></borders><cellStyleXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/></cellStyleXfs><cellXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/></cellXfs></styleSheet>
    |====================
    |xl/workbook.xml
    |--------------------
    |<?xml version="1.0" encoding="UTF-8"?>
    |<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"><workbookPr date1904="false"/><bookViews><workbookView activeTab="0"/></bookViews><sheets><sheet name="Sheet0" r:id="rId3" sheetId="1"/></sheets></workbook>
    |====================
    |xl/_rels/workbook.xml.rels
    |--------------------
    |<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Target="sharedStrings.xml" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/sharedStrings"/><Relationship Id="rId2" Target="styles.xml" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles"/><Relationship Id="rId3" Target="worksheets/sheet1.xml" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet"/></Relationships>
    |====================
    |xl/worksheets/sheet1.xml
    |--------------------
    |<?xml version="1.0" encoding="UTF-8"?>
    |<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><dimension ref="A1:B4"/><sheetViews><sheetView workbookViewId="0" tabSelected="true"/></sheetViews><sheetFormatPr defaultRowHeight="15.0"/><sheetData><row r="1"><c r="A1" t="s" s="0"><v>0</v></c><c r="B1" t="s" s="0"><v>1</v></c></row><row r="2"><c r="A2" t="s" s="0"><v>2</v></c><c r="B2" t="s" s="0"><v>3</v></c></row><row r="3"><c r="A3" t="s" s="0"><v>4</v></c><c r="B3" t="s" s="0"><v>5</v></c></row><row r="4"><c r="A4" t="s" s="0"><v>0</v></c><c r="B4" t="s" s="0"><v>6</v></c></row></sheetData><pageMargins bottom="0.75" footer="0.3" header="0.3" left="0.7" right="0.7" top="0.75"/></worksheet>
    |
      """.trimMargin(),
    )
  }

  private fun getIcuPlaceholdersDisabledExporter(): XlsxFileExporter {
    val built =
      buildExportTranslationList {
        add(
          languageTag = "cs",
          keyName = "key3",
          text = "{count, plural, one {'#' den '{'icuParam'}'} few {'#' dny} other {'#' dní}}",
        ) {
          key.isPlural = true
        }
        add(
          languageTag = "cs",
          keyName = "item",
          text = "I will be first {icuParam, number}",
        )
        add(
          languageTag = "cs",
          keyName = "key",
          text = "Text with multiple lines\nand , commas and \"quotes\" ",
        )
      }
    return getExporter(built.translations, false)
  }

  @Test
  fun `exports with placeholders (ICU placeholders enabled)`() {
    val exporter = getIcuPlaceholdersEnabledExporter()
    val data = getExportedCompressed(exporter)
    data.assertFile(
      "all.xlsx",
      """
    |====================
    |[Content_Types].xml
    |--------------------
    |<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types"><Default ContentType="application/vnd.openxmlformats-package.relationships+xml" Extension="rels"/><Default ContentType="application/xml" Extension="xml"/><Override ContentType="application/vnd.openxmlformats-officedocument.extended-properties+xml" PartName="/docProps/app.xml"/><Override ContentType="application/vnd.openxmlformats-package.core-properties+xml" PartName="/docProps/core.xml"/><Override ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sharedStrings+xml" PartName="/xl/sharedStrings.xml"/><Override ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml" PartName="/xl/styles.xml"/><Override ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml" PartName="/xl/workbook.xml"/><Override ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml" PartName="/xl/worksheets/sheet1.xml"/></Types>
    |====================
    |_rels/.rels
    |--------------------
    |<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Target="xl/workbook.xml" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument"/><Relationship Id="rId2" Target="docProps/app.xml" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/extended-properties"/><Relationship Id="rId3" Target="docProps/core.xml" Type="http://schemas.openxmlformats.org/package/2006/relationships/metadata/core-properties"/></Relationships>
    |====================
    |docProps/app.xml
    |--------------------
    |<?xml version="1.0" encoding="UTF-8"?>
    |<Properties xmlns="http://schemas.openxmlformats.org/officeDocument/2006/extended-properties"><Application>Apache POI</Application></Properties>
    |====================
    |docProps/core.xml
    |--------------------
    |<?xml version="1.0" encoding="UTF-8" standalone="yes"?><cp:coreProperties xmlns:cp="http://schemas.openxmlformats.org/package/2006/metadata/core-properties" xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns:dcterms="http://purl.org/dc/terms/" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"><dcterms:created xsi:type="dcterms:W3CDTF">2025-01-10T00:00:00Z</dcterms:created><dc:creator>Apache POI</dc:creator></cp:coreProperties>
    |====================
    |xl/sharedStrings.xml
    |--------------------
    |<?xml version="1.0" encoding="UTF-8"?>
    |<sst count="6" uniqueCount="6" xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><si><t>key</t></si><si><t>cs</t></si><si><t>key3</t></si><si><t>{count, plural, one {# den {icuParam, number}} few {# dny} other {# dní}}</t></si><si><t>item</t></si><si><t>I will be first '{'icuParam'}' {hello, number}</t></si></sst>
    |====================
    |xl/styles.xml
    |--------------------
    |<?xml version="1.0" encoding="UTF-8"?>
    |<styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><numFmts count="0"/><fonts count="1"><font><sz val="11.0"/><color indexed="8"/><name val="Calibri"/><family val="2"/><scheme val="minor"/></font></fonts><fills count="2"><fill><patternFill patternType="none"/></fill><fill><patternFill patternType="darkGray"/></fill></fills><borders count="1"><border><left/><right/><top/><bottom/><diagonal/></border></borders><cellStyleXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/></cellStyleXfs><cellXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/></cellXfs></styleSheet>
    |====================
    |xl/workbook.xml
    |--------------------
    |<?xml version="1.0" encoding="UTF-8"?>
    |<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"><workbookPr date1904="false"/><bookViews><workbookView activeTab="0"/></bookViews><sheets><sheet name="Sheet0" r:id="rId3" sheetId="1"/></sheets></workbook>
    |====================
    |xl/_rels/workbook.xml.rels
    |--------------------
    |<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Target="sharedStrings.xml" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/sharedStrings"/><Relationship Id="rId2" Target="styles.xml" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles"/><Relationship Id="rId3" Target="worksheets/sheet1.xml" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet"/></Relationships>
    |====================
    |xl/worksheets/sheet1.xml
    |--------------------
    |<?xml version="1.0" encoding="UTF-8"?>
    |<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><dimension ref="A1:B3"/><sheetViews><sheetView workbookViewId="0" tabSelected="true"/></sheetViews><sheetFormatPr defaultRowHeight="15.0"/><sheetData><row r="1"><c r="A1" t="s" s="0"><v>0</v></c><c r="B1" t="s" s="0"><v>1</v></c></row><row r="2"><c r="A2" t="s" s="0"><v>2</v></c><c r="B2" t="s" s="0"><v>3</v></c></row><row r="3"><c r="A3" t="s" s="0"><v>4</v></c><c r="B3" t="s" s="0"><v>5</v></c></row></sheetData><pageMargins bottom="0.75" footer="0.3" header="0.3" left="0.7" right="0.7" top="0.75"/></worksheet>
    |
      """.trimMargin(),
    )
  }

  @Test
  fun `correct exports translation with colon`() {
    val exporter = getExporter(getTranslationWithColon())
    val data = getExportedCompressed(exporter)
    data.assertFile(
      "all.xlsx",
      """
    |====================
    |[Content_Types].xml
    |--------------------
    |<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types"><Default ContentType="application/vnd.openxmlformats-package.relationships+xml" Extension="rels"/><Default ContentType="application/xml" Extension="xml"/><Override ContentType="application/vnd.openxmlformats-officedocument.extended-properties+xml" PartName="/docProps/app.xml"/><Override ContentType="application/vnd.openxmlformats-package.core-properties+xml" PartName="/docProps/core.xml"/><Override ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sharedStrings+xml" PartName="/xl/sharedStrings.xml"/><Override ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml" PartName="/xl/styles.xml"/><Override ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml" PartName="/xl/workbook.xml"/><Override ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml" PartName="/xl/worksheets/sheet1.xml"/></Types>
    |====================
    |_rels/.rels
    |--------------------
    |<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Target="xl/workbook.xml" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument"/><Relationship Id="rId2" Target="docProps/app.xml" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/extended-properties"/><Relationship Id="rId3" Target="docProps/core.xml" Type="http://schemas.openxmlformats.org/package/2006/relationships/metadata/core-properties"/></Relationships>
    |====================
    |docProps/app.xml
    |--------------------
    |<?xml version="1.0" encoding="UTF-8"?>
    |<Properties xmlns="http://schemas.openxmlformats.org/officeDocument/2006/extended-properties"><Application>Apache POI</Application></Properties>
    |====================
    |docProps/core.xml
    |--------------------
    |<?xml version="1.0" encoding="UTF-8" standalone="yes"?><cp:coreProperties xmlns:cp="http://schemas.openxmlformats.org/package/2006/metadata/core-properties" xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns:dcterms="http://purl.org/dc/terms/" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"><dcterms:created xsi:type="dcterms:W3CDTF">2025-01-10T00:00:00Z</dcterms:created><dc:creator>Apache POI</dc:creator></cp:coreProperties>
    |====================
    |xl/sharedStrings.xml
    |--------------------
    |<?xml version="1.0" encoding="UTF-8"?>
    |<sst count="4" uniqueCount="4" xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><si><t>key</t></si><si><t>cs</t></si><si><t>item</t></si><si><t>name : {name}</t></si></sst>
    |====================
    |xl/styles.xml
    |--------------------
    |<?xml version="1.0" encoding="UTF-8"?>
    |<styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><numFmts count="0"/><fonts count="1"><font><sz val="11.0"/><color indexed="8"/><name val="Calibri"/><family val="2"/><scheme val="minor"/></font></fonts><fills count="2"><fill><patternFill patternType="none"/></fill><fill><patternFill patternType="darkGray"/></fill></fills><borders count="1"><border><left/><right/><top/><bottom/><diagonal/></border></borders><cellStyleXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/></cellStyleXfs><cellXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/></cellXfs></styleSheet>
    |====================
    |xl/workbook.xml
    |--------------------
    |<?xml version="1.0" encoding="UTF-8"?>
    |<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"><workbookPr date1904="false"/><bookViews><workbookView activeTab="0"/></bookViews><sheets><sheet name="Sheet0" r:id="rId3" sheetId="1"/></sheets></workbook>
    |====================
    |xl/_rels/workbook.xml.rels
    |--------------------
    |<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Target="sharedStrings.xml" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/sharedStrings"/><Relationship Id="rId2" Target="styles.xml" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles"/><Relationship Id="rId3" Target="worksheets/sheet1.xml" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet"/></Relationships>
    |====================
    |xl/worksheets/sheet1.xml
    |--------------------
    |<?xml version="1.0" encoding="UTF-8"?>
    |<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><dimension ref="A1:B2"/><sheetViews><sheetView workbookViewId="0" tabSelected="true"/></sheetViews><sheetFormatPr defaultRowHeight="15.0"/><sheetData><row r="1"><c r="A1" t="s" s="0"><v>0</v></c><c r="B1" t="s" s="0"><v>1</v></c></row><row r="2"><c r="A2" t="s" s="0"><v>2</v></c><c r="B2" t="s" s="0"><v>3</v></c></row></sheetData><pageMargins bottom="0.75" footer="0.3" header="0.3" left="0.7" right="0.7" top="0.75"/></worksheet>
    |
      """.trimMargin(),
    )
  }

  private fun getTranslationWithColon(): MutableList<ExportTranslationView> {
    val built =
      buildExportTranslationList {
        add(
          languageTag = "cs",
          keyName = "item",
          text = "name : {name}",
        )
      }
    return built.translations
  }

  private fun getIcuPlaceholdersEnabledExporter(): XlsxFileExporter {
    val built =
      buildExportTranslationList {
        add(
          languageTag = "cs",
          keyName = "key3",
          text = "{count, plural, one {# den {icuParam, number}} few {# dny} other {# dní}}",
        ) {
          key.isPlural = true
        }
        add(
          languageTag = "cs",
          keyName = "item",
          text = "I will be first '{'icuParam'}' {hello, number}",
        )
      }
    return getExporter(built.translations, true)
  }

  private fun getExporter(
    translations: List<ExportTranslationView>,
    isProjectIcuPlaceholdersEnabled: Boolean = true,
    exportParams: ExportParams = ExportParams(format = ExportFormat.XLSX),
  ): XlsxFileExporter {
    return XlsxFileExporter(
      currentDate = currentDateProvider.date,
      translations = translations,
      exportParams = exportParams,
      isProjectIcuPlaceholdersEnabled = isProjectIcuPlaceholdersEnabled,
      pathProvider =
        ExportFilePathProvider(
          template = ExportFileStructureTemplateProvider(exportParams, translations).validateAndGetTemplate(),
          extension = exportParams.format.extension,
        ),
    )
  }
}
