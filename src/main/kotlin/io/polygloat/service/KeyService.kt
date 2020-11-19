package io.polygloat.service

import io.polygloat.constants.Message
import io.polygloat.dtos.PathDTO
import io.polygloat.dtos.request.EditSourceDTO
import io.polygloat.dtos.request.SetTranslationsDTO
import io.polygloat.dtos.request.validators.exceptions.ValidationException
import io.polygloat.dtos.response.SourceDTO
import io.polygloat.exceptions.NotFoundException
import io.polygloat.model.Repository
import io.polygloat.model.Source
import io.polygloat.repository.SourceRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*
import javax.persistence.EntityManager

@Service
open class KeyService(
        private val sourceRepository: SourceRepository,
        private val entityManager: EntityManager
) {

    private var translationService: TranslationService? = null

    @Transactional
    open fun getOrCreateSource(repository: Repository, path: PathDTO): Source {
        val source = getSource(repository, path)
                .orElseGet {
                    Source(name = path.fullPathString, repository = repository)
                }
        entityManager.persist(source)
        return source
    }

    open fun getAllKeys(repositoryId: Long): Set<Source>{
        return sourceRepository.getAllByRepositoryId(repositoryId);
    }

    open fun getSource(repositoryId: Long, pathDTO: PathDTO): Optional<Source> {
        return sourceRepository.getByNameAndRepositoryId(pathDTO.fullPathString, repositoryId)
    }

    open fun getSource(repository: Repository, pathDTO: PathDTO): Optional<Source> {
        return sourceRepository.getByNameAndRepository(pathDTO.fullPathString, repository)
    }

    open fun getSource(id: Long): Optional<Source> {
        return sourceRepository.findById(id)
    }

    open fun getSources(ids: Set<Long>): List<Source> {
        return sourceRepository.findAllById(ids)
    }

    open fun createSource(repository: Repository, dto: SourceDTO) {
        if (this.getSource(repository, dto.pathDto).isPresent) {
            throw ValidationException(Message.SOURCE_EXISTS)
        }
        val source = Source(name = dto.fullPathString, repository = repository)
        sourceRepository.save(source)
    }

    open fun editSource(repository: Repository, dto: EditSourceDTO) {
        //do nothing on no change
        if (dto.newFullPathString == dto.oldFullPathString) {
            return
        }
        if (getSource(repository, dto.newPathDto).isPresent) {
            throw ValidationException(Message.SOURCE_EXISTS)
        }
        val source = getSource(repository, dto.oldPathDto).orElseThrow { NotFoundException() }
        source.name = dto.newFullPathString
        sourceRepository.save(source)
    }

    open fun deleteSource(id: Long) {
        val source = getSource(id).orElseThrow { NotFoundException() }
        translationService!!.deleteAllBySource(id)
        sourceRepository.delete(source)
    }

    open fun deleteSources(ids: Collection<Long?>?) {
        translationService!!.deleteAllBySources(ids)
        sourceRepository.deleteAllByIdIn(ids)
    }

    open fun deleteAllByRepository(repositoryId: Long?) {
        sourceRepository.deleteAllByRepositoryId(repositoryId)
    }

    @Transactional
    open fun createSource(repository: Repository, dto: SetTranslationsDTO) {
        if (this.getSource(repository, PathDTO.fromFullPath(dto.key)).isPresent) {
            throw ValidationException(Message.SOURCE_EXISTS)
        }
        val source = Source(name = dto.key, repository = repository)
        sourceRepository.save(source)
        translationService!!.setForSource(source, dto.translations)
    }

    @Autowired
    open fun setTranslationService(translationService: TranslationService?) {
        this.translationService = translationService
    }

}