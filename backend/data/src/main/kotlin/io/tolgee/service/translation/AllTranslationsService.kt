package io.tolgee.service.translation

import io.tolgee.helpers.TextHelper
import io.tolgee.model.views.SimpleTranslationView
import io.tolgee.repository.TranslationRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.HashMap
import java.util.LinkedHashMap

@Component
class AllTranslationsService(
  private val translationRepository: TranslationRepository,
) {
  @Transactional
  @Suppress("UNCHECKED_CAST")
  fun getAllTranslations(
    languageTags: Set<String>,
    namespace: String?,
    projectId: Long,
    structureDelimiter: Char?,
  ): Map<String, Any> {
    val safeNamespace = if (namespace == "") null else namespace
    val allByLanguages = translationRepository.getTranslations(languageTags, safeNamespace, projectId)
    val langTranslations: HashMap<String, Any> = LinkedHashMap()
    for (translation in allByLanguages) {
      val map =
        langTranslations
          .computeIfAbsent(
            translation.languageTag,
          ) { LinkedHashMap<String, Any>() } as MutableMap<String, Any?>
      addToMap(translation, map, structureDelimiter)
    }
    return langTranslations
  }

  @Suppress("UNCHECKED_CAST")
  private fun addToMap(
    translation: SimpleTranslationView,
    map: MutableMap<String, Any?>,
    delimiter: Char?,
  ) {
    var currentMap = map
    val path = TextHelper.splitOnNonEscapedDelimiter(translation.key, delimiter).toMutableList()
    val name = path.removeLast()
    for (folderName in path) {
      val childMap = currentMap.computeIfAbsent(folderName) { LinkedHashMap<Any, Any>() }
      if (childMap is Map<*, *>) {
        currentMap = childMap as MutableMap<String, Any?>
        continue
      }
      // there is already string value, so we cannot replace it by map,
      // we have to save the key directly without nesting
      map[translation.key] = translation.text
      return
    }
    // The result already contains the key, so we have to add it to root without nesting
    if (currentMap.containsKey(name)) {
      map[translation.key] = translation.text
      return
    }
    currentMap[name] = translation.text
  }
}
