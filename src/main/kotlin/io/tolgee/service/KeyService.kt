package io.tolgee.service

import io.tolgee.constants.Message
import io.tolgee.dtos.PathDTO
import io.tolgee.dtos.request.DeprecatedEditKeyDTO
import io.tolgee.dtos.request.EditKeyDto
import io.tolgee.dtos.request.OldEditKeyDto
import io.tolgee.dtos.request.SetTranslationsWithKeyDto
import io.tolgee.dtos.request.validators.exceptions.ValidationException
import io.tolgee.dtos.response.DeprecatedKeyDto
import io.tolgee.exceptions.BadRequestException
import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.Project
import io.tolgee.model.key.Key
import io.tolgee.repository.KeyRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*
import javax.persistence.EntityManager

@Service
class KeyService(
        private val keyRepository: KeyRepository,
        private val entityManager: EntityManager,
        private val screenshotService: ScreenshotService,
        private val keyMetaService: KeyMetaService
) {
    private var translationService: TranslationService? = null

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
        return get(project.id, keyName)
                .orElseGet {
                    Key(name = keyName, project = project)
                }
    }

    fun getAll(projectId: Long): Set<Key> {
        return keyRepository.getAllByProjectId(projectId)
    }

    fun get(projectId: Long, name: String): Optional<Key> {
        return keyRepository.getByNameAndProjectId(name, projectId)
    }

    fun get(projectId: Long, pathDTO: PathDTO): Optional<Key> {
        return keyRepository.getByNameAndProjectId(pathDTO.fullPathString, projectId)
    }

    fun get(project: Project, pathDTO: PathDTO): Optional<Key> {
        return keyRepository.getByNameAndProject(pathDTO.fullPathString, project)
    }

    fun get(id: Long): Optional<Key> {
        return keyRepository.findById(id)
    }

    fun get(ids: Set<Long>): List<Key> {
        return keyRepository.findAllById(ids)
    }

    @Deprecated("Use other create method")
    fun create(project: Project, dto: DeprecatedKeyDto): Key {
        if (this.get(project, dto.pathDto).isPresent) {
            throw ValidationException(Message.KEY_EXISTS)
        }
        val key = Key(name = dto.fullPathString, project = project)
        return keyRepository.save(key)
    }

    @Deprecated("Ugly naming")
    fun edit(project: Project, dto: DeprecatedEditKeyDTO) {
        //do nothing on no change
        if (dto.newFullPathString == dto.oldFullPathString) {
            return
        }
        if (get(project, dto.newPathDto).isPresent) {
            throw ValidationException(Message.KEY_EXISTS)
        }
        val key = get(project, dto.oldPathDto).orElseThrow { NotFoundException() }
        key.name = dto.newFullPathString
        keyRepository.save(key)
    }

    fun edit(project: Project, dto: OldEditKeyDto): Key {
        val key = get(project, dto.oldPathDto).orElseThrow { NotFoundException() }
        //do nothing on no change
        if (dto.newName == dto.currentName) {
            return key
        }
        if (get(project, dto.newPathDto).isPresent) {
            throw ValidationException(Message.KEY_EXISTS)
        }
        key.name = dto.newName
        return keyRepository.save(key)
    }

    fun edit(project: Project, keyId: Long, dto: EditKeyDto): Key {
        val key = get(keyId).orElseThrow { NotFoundException() }
        //do nothing on no change
        if (key.name == dto.name) {
            return key
        }
        if (get(project.id, dto.name).isPresent) {
            throw ValidationException(Message.KEY_EXISTS)
        }
        key.name = dto.name
        return keyRepository.save(key)
    }

    fun delete(id: Long) {
        val key = get(id).orElseThrow { NotFoundException() }
        translationService!!.deleteAllByKey(id)
        keyMetaService.deleteAllByKeyId(id)
        screenshotService.deleteAllByKeyId(id)
        keyRepository.delete(key)
    }

    fun deleteMultiple(ids: Collection<Long>) {
        translationService!!.deleteAllByKeys(ids)
        keyMetaService.deleteAllByKeyIdIn(ids)
        screenshotService.deleteAllByKeyId(ids)
        keyRepository.deleteAllByIdIn(ids)
    }

    fun deleteAllByProject(projectId: Long) {
        keyMetaService.deleteAllByProjectId(projectId)
        val ids = keyRepository.getIdsByProjectId(projectId)
        keyRepository.deleteAllByIdIn(ids)
    }

    @Transactional
    fun create(project: Project, dto: SetTranslationsWithKeyDto): Key {
        val key = create(project, dto.key)
        translationService!!.setForKey(key, dto.translations)
        return key
    }

    @Transactional
    fun create(project: Project, name: String): Key {
        if (this.get(project.id, name).isPresent) {
            throw BadRequestException(Message.KEY_EXISTS)
        }
        val key = Key(name = name, project = project)
        return keyRepository.save(key)
    }

    @Autowired
    fun setTranslationService(translationService: TranslationService?) {
        this.translationService = translationService
    }

    fun checkInProject(key: Key, project: Project) {
        if (key.project!!.id != project.id) {
            throw BadRequestException(Message.KEY_NOT_FROM_PROJECT)
        }
    }


    fun saveAll(entities: Collection<Key>): MutableList<Key> = this.keyRepository.saveAll(entities)
}
