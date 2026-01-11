package io.tolgee.formats.importCommon

enum class ImportFileFormat(
  val extensions: Array<String>,
) {
  JSON(arrayOf("json")),
  PO(arrayOf("po")),
  STRINGS(arrayOf("strings")),
  STRINGSDICT(arrayOf("stringsdict")),
  XLIFF(arrayOf("xliff", "xlf")),
  XCSTRINGS(arrayOf("xcstrings")),
  PROPERTIES(arrayOf("properties")),
  XML(arrayOf("xml")),
  ARB(arrayOf("arb")),
  YAML(arrayOf("yaml", "yml")),
  CSV(arrayOf("csv")),
  RESX(arrayOf("resx")),
  XLSX(arrayOf("xls", "xlsx")),
  ;

  companion object {
    private val extensionFormatMap by lazy {
      entries
        .flatMap { format ->
          format.extensions.map { it to format }
        }.toMap()
    }

    fun findByExtension(extension: String?): ImportFileFormat? {
      return extensionFormatMap[extension]
    }
  }
}
