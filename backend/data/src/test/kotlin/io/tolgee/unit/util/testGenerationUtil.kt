package io.tolgee.unit.util

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.tolgee.service.dataImport.processors.FileProcessorContext

/**
 * Run this function in debug window to generate test result of each file processor
 * When editing, do it in the debug window and copy the result to the test file
 */
@Suppress("unused")
fun generateTestsForImportResult(fileProcessorContext: FileProcessorContext): String {
  val translations = fileProcessorContext.translations
  val languageCount = fileProcessorContext.languages.size
  val size = translations.size
  val code = StringBuilder()
  val i = { i: Int -> (1..i).joinToString("") { "  " } }
  val writeMutlilineString = ms@{ string: String?, indent: Int ->
    string ?: return@ms
    code.appendLine("""${i(indent)}${"\"\"\""}""")
    code.appendLine(i(indent) + string.replace("\n", "\n${i(5)}"))
    code.appendLine("""${i(indent)}${"\"\"\""}.trimIndent()""")
  }
  val escape = { str: String?, newLines: Boolean ->
    str?.replace("\"", "\\\"").let {
      if (newLines) {
        return@let it?.replace("\n", "\\n")
      }
      it
    }
  }
  code.appendLine("${i(2)}fileProcessorContext.assertLanguagesCount($languageCount)")
  fileProcessorContext.translations.forEach { (keyName, translations) ->
    val byLanguage = translations.groupBy { it.language.name }
    byLanguage.forEach { (language, translations) ->
      code.appendLine("""${i(2)}fileProcessorContext.assertTranslations("$language", "$keyName")""")
      val translation = translations.singleOrNull() ?: return@forEach
      if (translation.isPlural) {
        code.appendLine("""${i(3)}.assertSinglePlural {""")
        code.appendLine("""${i(4)}hasText(""")
        writeMutlilineString(escape(translation.text, false), 5)
        code.appendLine("""${i(4)})""")
        code.appendLine("""${i(4)}isPluralOptimized()""")
        code.appendLine("""${i(3)}}""")
      } else {
        code.appendLine("""${i(3)}.assertSingle {""")
        code.appendLine("""${i(4)}hasText("${escape(translation.text, true)}")""")
        code.appendLine("""${i(3)}}""")
      }
      code.appendLine("${i(2)}fileProcessorContext.assertLanguagesCount($languageCount)")
    }
  }
  fileProcessorContext.keys.forEach { keyName, importKey ->
    code.appendLine("""${i(2)}fileProcessorContext.assertKey("$keyName"){""")
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

/**
 * Run this function in debug window to generate test code for each export result
 * When editing, do it in the debug window and copy the result to the test file
 */
@Suppress("unused")
fun generateTestsForExportResult(data: Map<String, String>): String {
  return data.map {
    "data.assertFile(\"${it.key}\", \"\"\"\n" +
      "    |${
        it.value
          .replace("\$", "\${'$'}")
          .replace("\n", "\n    |")
      }\n" +
      "    \"\"\".trimMargin())"
  }
    .joinToString("\n")
}
