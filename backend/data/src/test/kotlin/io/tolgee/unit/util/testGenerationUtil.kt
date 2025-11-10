package io.tolgee.unit.util

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.tolgee.model.dataImport.ImportTranslation
import io.tolgee.service.dataImport.processors.FileProcessorContext

/**
 * Run this function in debug window to generate test result of each file processor
 * When editing, do it in the debug window and copy the result to the test file
 */
@Suppress("unused")
fun generateTestsForImportResult(fileProcessorContext: FileProcessorContext): String {
  fileProcessorContext.translations
  val languageCount = fileProcessorContext.languages.size
  val code = StringBuilder()
  val i = { i: Int -> (1..i).joinToString("") { "  " } }
  val writeMutlilineString = ms@{ string: String?, indent: Int ->
    string ?: return@ms
    code.appendLine("""${i(indent)}${"\"\"\""}""")
    code.appendLine(i(indent) + string.replace("\n", "\n${i(5)}"))
    code.appendLine("""${i(indent)}${"\"\"\""}.trimIndent()""")
  }
  val escape = { str: String?, newLines: Boolean ->
    str
      ?.replace("\\", "\\\\")
      ?.replace("\"", "\\\"")
      .let {
        if (newLines) {
          return@let it?.replace("\n", "\\n")
        }
        it
      }?.replace("\$", "\${'$'}")
  }
  code.appendLine("${i(2)}mockUtil.fileProcessorContext.assertLanguagesCount($languageCount)")
  fileProcessorContext.translations.forEach { (keyName, translations) ->
    val byLanguage = translations.groupBy { it.language.name }
    byLanguage.forEach byLang@{ (language, translations) ->
      code.appendLine("""${i(2)}mockUtil.fileProcessorContext.assertTranslations("$language", "$keyName")""")
      translations.singleOrNull()?.let { translation ->
        if (translation.isPlural) {
          code.appendLine("""${i(3)}.assertSinglePlural {""")
          code.appendLine("""${i(4)}hasText(""")
          writeMutlilineString(escape(translation.text, false), 5)
          code.appendLine("""${i(4)})""")
          code.appendLine("""${i(4)}isPluralOptimized()""")
          code.appendLine("""${i(3)}}""")
          return@byLang
        }
        code.appendLine("""${i(3)}.assertSingle {""")
        code.appendLine("""${i(4)}hasText("${escape(translation.text, true)}")""")
        code.appendLine("""${i(3)}}""")
        return@byLang
      }
      translations.firstIfAllSameOrNull()?.let { translation ->
        code.appendLine("""${i(3)}.assertAllSame {""")
        code.appendLine("""${i(4)}hasText("${escape(translation.text, true)}")""")
        code.appendLine("""${i(3)}}""")
        return@byLang
      }
    }
  }
  fileProcessorContext.keys.forEach { keyName, importKey ->
    code.appendLine("""${i(2)}mockUtil.fileProcessorContext.assertKey("$keyName"){""")
    val keyMeta = fileProcessorContext.keys[keyName]?.keyMeta
    val custom = keyMeta?.custom
    if (custom == null) {
      code.appendLine("""${i(3)}custom.assert.isNull()""")
    } else {
      code.appendLine("""${i(3)}customEquals(""")
      val customString = jacksonObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(custom)
      writeMutlilineString(customString, 4)
      code.appendLine("""${i(3)})""")
    }
    val description = keyMeta?.description
    if (description == null) {
      code.appendLine("""${i(3)}description.assert.isNull()""")
    } else {
      code.appendLine("""${i(3)}description.assert.isEqualTo("${escape(keyMeta.description, true)}")""")
    }
    code.appendLine("""${i(2)}}""")
  }

  return code.toString()
}

private fun List<ImportTranslation>.firstIfAllSameOrNull(): ImportTranslation? {
  if (this.map { it.text }.toSet().size == 1) {
    return this.first()
  }
  return null
}

/**
 * Run this function in debug window to generate test code for each export result
 * When editing, do it in the debug window and copy the result to the test file
 */
@Suppress("unused")
fun generateTestsForExportResult(data: Map<String, String>): String {
  return data
    .map {
      "data.assertFile(\"${it.key}\", \"\"\"\n" +
        "    |${
          it.value
            .replace("\$", "\${'$'}")
            .replace("\n", "\n    |")
        }\n" +
        "    \"\"\".trimMargin())"
    }.joinToString("\n")
}
