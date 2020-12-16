package io.polygloat.service

import io.polygloat.constants.Message
import io.polygloat.dtos.PathDTO
import io.polygloat.dtos.request.EditKeyDTO
import io.polygloat.dtos.request.SetTranslationsDTO
import io.polygloat.dtos.request.validators.exceptions.ValidationException
import io.polygloat.dtos.response.KeyDTO
import io.polygloat.exceptions.NotFoundException
import io.polygloat.model.Repository
import io.polygloat.model.Key
import io.polygloat.repository.KeyRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*
import javax.persistence.EntityManager

@Service
open class KeyService(
        private val keyRepository: KeyRepository,
        private val entityManager: EntityManager,
        private val screenshotService: ScreenshotService
) {

    private var translationService: TranslationService? = null

    @Transactional
    open fun getOrCreateKey(repository: Repository, path: PathDTO): Key {
        val key = get(repository, path)
                .orElseGet {
                    Key(name = path.fullPathString, repository = repository)
                }
        entityManager.persist(key)
        return key
    }

    open fun getAll(repositoryId: Long): Set<Key> {
        return keyRepository.getAllByRepositoryId(repositoryId)
    }

    open fun get(repositoryId: Long, pathDTO: PathDTO): Optional<Key> {
        return keyRepository.getByNameAndRepositoryId(pathDTO.fullPathString, repositoryId)
    }

    open fun get(repository: Repository, pathDTO: PathDTO): Optional<Key> {
        return keyRepository.getByNameAndRepository(pathDTO.fullPathString, repository)
    }

    open fun get(id: Long): Optional<Key> {
        return keyRepository.findById(id)
    }

    open fun get(ids: Set<Long>): List<Key> {
        return keyRepository.findAllById(ids)
    }

    open fun create(repository: Repository, dto: KeyDTO): Key {
        if (this.get(repository, dto.pathDto).isPresent) {
            throw ValidationException(Message.KEY_EXISTS)
        }
        val key = Key(name = dto.fullPathString, repository = repository)
        return keyRepository.save(key)
    }

    open fun edit(repository: Repository, dto: EditKeyDTO) {
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

    open fun delete(id: Long) {
        val key = get(id).orElseThrow { NotFoundException() }
        translationService!!.deleteAllByKey(id)
        screenshotService.deleteAllByKeyId(id)
        keyRepository.delete(key)
    }

    open fun deleteMultiple(ids: Collection<Long>) {
        translationService!!.deleteAllByKeys(ids)
        screenshotService.deleteAllByKeyId(ids)
        keyRepository.deleteAllByIdIn(ids)
    }

    open fun deleteAllByRepository(repositoryId: Long?) {
        keyRepository.deleteAllByRepositoryId(repositoryId)
    }

    @Transactional
    open fun create(repository: Repository, dto: SetTranslationsDTO): Key {
        if (this.get(repository, PathDTO.fromFullPath(dto.key)).isPresent) {
            throw ValidationException(Message.KEY_EXISTS)
        }
        val key = Key(name = dto.key, repository = repository)
        keyRepository.save(key)
        translationService!!.setForKey(key, dto.translations)
        return key
    }

    @Autowired
    open fun setTranslationService(translationService: TranslationService?) {
        this.translationService = translationService
    }

}