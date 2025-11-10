package io.tolgee.service.key.utils

import io.tolgee.dtos.request.translation.ImportKeysItemDto
import io.tolgee.formats.convertToPluralIfAnyIsPlural
import io.tolgee.model.Project
import io.tolgee.model.key.Key
import io.tolgee.model.key.KeyMeta
import io.tolgee.model.key.Namespace
import io.tolgee.service.key.KeyMetaService
import io.tolgee.service.key.KeyService
import io.tolgee.service.key.NamespaceService
import io.tolgee.service.key.TagService
import io.tolgee.service.language.LanguageService
import io.tolgee.service.security.SecurityService
import io.tolgee.service.translation.TranslationService
import io.tolgee.util.getSafeNamespace
import org.springframework.context.ApplicationContext

class KeysImporter(
  applicationContext: ApplicationContext,
  val keys: List<ImportKeysItemDto>,
  val project: Project,
) {
  private val translationService: TranslationService = applicationContext.getBean(TranslationService::class.java)
  private val keyService: KeyService = applicationContext.getBean(KeyService::class.java)
  private val namespaceService: NamespaceService = applicationContext.getBean(NamespaceService::class.java)
  private val languageService: LanguageService = applicationContext.getBean(LanguageService::class.java)
  private val tagService: TagService = applicationContext.getBean(TagService::class.java)
  private val securityService: SecurityService = applicationContext.getBean(SecurityService::class.java)
  private val keyMetaService: KeyMetaService = applicationContext.getBean(KeyMetaService::class.java)

  fun import() {
    val existing =
      keyService
        .getAll(project.id)
        .associateBy { ((it.namespace?.name to it.name)) }
        .toMutableMap()
    val namespaces = mutableMapOf<String, Namespace>()
    namespaceService.getAllInProject(project.id).associateByTo(namespaces) { it.name }
    val languageTags = keys.flatMap { it.translations.keys }.toSet()
    val languages = languageService.findEntitiesByTags(languageTags, project.id).associateBy { it.tag }

    securityService.checkLanguageTranslatePermissionByTag(project.id, languageTags)

    val toTag = mutableMapOf<Key, List<String>>()
    val keyMetasToSave = mutableListOf<KeyMeta>()

    keys.forEach { keyDto ->
      val safeNamespace = getSafeNamespace(keyDto.namespace)
      if (!existing.containsKey(safeNamespace to keyDto.name)) {
        val key =
          Key(
            name = keyDto.name,
            project = project,
          ).apply {
            if (safeNamespace != null && !namespaces.containsKey(safeNamespace)) {
              val ns = namespaceService.create(safeNamespace, project.id)
              if (ns != null) {
                namespaces[safeNamespace] = ns
              }
            }
            this.namespace = namespaces[safeNamespace]
          }

        val convertedToPlurals = keyDto.translations.convertToPluralIfAnyIsPlural()?.convertedStrings
        key.isPlural = convertedToPlurals != null
        keyService.save(key)

        val translations = convertedToPlurals ?: keyDto.translations
        translations.entries.forEach { (languageTag, value) ->
          languages[languageTag]?.let { language ->
            translationService.setTranslationText(key, language, value)
          }
        }
        existing[safeNamespace to keyDto.name] = key

        if (!keyDto.tags.isNullOrEmpty()) {
          existing[safeNamespace to keyDto.name]?.let { key ->
            toTag[key] = keyDto.tags
          }
        }
        if (keyDto.description != key.keyMeta?.description) {
          val keyMeta = key.keyMeta ?: KeyMeta(key = key)
          key.keyMeta = keyMeta
          keyMeta.description = keyDto.description
          keyMetasToSave.add(keyMeta)
        }
      }
    }

    tagService.tagKeys(toTag)
    keyMetaService.saveAll(keyMetasToSave)
  }
}
