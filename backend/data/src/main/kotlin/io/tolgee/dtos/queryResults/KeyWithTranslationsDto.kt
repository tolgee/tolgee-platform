package io.tolgee.dtos.queryResults

import io.tolgee.dtos.PathDTO
import java.util.LinkedList

class KeyWithTranslationsDto(
  queryResult: Array<Any?>,
) {
  val path: PathDTO
  val id: Long
  private val translations: MutableMap<String, String?> = LinkedHashMap()

  fun getTranslations(): Map<String, String?> {
    return translations
  }

  init {
    val data = LinkedList(listOf(*queryResult))
    id = data.removeFirst() as Long
    path = PathDTO.fromFullPath(data.removeFirst() as String)
    var i = 0
    while (i < data.size) {
      val key = data[i] as String?
      val value = data[i + 1] as String?

      // remove not existing languages or folders
      if (key == null) {
        i += 2
        continue
      }
      translations[key] = value
      i += 2
    }
  }
}
