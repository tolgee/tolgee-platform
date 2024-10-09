package io.tolgee.formats.importCommon

enum class ImportFileFormat(val extensions: Array<String>) {
  JSON(arrayOf("json")),
  PO(arrayOf("po")),
  STRINGS(arrayOf("strings")),
  STRINGSDICT(arrayOf("stringsdict")),
  XLIFF(arrayOf("xliff", "xlf")),
  PROPERTIES(arrayOf("properties")),
  XML(arrayOf("xml")),
  ARB(arrayOf("arb")),
  YAML(arrayOf("yaml", "yml")),
  CSV(arrayOf("csv")),
  ;

  companion object {
    private val extensionFormatMap by lazy {
      entries.flatMap { format ->
        format.extensions.map { it to format }
      }.toMap()
    }

    fun findByExtension(extension: String?): ImportFileFormat? {
      return extensionFormatMap[extension]
    }
  }
}
