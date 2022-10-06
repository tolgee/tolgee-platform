package io.tolgee.service

import io.tolgee.constants.Message
import io.tolgee.dtos.request.key.CreateKeyDto
import io.tolgee.dtos.request.key.DeprecatedEditKeyDTO
import io.tolgee.dtos.request.key.EditKeyDto
import io.tolgee.dtos.request.key.OldEditKeyDto
import io.tolgee.dtos.request.translation.SetTranslationsWithKeyDto
import io.tolgee.dtos.request.validators.exceptions.ValidationException
import io.tolgee.exceptions.BadRequestException
import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.Project
import io.tolgee.model.key.Key
import io.tolgee.repository.KeyRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*
import javax.persistence.EntityManager

@Service
class KeyService(
  private val keyRepository: KeyRepository,
  private val entityManager: EntityManager,
  private val screenshotService: ScreenshotService,
  private val keyMetaService: KeyMetaService,
  private val tagService: TagService,
) {
  private lateinit var translationService: TranslationService

  @Transactional
  fun getOrCreateKey(project: Project, keyName: String, namespace: String): Key {
    val key = getOrCreateKeyNoPersist(project, keyName, namespace)
    entityManager.persist(key)
    return key
  }

  @Transactional
  fun getOrCreateKeyNoPersist(project: Project, keyName: String, namespace: String): Key {
    return findOptional(project.id, keyName, namespace).orElseGet {
      Key(name = keyName, project = project)
    }!!
  }

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
    if (this.findOptional(project.id, dto.name, dto.namespace).isPresent) {
      throw BadRequestException(Message.KEY_EXISTS)
    }

    val key = save(Key(name = dto.name, project = project))

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

  @Deprecated("Ugly naming")
  fun edit(project: Project, dto: DeprecatedEditKeyDTO) {
    // do nothing on no change
    if (dto.newFullPathString == dto.oldFullPathString) {
      return
    }
    if (findOptional(project.id, dto.newPathDto.fullPathString, null).isPresent) {
      throw ValidationException(Message.KEY_EXISTS)
    }
    val key = get(project.id, dto.oldPathDto.fullPathString, null)
    val oldName = key.name
    key.name = dto.newFullPathString
    save(key)
  }

  fun edit(projectId: Long, dto: OldEditKeyDto): Key {
    val key = get(projectId, dto.oldPathDto.fullPathString, null)
    return edit(key, dto.newName, null)
  }

  fun edit(keyId: Long, dto: EditKeyDto): Key {
    val key = findOptional(keyId).orElseThrow { NotFoundException() }
    return edit(key, dto.name, dto.namespace)
  }

  fun edit(key: Key, newName: String, newNamespace: String?): Key {
    // do nothing on no change
    if (key.name == newName && key.namespace?.name == newNamespace) {
      return key
    }
    if (findOptional(key.project.id, newName, newNamespace).isPresent) {
      throw ValidationException(Message.KEY_EXISTS)
    }
    key.name = newName
    return save(key)
  }

  @Transactional
  fun delete(id: Long) {
    val key = findOptional(id).orElseThrow { NotFoundException() }
    translationService.deleteAllByKey(id)
    keyMetaService.deleteAllByKeyId(id)
    screenshotService.deleteAllByKeyId(id)
    keyRepository.delete(key)
  }

  @Transactional
  fun deleteMultiple(ids: Collection<Long>) {
    translationService.deleteAllByKeys(ids)
    keyMetaService.deleteAllByKeyIdIn(ids)
    screenshotService.deleteAllByKeyId(ids)
    val keys = keyRepository.findAllByIdIn(ids)
    keys.forEach {
    }
    keyRepository.deleteAllByIdIn(keys.map { it.id })
  }

  fun deleteAllByProject(projectId: Long) {
    val ids = keyRepository.getIdsByProjectId(projectId)
    keyMetaService.deleteAllByKeyIdIn(ids)
    keyRepository.deleteAllByIdIn(ids)
  }

  @Transactional
  fun create(project: Project, dto: SetTranslationsWithKeyDto): Key {
    val key = create(project, dto.key, dto.key)
    translationService.setForKey(key, dto.translations)
    return key
  }

  @Transactional
  fun create(project: Project, name: String, namespace: String?): Key {
    if (this.findOptional(project.id, name, namespace).isPresent) {
      throw BadRequestException(Message.KEY_EXISTS)
    }
    val key = Key(name = name, project = project)
    return save(key)
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
