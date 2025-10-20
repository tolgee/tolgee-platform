package io.tolgee.formats.genericTable.`in`

import io.tolgee.formats.genericTable.TableEntry

class TableParser(
  private val rawData: List<List<String>>,
  private val languageFallback: String,
) {
  val headers: List<String>? by lazy {
    rawData.firstOrNull()
  }

  val languages: List<String> by lazy {
    headers?.drop(1) ?: emptyList()
  }

  val languagesWithFallback: Sequence<String>
    get() = languages.asSequence().plus(generateSequence { languageFallback })

  val rows: List<List<String>> by lazy {
    rawData.drop(1)
  }

  fun List<String>.rowToTableEntries(): Sequence<TableEntry> {
    if (isEmpty()) {
      return emptySequence()
    }
    val keyName = getOrNull(0) ?: ""
    if (size == 1) {
      return sequenceOf(TableEntry(keyName, languageFallback, null))
    }
    val translations = drop(1).asSequence()
    return translations
      .zip(languagesWithFallback)
      .map { (translation, languageTag) ->
        TableEntry(
          keyName,
          languageTag,
          translation,
        )
      }
  }

  fun parse(): List<TableEntry> {
    return rows.flatMap {
      it.rowToTableEntries()
    }
  }
}
