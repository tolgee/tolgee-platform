package io.tolgee.service

import io.tolgee.constants.Message
import io.tolgee.dtos.PathDTO
import io.tolgee.dtos.request.DeprecatedEditKeyDTO
import io.tolgee.dtos.request.EditKeyDTO
import io.tolgee.dtos.request.SetTranslationsDTO
import io.tolgee.dtos.request.validators.exceptions.ValidationException
import io.tolgee.dtos.response.DeprecatedKeyDto
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

    fun getAll(repositoryId: Long): Set<Key> {
        return keyRepository.getAllByProjectId(repositoryId)
    }

    fun get(repositoryId: Long, name: String): Optional<Key> {
        return keyRepository.getByNameAndProjectId(name, repositoryId)
    }

    fun get(repositoryId: Long, pathDTO: PathDTO): Optional<Key> {
        return keyRepository.getByNameAndProjectId(pathDTO.fullPathString, repositoryId)
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

    fun edit(project: Project, dto: EditKeyDTO) {
        //do nothing on no change
        if (dto.newName == dto.currentName) {
            return
        }
        if (get(project, dto.newPathDto).isPresent) {
            throw ValidationException(Message.KEY_EXISTS)
        }
        val key = get(project, dto.oldPathDto).orElseThrow { NotFoundException() }
        key.name = dto.newName
        keyRepository.save(key)
    }

    fun delete(id: Long) {
        val key = get(id).orElseThrow { NotFoundException() }
        translationService!!.deleteAllByKey(id)
        screenshotService.deleteAllByKeyId(id)
        keyRepository.delete(key)
    }

    fun deleteMultiple(ids: Collection<Long>) {
        translationService!!.deleteAllByKeys(ids)
        screenshotService.deleteAllByKeyId(ids)
        keyRepository.deleteAllByIdIn(ids)
    }

    fun deleteAllByRepository(repositoryId: Long) {
        keyMetaService.deleteAllByRepositoryId(repositoryId)
        keyRepository.deleteAllByRepositoryId(repositoryId)
    }

    @Transactional
    fun create(project: Project, dto: SetTranslationsDTO): Key {
        if (this.get(project, PathDTO.fromFullPath(dto.key)).isPresent) {
            throw ValidationException(Message.KEY_EXISTS)
        }
        val key = Key(name = dto.key, project = project)
        keyRepository.save(key)
        translationService!!.setForKey(key, dto.translations!!)
        return key
    }

    @Autowired
    fun setTranslationService(translationService: TranslationService?) {
        this.translationService = translationService
    }

    fun saveAll(entities: Collection<Key>) = this.keyRepository.saveAll(entities)
}
