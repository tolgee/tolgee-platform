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
import io.tolgee.model.Project_
import io.tolgee.model.Screenshot
import io.tolgee.model.key.Key
import io.tolgee.model.key.KeyMeta_
import io.tolgee.model.key.Key_
import io.tolgee.model.key.Namespace
import io.tolgee.model.key.Namespace_
import io.tolgee.repository.KeyRepository
import io.tolgee.service.LanguageService
import io.tolgee.service.translation.TranslationService
import io.tolgee.util.equalNullable
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
import javax.persistence.criteria.CriteriaBuilder
import javax.persistence.criteria.Join
import javax.persistence.criteria.JoinType

@Service
class KeyService(
  private val keyRepository: KeyRepository,
  private val screenshotService: ScreenshotService,
  private val keyMetaService: KeyMetaService,
  private val tagService: TagService,
  private val namespaceService: NamespaceService,
  private val languageService: LanguageService,
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
    val keys = keyRepository.findAllByIdIn(ids)
    val namespaces = keys.map { it.namespace }
    keyRepository.deleteAllByIdIn(keys.map { it.id })
    namespaceService.deleteUnusedNamespaces(namespaces)
  }

  fun deleteAllByProject(projectId: Long) {
    val ids = keyRepository.getIdsByProjectId(projectId)
    keyMetaService.deleteAllByKeyIdIn(ids)
    keyRepository.deleteAllByIdIn(ids)
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
    val existing = this.getAll(project.id)
      .associateBy { ((it.namespace?.name to it.name)) }
      .toMutableMap()
    val namespaces = mutableMapOf<String, Namespace>()
    namespaceService.getAllInProject(project.id).associateByTo(namespaces) { it.name }
    val languageTags = keys.flatMap { it.translations.keys }.toSet()
    val languages = languageService.findByTags(languageTags, project.id).map { it.tag to it }.toMap()

    val toTag = mutableMapOf<Key, List<String>>()

    keys.forEach { keyDto ->
      val safeNamespace = namespaceService.getSafeName(keyDto.namespace)
      if (!existing.containsKey(safeNamespace to keyDto.name)) {
        val key = Key(
          name = keyDto.name,
          project = project
        ).apply {
          if (safeNamespace != null && !namespaces.containsKey(safeNamespace)) {
            val ns = namespaceService.create(safeNamespace, project.id)
            if (ns != null) {
              namespaces.put(safeNamespace, ns)
            }
          }
          this.namespace = namespaces[safeNamespace]
        }
        save(key)
        keyDto.translations.entries.forEach { (languageTag, value) ->
          languages[languageTag]?.let { language ->
            translationService.setTranslation(key, language, value)
          }
        }
        existing[safeNamespace to keyDto.name] = key

        if (!keyDto.tags.isNullOrEmpty()) {
          existing[safeNamespace to keyDto.name]?.let { key ->
            toTag[key] = keyDto.tags
          }
        }
      }
    }

    tagService.tagKeys(toTag)
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
    val cb: CriteriaBuilder = entityManager.criteriaBuilder
    val query = cb.createQuery(Key::class.java)
    val root = query.from(Key::class.java)
    val project = root.join(Key_.project)
    project.on(cb.equal(project.get(Project_.id), projectId))
    val namespace = root.fetch(Key_.namespace, JoinType.LEFT) as Join<Key, Namespace>
    val keyMeta = root.fetch(Key_.keyMeta, JoinType.LEFT)
    keyMeta.fetch(KeyMeta_.tags, JoinType.LEFT)
    val predicates = dto.keys.map { key ->
      cb.and(
        cb.equal(root.get(Key_.name), key.name),
        cb.equalNullable(namespace.get(Namespace_.name), key.namespace)
      )
    }

    val keyPredicates = cb.or(*predicates.toTypedArray())

    query.where(keyPredicates)
    query.orderBy(cb.asc(namespace.get(Namespace_.name)), cb.asc(root.get(Key_.name)))

    val result = entityManager.createQuery(query).resultList
    val screenshots = screenshotService.getScreenshotsForKeys(result.map { it.id })

    val translations = translationService.getForKeys(result.map { it.id }, dto.languageTags)
      .groupBy { it.key.id }

    result.map {
      it.translations = translations[it.id]?.toMutableList() ?: mutableListOf()
    }

    return result.map { it to (screenshots[it.id] ?: listOf()) }.toList()
  }

  fun getPaged(projectId: Long, pageable: Pageable): Page<Key> = keyRepository.getAllByProjectId(projectId, pageable)
  fun getKeysWithTags(keys: Set<Key>): List<Key> = keyRepository.getWithTags(keys)
}
