package io.tolgee.service

import io.tolgee.constants.Message
import io.tolgee.dtos.PathDTO
import io.tolgee.dtos.request.DeprecatedEditKeyDTO
import io.tolgee.dtos.request.EditKeyDTO
import io.tolgee.dtos.request.SetTranslationsDTO
import io.tolgee.dtos.request.validators.exceptions.ValidationException
import io.tolgee.dtos.response.DeprecatedKeyDto
import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.Key
import io.tolgee.model.Repository
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
        private val screenshotService: ScreenshotService
) {

    private var translationService: TranslationService? = null

    @Transactional
    fun getOrCreateKey(repository: Repository, path: PathDTO): Key {
        return getOrCreateKey(repository, path.fullPathString)
    }

    @Transactional
    fun getOrCreateKey(repository: Repository, keyName: String): Key {
        val key = getOrCreateKeyNoPersist(repository, keyName)
        entityManager.persist(key)
        return key
    }

    @Transactional
    fun getOrCreateKeyNoPersist(repository: Repository, keyName: String): Key {
        return get(repository.id, keyName)
                .orElseGet {
                    Key(name = keyName, repository = repository)
                }
    }

    fun getAll(repositoryId: Long): Set<Key> {
        return keyRepository.getAllByRepositoryId(repositoryId)
    }

    fun get(repositoryId: Long, name: String): Optional<Key> {
        return keyRepository.getByNameAndRepositoryId(name, repositoryId)
    }

    fun get(repositoryId: Long, pathDTO: PathDTO): Optional<Key> {
        return keyRepository.getByNameAndRepositoryId(pathDTO.fullPathString, repositoryId)
    }

    fun get(repository: Repository, pathDTO: PathDTO): Optional<Key> {
        return keyRepository.getByNameAndRepository(pathDTO.fullPathString, repository)
    }

    fun get(id: Long): Optional<Key> {
        return keyRepository.findById(id)
    }

    fun get(ids: Set<Long>): List<Key> {
        return keyRepository.findAllById(ids)
    }

    fun create(repository: Repository, dto: DeprecatedKeyDto): Key {
        if (this.get(repository, dto.pathDto).isPresent) {
            throw ValidationException(Message.KEY_EXISTS)
        }
        val key = Key(name = dto.fullPathString, repository = repository)
        return keyRepository.save(key)
    }

    @Deprecated("Ugly naming")
    fun edit(repository: Repository, dto: DeprecatedEditKeyDTO) {
        //do nothing on no change
        if (dto.newFullPathString == dto.oldFullPathString) {
            return
        }
        if (get(repository, dto.newPathDto).isPresent) {
            throw ValidationException(Message.KEY_EXISTS)
        }
        val key = get(repository, dto.oldPathDto).orElseThrow { NotFoundException() }
        key.name = dto.newFullPathString
        keyRepository.save(key)
    }

    fun edit(repository: Repository, dto: EditKeyDTO) {
        //do nothing on no change
        if (dto.newName == dto.currentName) {
            return
        }
        if (get(repository, dto.newPathDto).isPresent) {
            throw ValidationException(Message.KEY_EXISTS)
        }
        val key = get(repository, dto.oldPathDto).orElseThrow { NotFoundException() }
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

    fun deleteAllByRepository(repositoryId: Long?) {
        keyRepository.deleteAllByRepositoryId(repositoryId)
    }

    @Transactional
    fun create(repository: Repository, dto: SetTranslationsDTO): Key {
        if (this.get(repository, PathDTO.fromFullPath(dto.key)).isPresent) {
            throw ValidationException(Message.KEY_EXISTS)
        }
        val key = Key(name = dto.key, repository = repository)
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
