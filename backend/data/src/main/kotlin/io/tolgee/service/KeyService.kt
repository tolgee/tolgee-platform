package io.tolgee.service

import io.tolgee.constants.Message
import io.tolgee.dtos.PathDTO
import io.tolgee.dtos.request.key.CreateKeyDto
import io.tolgee.dtos.request.key.DeprecatedEditKeyDTO
import io.tolgee.dtos.request.key.EditKeyDto
import io.tolgee.dtos.request.key.OldEditKeyDto
import io.tolgee.dtos.request.translation.SetTranslationsWithKeyDto
import io.tolgee.dtos.request.validators.exceptions.ValidationException
import io.tolgee.dtos.response.DeprecatedKeyDto
import io.tolgee.exceptions.BadRequestException
import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.Project
import io.tolgee.model.key.Key
import io.tolgee.repository.KeyRepository
import io.tolgee.socketio.ITranslationsSocketIoModule
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
  private val translationsSocketIoModule: ITranslationsSocketIoModule,
  private val tagService: TagService,
) {
  private lateinit var translationService: TranslationService

  @Transactional
  fun getOrCreateKey(project: Project, path: PathDTO): Key {
    return getOrCreateKey(project, path.fullPathString)
  }

  @Transactional
  fun getOrCreateKey(project: Project, keyName: String): Key {
    val key = getOrCreateKeyNoPersist(project, keyName)
    entityManager.persist(key)
    return key
  }

  @Transactional
  fun getOrCreateKeyNoPersist(project: Project, keyName: String): Key {
    return findOptional(project.id, keyName)
      .orElseGet {
        Key(name = keyName, project = project)
      }
  }

  fun getAll(projectId: Long): Set<Key> {
    return keyRepository.getAllByProjectId(projectId)
  }

  fun get(projectId: Long, name: String): Key {
    return keyRepository.getByNameAndProjectId(name, projectId).orElse(null)
      ?: throw NotFoundException(Message.KEY_NOT_FOUND)
  }

  fun findOptional(projectId: Long, name: String): Optional<Key> {
    return keyRepository.getByNameAndProjectId(name, projectId)
  }

  fun findOptional(projectId: Long, pathDTO: PathDTO): Optional<Key> {
    return keyRepository.getByNameAndProjectId(pathDTO.fullPathString, projectId)
  }

  fun get(id: Long): Key {
    return keyRepository.findByIdOrNull(id) ?: throw NotFoundException(Message.KEY_NOT_FOUND)
  }

  fun findOptional(id: Long): Optional<Key> {
    return keyRepository.findById(id)
  }

  fun findOptional(ids: Set<Long>): List<Key> {
    return keyRepository.findAllById(ids)
  }

  fun save(key: Key): Key {
    val wasCreated = key.id == 0L
    keyRepository.save(key)
    // modification is handled in edit separately
    if (wasCreated) {
      translationsSocketIoModule.onKeyCreated(key)
    }
    return key
  }

  @Deprecated("Use other create method")
  fun create(project: Project, dto: DeprecatedKeyDto): Key {
    if (this.findOptional(project.id, dto.pathDto).isPresent) {
      throw ValidationException(io.tolgee.constants.Message.KEY_EXISTS)
    }
    val key = Key(name = dto.fullPathString, project = project)
    return save(key)
  }

  @Transactional
  fun create(project: Project, dto: CreateKeyDto): Key {
    if (this.findOptional(project.id, dto.name).isPresent) {
      throw BadRequestException(io.tolgee.constants.Message.KEY_EXISTS)
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
    if (findOptional(project.id, dto.newPathDto).isPresent) {
      throw ValidationException(io.tolgee.constants.Message.KEY_EXISTS)
    }
    val key = findOptional(project.id, dto.oldPathDto).orElseThrow { NotFoundException() }
    val oldName = key.name
    key.name = dto.newFullPathString
    translationsSocketIoModule.onKeyModified(key, oldName)
    save(key)
  }

  fun edit(projectId: Long, dto: OldEditKeyDto): Key {
    val key = findOptional(projectId, dto.oldPathDto).orElseThrow { NotFoundException() }
    return edit(key, dto.newName)
  }

  fun edit(keyId: Long, dto: EditKeyDto): Key {
    val key = findOptional(keyId).orElseThrow { NotFoundException() }
    return edit(key, dto.name)
  }

  fun edit(key: Key, newName: String): Key {
    val oldName = key.name
    // do nothing on no change
    if (key.name == newName) {
      return key
    }
    if (findOptional(key.project!!.id, newName).isPresent) {
      throw ValidationException(Message.KEY_EXISTS)
    }
    key.name = newName
    translationsSocketIoModule.onKeyModified(key, oldName)
    return save(key)
  }

  @Transactional
  fun delete(id: Long) {
    val key = findOptional(id).orElseThrow { NotFoundException() }
    translationService.deleteAllByKey(id)
    keyMetaService.deleteAllByKeyId(id)
    screenshotService.deleteAllByKeyId(id)
    keyRepository.delete(key)
    translationsSocketIoModule.onKeyDeleted(key)
  }

  @Transactional
  fun deleteMultiple(ids: Collection<Long>) {
    translationService.deleteAllByKeys(ids)
    keyMetaService.deleteAllByKeyIdIn(ids)
    screenshotService.deleteAllByKeyId(ids)
    val keys = keyRepository.findAllByIdIn(ids)
    keys.forEach {
      translationsSocketIoModule.onKeyDeleted(it)
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
    val key = create(project, dto.key)
    translationService.setForKey(key, dto.translations)
    return key
  }

  @Transactional
  fun create(project: Project, name: String): Key {
    if (this.findOptional(project.id, name).isPresent) {
      throw BadRequestException(io.tolgee.constants.Message.KEY_EXISTS)
    }
    val key = Key(name = name, project = project)
    return save(key)
  }

  @Autowired
  fun setTranslationService(translationService: TranslationService) {
    this.translationService = translationService
  }

  fun checkInProject(key: Key, projectId: Long) {
    if (key.project!!.id != projectId) {
      throw BadRequestException(io.tolgee.constants.Message.KEY_NOT_FROM_PROJECT)
    }
  }

  fun saveAll(entities: Collection<Key>): MutableList<Key> = entities.map { save(it) }.toMutableList()
}
