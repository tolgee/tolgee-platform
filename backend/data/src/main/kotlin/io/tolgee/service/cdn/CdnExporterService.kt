package io.tolgee.service.cdn

import io.tolgee.component.CdnStorageProvider
import io.tolgee.dtos.request.CdnExporterDto
import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.Project
import io.tolgee.model.cdn.CdnExporter
import io.tolgee.model.cdn.CdnStorage
import io.tolgee.repository.cdn.CdnExporterRepository
import io.tolgee.service.project.ProjectService
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
  private val entityManager: EntityManager,
  private val cdnStorageProvider: CdnStorageProvider,
  private val projectService: ProjectService
) {
  @Transactional
  fun create(projectId: Long, dto: CdnExporterDto): CdnExporter {
    val cdnExporter = CdnExporter(entityManager.getReference(Project::class.java, projectId))
    cdnExporter.name = dto.name
    cdnExporter.cdnStorage = getStorage(projectId, dto.cdnStorageId)
    cdnExporter.copyPropsFrom(dto)
    cdnExporter.slug = generateSlug(projectId)
    return cdnExporterRepository.save(cdnExporter)
  }

  fun generateSlug(projectId: Long): String {
    val projectDto = projectService.getDto(projectId)
    return slugGenerator.generate(projectDto.name, 3, 50) {
      cdnExporterRepository.isSlugUnique(projectDto.id, it)
    }
  }

  private fun getStorage(projectId: Long, cdnStorageId: Long?): CdnStorage? {
    cdnStorageId ?: return null
    return cdnStorageProvider.getStorage(projectId, cdnStorageId)
  }

  fun get(id: Long) = find(id) ?: throw NotFoundException()

  fun find(id: Long) = cdnExporterRepository.findById(id).orElse(null)

  @Transactional
  fun update(projectId: Long, id: Long, dto: CdnExporterDto): CdnExporter {
    val exporter = get(projectId, id)
    exporter.cdnStorage = getStorage(projectId, dto.cdnStorageId)
    exporter.name = dto.name
    exporter.copyPropsFrom(dto)
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
