package io.tolgee.service

import io.tolgee.dtos.request.CdnDto
import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.Project
import io.tolgee.model.cdn.Cdn
import io.tolgee.repository.CdnRepository
import io.tolgee.util.SlugGenerator
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import javax.persistence.EntityManager
import javax.transaction.Transactional

@Service
class CdnService(
  private val cdnRepository: CdnRepository,
  private val slugGenerator: SlugGenerator,
  private val entityManager: EntityManager
) {
  @Transactional
  fun create(projectId: Long, dto: CdnDto): Cdn {
    val slug = slugGenerator.generate(dto.name, 3, 50) {
      cdnRepository.isSlugUnique(projectId, it)
    }
    val cdn = Cdn(entityManager.getReference(Project::class.java, projectId)).apply {
      name = dto.name
      exportParams = dto.exportParams
      this.slug = slug
    }
    return cdnRepository.save(cdn)
  }

  fun get(id: Long) = find(id) ?: throw NotFoundException()

  fun find(id: Long) = cdnRepository.findById(id).orElse(null)

  @Transactional
  fun update(id: Long, dto: CdnDto): Cdn {
    val cdn = get(id)
    cdn.name = dto.name
    cdn.exportParams = dto.exportParams
    return cdnRepository.save(cdn)
  }

  fun delete(id: Long) {
    cdnRepository.deleteById(id)
  }

  fun getAllInProject(projectId: Long, pageable: Pageable): Page<Cdn> {
    return cdnRepository.findAllByProjectId(projectId, pageable)
  }

  fun get(projectId: Long, cdnId: Long): Cdn {
    return cdnRepository.getByProjectIdAndId(projectId, cdnId)
  }
}
