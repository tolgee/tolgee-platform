package io.tolgee.service.key

import io.tolgee.constants.Message
import io.tolgee.dtos.KeyImportResolvableResult
import io.tolgee.dtos.cacheable.ProjectDto
import io.tolgee.dtos.request.GetKeysRequestDto
import io.tolgee.dtos.request.key.CreateKeyDto
import io.tolgee.dtos.request.key.EditKeyDto
import io.tolgee.dtos.request.translation.ImportKeysItemDto
import io.tolgee.dtos.request.translation.importKeysResolvable.ImportKeysResolvableItemDto
import io.tolgee.dtos.request.validators.exceptions.ValidationException
import io.tolgee.exceptions.BadRequestException
import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.Language
import io.tolgee.model.Project
import io.tolgee.model.Screenshot
import io.tolgee.model.enums.TranslationState
import io.tolgee.model.key.Key
import io.tolgee.repository.KeyRepository
import io.tolgee.repository.LanguageRepository
import io.tolgee.service.key.utils.KeyInfoProvider
import io.tolgee.service.key.utils.KeysImporter
import io.tolgee.service.translation.TranslationService
import io.tolgee.util.Logging
import io.tolgee.util.setSimilarityLimit
import jakarta.persistence.EntityManager
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Lazy
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
class KeyService(
  private val keyRepository: KeyRepository,
  private val screenshotService: ScreenshotService,
  private val keyMetaService: KeyMetaService,
  private val tagService: TagService,
  private val namespaceService: NamespaceService,
  private val applicationContext: ApplicationContext,
  private val entityManager: EntityManager
  @Lazy
  private var translationService: TranslationService,
  private val languageRepository: LanguageRepository
) : Logging {
  private lateinit var translationService: TranslationService

  fun getAll(projectId: Long): Set<Key> {
    return keyRepository.getAllByProjectId(projectId)
  }

  fun getAllSortedById(projectId: Long): List<Key> {
    return keyRepository.getAllByProjectIdSortedById(projectId)
  }

  fun get(projectId: Long, name: String, namespace: String?): Key {
    return keyRepository.getByNameAndNamespace(projectId, name, namespace)
      .orElseThrow { NotFoundException(Message.KEY_NOT_FOUND) }!!
  }

  fun find(projectId: Long, name: String, namespace: String?): Key? {
    return this.findOptional(projectId, name, namespace).orElseGet { null }
  }

  private fun findOptional(projectId: Long, name: String, namespace: String?): Optional<Key> {
    return keyRepository.getByNameAndNamespace(projectId, name, namespace)
  }

  fun get(id: Long): Key {
    return keyRepository.findByIdOrNull(id) ?: throw NotFoundException(Message.KEY_NOT_FOUND)
  }

  fun find(id: Long): Key? {
    return keyRepository.findById(id).orElse(null)
  }

  fun findOptional(id: Long): Optional<Key> {
    return keyRepository.findById(id)
  }

  fun findAllWithProjectsAndMetas(ids: Set<Long>): List<Key> {
    return keyRepository.findWithProjectsAndMetas(ids)
  }

  fun save(key: Key): Key {
    keyRepository.save(key)
    return key
  }

  @Transactional
  fun create(project: Project, dto: CreateKeyDto): Key {
    val key = create(project, dto.name, dto.namespace)
    val created = dto.translations?.let {
      if (it.isEmpty()) {
        return@let null
      }
      translationService.setForKey(key, it)
    }

    dto.states?.map {
      val translation = created?.get(it.key)
        ?: throw BadRequestException(Message.CANNOT_SET_STATE_FOR_MISSING_TRANSLATION)
      translation to it.value.translationState
    }?.toMap()?.let { translationService.setStateBatch(it) }

    dto.tags?.forEach {
      tagService.tagKey(key, it)
    }

    storeScreenshots(dto, key)

    return key
  }

  private fun storeScreenshots(dto: CreateKeyDto, key: Key) {
    @Suppress("DEPRECATION")
    val screenshotUploadedImageIds = dto.screenshotUploadedImageIds
    val screenshots = dto.screenshots

    if (!screenshotUploadedImageIds.isNullOrEmpty() && !dto.screenshots.isNullOrEmpty()) {
      throw BadRequestException(Message.PROVIDE_ONLY_ONE_OF_SCREENSHOTS_AND_SCREENSHOT_UPLOADED_IMAGE_IDS)
    }

    if (!screenshotUploadedImageIds.isNullOrEmpty()) {
      screenshotService.saveUploadedImages(screenshotUploadedImageIds, key)
      return
    }

    if (!screenshots.isNullOrEmpty()) {
      screenshotService.saveUploadedImages(screenshots, key)
      return
    }
  }

  @Transactional
  fun create(project: Project, name: String, namespace: String?): Key {
    checkKeyNotExisting(projectId = project.id, name = name, namespace = namespace)
    return createWithoutExistenceCheck(project, name, namespace)
  }

  @Transactional
  fun createWithoutExistenceCheck(project: Project, name: String, namespace: String?): Key {
    val key = Key(name = name, project = project)
    if (!namespace.isNullOrBlank()) {
      key.namespace = namespaceService.findOrCreate(namespace, project.id)
    }
    return save(key)
  }

  @Transactional
  fun edit(keyId: Long, dto: EditKeyDto): Key {
    val key = findOptional(keyId).orElseThrow { NotFoundException() }
    return edit(key, dto.name, dto.namespace)
  }

  fun edit(key: Key, newName: String, newNamespace: String?): Key {
    if (key.name == newName && key.namespace?.name == newNamespace) {
      return key
    }

    checkKeyNotExisting(key.project.id, newName, newNamespace)

    key.name = newName

    val oldNamespace = key.namespace
    val namespaceModified = key.namespace?.name != newNamespace
    if (namespaceModified) {
      key.namespace = namespaceService.findOrCreate(newNamespace, key.project.id)
    }

    save(key)

    if (namespaceModified && oldNamespace != null) {
      namespaceService.deleteIfUnused(oldNamespace)
    }

    return key
  }

  @Transactional
  fun setNamespace(keyIds: List<Long>, namespace: String?) {
    val keys = keyRepository.getKeysWithNamespaces(keyIds)
    val projectId = keys.map { it.project.id }.distinct().singleOrNull() ?: return
    val namespaceEntity = namespaceService.findOrCreate(namespace, projectId)

    val oldNamespaces = keys.map {
      val oldNamespace = it.namespace
      it.namespace = namespaceEntity
      oldNamespace
    }

    val modifiedNamespaces = oldNamespaces
      .filter { it?.name != namespace }
      .filterNotNull()
      .distinctBy { it.id }

    namespaceService.deleteIfUnused(modifiedNamespaces)
    keyRepository.saveAll(keys)
  }

  fun checkKeyNotExisting(projectId: Long, name: String, namespace: String?) {
    if (findOptional(projectId, name, namespace).isPresent) {
      throw ValidationException(Message.KEY_EXISTS)
    }
  }

  @Transactional
  fun delete(id: Long) {
    val key = findOptional(id).orElseThrow { NotFoundException() }
    translationService.deleteAllByKey(id)
    keyMetaService.deleteAllByKeyId(id)
    screenshotService.deleteAllByKeyId(id)
    keyRepository.delete(key)
    namespaceService.deleteIfUnused(key.namespace)
  }

  @Transactional
  fun deleteMultiple(keys: List<Key>) {
    traceLogMeasureTime("delete multiple keys: delete translations") {
      translationService.deleteAllByKeys(keys.map { it.id })
    }

    traceLogMeasureTime("delete multiple keys: delete key metas") {
      keyMetaService.deleteAllByKeys(keys)
    }

    traceLogMeasureTime("delete multiple keys: delete screenshots") {
      screenshotService.deleteAllByKeyId(keys.map { it.id })
    }

    val namespaces = keys.map { it.namespace }

    traceLogMeasureTime("delete multiple keys: delete the keys") {
      keyRepository.deleteAll(keys)
    }

    namespaceService.deleteUnusedNamespaces(namespaces)
  }

  @Transactional
  fun deleteMultiple(ids: Collection<Long>) {
    traceLogMeasureTime("delete multiple keys: delete translations") {
      translationService.deleteAllByKeys(ids)
    }

    traceLogMeasureTime("delete multiple keys: delete key metas") {
      keyMetaService.deleteAllByKeyIdIn(ids)
    }

    traceLogMeasureTime("delete multiple keys: delete screenshots") {
      screenshotService.deleteAllByKeyId(ids)
    }


    val keys = traceLogMeasureTime("delete multiple keys: fetch keys") {
      keyRepository.findAllByIdInForDelete(ids)
    }

    val namespaces = keys.map { it.namespace }

    traceLogMeasureTime("delete multiple keys: delete the keys") {
      keyRepository.deleteAll(keys)
    }

    namespaceService.deleteUnusedNamespaces(namespaces)
  }

  @Transactional
  fun deleteAllByProject(projectId: Long) {
    val keys = traceLogMeasureTime("delete all keys by project: fetch keys") {
      keyRepository.getByProjectIdWithFetchedMetas(projectId)
    }
    this.deleteMultiple(keys.map { it.id })
  }

  fun checkInProject(key: Key, projectId: Long) {
    if (key.project.id != projectId) {
      throw BadRequestException(Message.KEY_NOT_FROM_PROJECT)
    }
  }

  fun saveAll(entities: Collection<Key>): MutableList<Key> = entities.map { save(it) }.toMutableList()

  @Transactional
  fun importKeys(keys: List<ImportKeysItemDto>, project: Project) {
    KeysImporter(applicationContext, keys, project).import()
  }

  @Transactional
  fun searchKeys(
    search: String,
    languageTag: String?,
    project: ProjectDto,
    pageable: Pageable
  ): Page<KeySearchResultView> {
    entityManager.setSimilarityLimit(0.00001)
    return keyRepository.searchKeys(search, project.id, languageTag, pageable)
  }

  @Transactional
  fun importKeysResolvable(keys: List<ImportKeysResolvableItemDto>, projectEntity: Project): KeyImportResolvableResult {
    val importer = ResolvingKeyImporter(
      applicationContext = applicationContext,
      keysToImport = keys,
      projectEntity = projectEntity
    )
    return importer()
  }

  @Suppress("UNCHECKED_CAST")
  fun getKeysInfo(dto: GetKeysRequestDto, projectId: Long): List<Pair<Key, List<Screenshot>>> {
    return KeyInfoProvider(applicationContext, projectId, dto).get()
  }

  fun getPaged(projectId: Long, pageable: Pageable): Page<Key> = keyRepository.getAllByProjectId(projectId, pageable)
  fun getKeysWithTags(keys: Set<Key>): List<Key> = keyRepository.getWithTags(keys)
  fun getKeysWithTagsById(keysIds: Iterable<Long>): Set<Key> = keyRepository.getWithTagsByIds(keysIds)

  fun find(id: List<Long>): List<Key> {
    return keyRepository.findAllByIdIn(id)
  }

  @Transactional
  fun getDisabledLanguages(projectId: Long, keyId: Long): List<Language> {
    return keyRepository.getDisabledLanguages(projectId, keyId)
  }

  @Transactional
  fun setDisabledLanguages(projectId: Long, keyId: Long, languageIds: List<Long>): List<Language> {
    val key = keyRepository.findByProjectIdAndId(projectId, keyId) ?: throw NotFoundException()
    enableRestOfLanguages(projectId, languageIds, key)
    return disableLanguages(projectId, languageIds, key)
  }

  private fun enableRestOfLanguages(projectId: Long, languageIdsToDisable: List<Long>, key: Key) {
    val currentlyDisabledLanguages = keyRepository.getDisabledLanguages(projectId, key.id)
    val languagesToEnable = currentlyDisabledLanguages.filter { !languageIdsToDisable.contains(it.id) }
    languagesToEnable.forEach { language ->
      val translation = translationService.find(key, language).orElse(null) ?: return@forEach
      translation.clear()
      translationService.save(translation)
    }
  }

  private fun disableLanguages(
    projectId: Long,
    languageIds: List<Long>,
    key: Key
  ): List<Language> {
    val languages = languageRepository.findAllByProjectIdAndIdInOrderById(projectId, languageIds)
    languages.map { language ->
      val translation = translationService.getOrCreate(key, language)
      translation.clear()
      translation.state = TranslationState.DISABLED
      translationService.save(translation)
    }
    return languages
  }
}
