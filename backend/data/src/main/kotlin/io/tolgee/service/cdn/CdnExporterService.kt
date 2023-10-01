package io.tolgee.service.cdn

import io.tolgee.dtos.request.CdnExporterDto
import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.Project
import io.tolgee.model.cdn.CdnExporter
import io.tolgee.repository.cdn.CdnExporterRepository
import io.tolgee.util.SlugGenerator
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import javax.persistence.EntityManager
import javax.transaction.Transactional

@Service
class CdnExporterService(
  private val cdnExporterRepository: CdnExporterRepository,
  private val slugGenerator: SlugGenerator,
  private val entityManager: EntityManager
) {
  @Transactional
  fun create(projectId: Long, dto: CdnExporterDto): CdnExporter {
    val slug = slugGenerator.generate(dto.name, 3, 50) {
      cdnExporterRepository.isSlugUnique(projectId, it)
    }
    val cdnExporter = CdnExporter(entityManager.getReference(Project::class.java, projectId)).apply {
      name = dto.name
      copyPropsFrom(dto.exportParams)
      this.slug = slug
    }
    return cdnExporterRepository.save(cdnExporter)
  }

  fun get(id: Long) = find(id) ?: throw NotFoundException()

  fun find(id: Long) = cdnExporterRepository.findById(id).orElse(null)

  @Transactional
  fun update(id: Long, dto: CdnExporterDto): CdnExporter {
    val exporter = get(id)
    exporter.name = dto.name
    exporter.copyPropsFrom(dto.exportParams)
    return cdnExporterRepository.save(exporter)
  }

  fun delete(id: Long) {
    cdnExporterRepository.deleteById(id)
  }

  fun getAllInProject(projectId: Long, pageable: Pageable): Page<CdnExporter> {
    return cdnExporterRepository.findAllByProjectId(projectId, pageable)
  }

  fun get(projectId: Long, cdnId: Long): CdnExporter {
    return cdnExporterRepository.getByProjectIdAndId(projectId, cdnId)
  }
}
