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
import io.tolgee.model.Project
import io.tolgee.model.Screenshot
import io.tolgee.model.key.Key
import io.tolgee.repository.KeyRepository
import io.tolgee.service.key.utils.KeyInfoProvider
import io.tolgee.service.key.utils.KeysImporter
import io.tolgee.service.translation.TranslationService
import io.tolgee.util.setSimilarityLimit
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*
import javax.persistence.EntityManager

@Service
class KeyService(
  private val keyRepository: KeyRepository,
  private val screenshotService: ScreenshotService,
  private val keyMetaService: KeyMetaService,
  private val tagService: TagService,
  private val namespaceService: NamespaceService,
  private val applicationContext: ApplicationContext,
  private val entityManager: EntityManager
) {
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
    dto.translations?.let {
      translationService.setForKey(key, it)
    }

    dto.tags?.forEach {
      tagService.tagKey(key, it)
    }

    storeScreenshots(dto, key)

    return key
  }

  private fun storeScreenshots(dto: CreateKeyDto, key: Key) {
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
  fun deleteMultiple(ids: Collection<Long>) {
    translationService.deleteAllByKeys(ids)
    keyMetaService.deleteAllByKeyIdIn(ids)
    screenshotService.deleteAllByKeyId(ids)
    val keys = keyRepository.findAllByIdInForDelete(ids)
    val namespaces = keys.map { it.namespace }
    keyRepository.deleteAllByIdIn(keys.map { it.id })
    namespaceService.deleteUnusedNamespaces(namespaces)
  }

  @Transactional
  fun deleteAllByProject(projectId: Long) {
    val ids = keyRepository.getIdsByProjectId(projectId)
    keyMetaService.deleteAllByKeyIdIn(ids)
    this.deleteMultiple(ids)
  }

  @Autowired
  fun setTranslationService(translationService: TranslationService) {
    this.translationService = translationService
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
}
