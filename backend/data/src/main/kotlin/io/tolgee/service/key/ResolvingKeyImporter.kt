package io.tolgee.service.key

import io.tolgee.constants.Message
import io.tolgee.dtos.request.translation.importKeysResolvable.ImportKeysResolvableItemDto
import io.tolgee.dtos.request.translation.importKeysResolvable.ImportTranslationResolution
import io.tolgee.dtos.request.translation.importKeysResolvable.ImportTranslationResolvableDto
import io.tolgee.exceptions.BadRequestException
import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.Language
import io.tolgee.model.Project
import io.tolgee.model.Project_
import io.tolgee.model.key.Key
import io.tolgee.model.key.Key_
import io.tolgee.model.key.Namespace
import io.tolgee.model.key.Namespace_
import io.tolgee.model.translation.Translation
import io.tolgee.service.LanguageService
import io.tolgee.service.translation.TranslationService
import org.springframework.context.ApplicationContext
import java.io.Serializable
import javax.persistence.EntityManager
import javax.persistence.criteria.Join

class ResolvingKeyImporter(
  val applicationContext: ApplicationContext,
  val keysToImport: List<ImportKeysResolvableItemDto>,
  val projectEntity: Project
) {
  private val entityManager = applicationContext.getBean(EntityManager::class.java)
  private val keyService = applicationContext.getBean(KeyService::class.java)
  private val languageService = applicationContext.getBean(LanguageService::class.java)
  private val translationService = applicationContext.getBean(TranslationService::class.java)

  private val errors = mutableListOf<List<Serializable?>>()

  operator fun invoke() {
    tryImport()
    checkErrors()
  }

  private fun tryImport() {
    keysToImport.forEach keys@{ keyToImport ->
      val key = getOrCreateKey(keyToImport)

      keyToImport.mapLanguageAsKey().forEach translations@{ (language, resolvable) ->
        language ?: throw NotFoundException(Message.LANGUAGE_NOT_FOUND)
        val existingTranslation = getExistingTranslation(key, language)

        val isNew = existingTranslation == null

        if (validate(isNew, resolvable, key, language)) return@translations

        if (!isNew && resolvable.resolution == ImportTranslationResolution.OVERRIDE) {
          translationService.setTranslation(existingTranslation!!, resolvable.text)
          return@translations
        }

        if (isNew) {
          val translation = Translation(resolvable.text).apply {
            this.key = key
            this.language = language
          }
          translationService.save(translation)
        }
      }
    }
  }

  private fun checkErrors() {
    if (errors.isNotEmpty()) {
      @Suppress("UNCHECKED_CAST")
      throw BadRequestException(Message.IMPORT_KEYS_ERROR, errors as List<Serializable>)
    }
  }

  private fun validate(
    isNew: Boolean,
    resolvable: ImportTranslationResolvableDto,
    key: Key,
    language: Language
  ): Boolean {
    if (!isNew && resolvable.resolution == ImportTranslationResolution.NEW) {
      errors.add(
        listOf(Message.TRANSLATION_EXISTS.code, key.namespace?.name, key.name, language.tag)
      )
      return true
    }

    if (isNew && resolvable.resolution != ImportTranslationResolution.NEW) {
      errors.add(
        listOf(Message.TRANSLATION_NOT_FOUND.code, key.namespace?.name, key.name, language.tag)
      )
      return true
    }
    return false
  }

  private fun getExistingTranslation(
    key: Key,
    language: Language
  ) = existingTranslations[key.namespace?.name to key.name]?.get(language.tag)

  private fun ImportKeysResolvableItemDto.mapLanguageAsKey() =
    translations.mapNotNull { (languageTag, value) ->
      value ?: return@mapNotNull null
      languages[languageTag] to value
    }

  private fun getOrCreateKey(keyToImport: ImportKeysResolvableItemDto) =
    existingKeys[keyToImport.namespace to keyToImport.name] ?: let {
      keyService.create(
        name = keyToImport.name,
        namespace = keyToImport.namespace,
        project = projectEntity
      )
    }

  private fun getAllByNamespaceAndName(projectId: Long, keys: List<Pair<String?, String?>>): List<Key> {
    val cb = entityManager.criteriaBuilder
    val query = cb.createQuery(Key::class.java)
    val root = query.from(Key::class.java)

    @Suppress("UNCHECKED_CAST")
    val namespaceJoin: Join<Key, Namespace> = root.fetch(Key_.namespace) as Join<Key, Namespace>

    val predicates = keys.map { (namespace, name) ->
      cb.and(
        cb.equal(root.get(Key_.name), name),
        cb.equal(namespaceJoin.get(Namespace_.name), namespace)
      )
    }.toTypedArray()

    val projectIdPath = root.get(Key_.project).get(Project_.id)

    query.where(cb.and(cb.equal(projectIdPath, projectId), cb.or(*predicates)))

    return this.entityManager.createQuery(query).resultList
  }

  private val existingKeys by lazy {
    this.getAllByNamespaceAndName(
      projectId = projectEntity.id,
      keys = keysToImport.map { it.namespace to it.name }
    ).associateBy { (it.namespace?.name to it.name) }
  }

  private val languages by lazy {
    val tags = keysToImport.flatMap { it.translations.keys }.toSet()
    languageService.findByTags(tags, projectEntity.id).associateBy { it.tag }
  }

  private val keyLanguagesMap by lazy {
    keysToImport.mapNotNull {
      val key = existingKeys[it.namespace to it.name] ?: return@mapNotNull null
      val keyLanguages = it.translations.keys.mapNotNull { tag -> languages[tag] }
      key to keyLanguages
    }.toMap()
  }

  private val existingTranslations by lazy {
    translationService.get(keyLanguagesMap)
      .groupBy { it.key.namespace?.name to it.key.name }.map { (key, translations) ->
        key to translations.associateBy { it.language.tag }
      }.toMap()
  }
}
