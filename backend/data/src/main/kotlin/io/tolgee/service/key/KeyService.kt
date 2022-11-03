package io.tolgee.service.key

import io.tolgee.constants.Message
import io.tolgee.dtos.request.key.CreateKeyDto
import io.tolgee.dtos.request.key.EditKeyDto
import io.tolgee.dtos.request.validators.exceptions.ValidationException
import io.tolgee.exceptions.BadRequestException
import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.Project
import io.tolgee.model.key.Key
import io.tolgee.repository.KeyRepository
import io.tolgee.service.translation.TranslationService
import org.springframework.beans.factory.annotation.Autowired
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
  private val namespaceService: NamespaceService
) {
  private lateinit var translationService: TranslationService

  fun getAll(projectId: Long): Set<Key> {
    return keyRepository.getAllByProjectId(projectId)
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

    dto.screenshotUploadedImageIds?.let {
      screenshotService.saveUploadedImages(it, key)
    }

    return key
  }

  @Transactional
  fun create(project: Project, name: String, namespace: String?): Key {
    checkKeyNotExisting(projectId = project.id, name = name, namespace = namespace)

    val key = Key(name = name, project = project)
    if (!namespace.isNullOrBlank()) {
      key.namespace = namespaceService.findOrCreate(namespace, project.id)
    }
    return save(key)
  }

  fun edit(keyId: Long, dto: EditKeyDto): Key {
    val key = findOptional(keyId).orElseThrow { NotFoundException() }
    return edit(key, dto.name, dto.namespace)
  }

  fun edit(key: Key, newName: String, newNamespace: String?): Key {
    if (key.name == newName && key.namespace?.name == newNamespace) {
      return key
    }

    checkKeyNotExisting(key.project.id, newName, newNamespace)

    if (key.namespace?.name != newNamespace) {
      namespaceService.update(key, newNamespace)
    }

    key.name = newName
    return save(key)
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
}
