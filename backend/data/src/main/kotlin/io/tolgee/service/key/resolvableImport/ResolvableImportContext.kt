package io.tolgee.service.key.resolvableImport

import io.tolgee.dtos.request.translation.importKeysResolvable.ImportKeysResolvableItemDto
import io.tolgee.model.Project
import io.tolgee.model.Project_
import io.tolgee.model.enums.Scope
import io.tolgee.model.key.Key
import io.tolgee.model.key.Key_
import io.tolgee.model.key.Namespace
import io.tolgee.model.key.Namespace_
import io.tolgee.service.key.KeyService
import io.tolgee.service.key.resolvableImport.ResolvableImporter.TranslationToModify
import io.tolgee.service.language.LanguageService
import io.tolgee.service.security.SecurityService
import io.tolgee.service.translation.TranslationService
import io.tolgee.util.equalNullable
import jakarta.persistence.EntityManager
import jakarta.persistence.criteria.Join
import jakarta.persistence.criteria.JoinType
import org.springframework.context.ApplicationContext
import java.io.Serializable

class ResolvableImportContext(
  val applicationContext: ApplicationContext,
  private val keysToImport: List<ImportKeysResolvableItemDto>,
  val projectEntity: Project,
) {
  val errors = mutableListOf<List<Serializable?>>()
  var importedKeys: List<Key> = emptyList()
  val updatedTranslationIds = mutableListOf<Long>()

  // If argName is null, the change was from plural to not plural
  // If argName is not null, the change was from not plural to plural
  val isPluralChangedForKeys = mutableMapOf<Long, String?>()
  val outdatedKeys: MutableList<Long> = mutableListOf()

  private fun getAllByNamespaceAndName(
    projectId: Long,
    keys: List<Pair<String?, String?>>,
  ): List<Key> {
    val cb = entityManager.criteriaBuilder
    val query = cb.createQuery(Key::class.java)
    val root = query.from(Key::class.java)

    @Suppress("UNCHECKED_CAST")
    val namespaceJoin: Join<Key, Namespace> = root.fetch(Key_.namespace, JoinType.LEFT) as Join<Key, Namespace>

    val predicates =
      keys
        .map { (namespace, name) ->
          cb.and(
            cb.equal(root.get(Key_.name), name),
            cb.equalNullable(namespaceJoin.get(Namespace_.name), namespace),
          )
        }.toTypedArray()

    val projectIdPath = root.get(Key_.project).get(Project_.id)

    query.where(cb.and(cb.equal(projectIdPath, projectId), cb.or(*predicates)))

    return this.entityManager.createQuery(query).resultList
  }

  fun getOrCreateKey(keyToImport: ImportKeysResolvableItemDto): Pair<Key, Boolean> {
    var isNew = false
    val key =
      existingKeys.computeIfAbsent(keyToImport.namespace to keyToImport.name) {
        isNew = true
        securityService.checkProjectPermission(projectEntity.id, Scope.KEYS_CREATE)
        keyService.createWithoutExistenceCheck(
          project = projectEntity,
          name = keyToImport.name,
          namespace = keyToImport.namespace,
          isPlural = false,
        )
      }
    return key to isNew
  }

  fun getExistingTranslation(
    key: Key,
    languageTag: String,
  ) = existingTranslations[key.namespace?.name to key.name]?.get(languageTag)

  fun saveTranslations(translations: List<TranslationToModify>) {
    translations.forEach {
      translationService.setTranslationText(it.translation, it.text)
      updatedTranslationIds.add(it.translation.id)
    }
  }

  private val existingKeys by lazy {
    this
      .getAllByNamespaceAndName(
        projectId = projectEntity.id,
        keys = keysToImport.map { it.namespace to it.name },
      ).associateBy { (it.namespace?.name to it.name) }
      .toMutableMap()
  }

  val languages by lazy {
    val tags = keysToImport.flatMap { it.translations.keys }.toSet()
    languageService.findByTags(tags, projectEntity.id).associateBy { it.tag }
  }

  private val existingTranslations by lazy {
    translationService
      .get(keyLanguagesMap)
      .groupBy { it.key.namespace?.name to it.key.name }
      .map { (key, translations) ->
        key to translations.associateBy { it.language.tag }
      }.toMap()
  }

  private val keyLanguagesMap by lazy {
    keysToImport
      .mapNotNull {
        val key = existingKeys[it.namespace to it.name] ?: return@mapNotNull null
        val keyLanguages = it.translations.keys.mapNotNull { tag -> languages[tag] }
        key to keyLanguages
      }.toMap()
  }

  val keyService: KeyService by lazy {
    applicationContext.getBean(KeyService::class.java)
  }

  private val securityService by lazy {
    applicationContext.getBean(SecurityService::class.java)
  }

  private val languageService by lazy {
    applicationContext.getBean(LanguageService::class.java)
  }

  val entityManager: EntityManager by lazy {
    applicationContext.getBean(EntityManager::class.java)
  }

  val translationService: TranslationService by lazy {
    applicationContext.getBean(TranslationService::class.java)
  }
}
