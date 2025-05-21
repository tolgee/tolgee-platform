package io.tolgee.service.label

import io.tolgee.dtos.request.label.CreateLabelDto
import io.tolgee.model.Project
import io.tolgee.model.translation.Label
import io.tolgee.repository.LabelRepository
import jakarta.persistence.EntityManager
import jakarta.transaction.Transactional
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import java.util.Optional

@Service
class LabelService(
  private val labelRepository: LabelRepository,
  private val entityManager: EntityManager,
  ) {
  fun getProjectLabels(projectId: Long, pageable: Pageable): Page<Label> {
    return labelRepository.findByProjectId(projectId, pageable)
  }

  fun find(labelId: Long): Optional<Label> {
    return labelRepository.findById(labelId)
  }

  @Transactional
  fun createLabel(
    projectId: Long,
    dto: CreateLabelDto,
    ): Label {
    val label = Label()

    label.name = dto.name
    label.description = dto.description
    label.color = dto.color
    label.project = entityManager.getReference(Project::class.java, projectId)

    labelRepository.save(label)
    return label
  }

  @Transactional
  fun updateLabel(
    labelId: Long,
    dto: CreateLabelDto,
  ): Label {
    val label = labelRepository.getReferenceById(labelId)
    label.name = dto.name
    label.description = dto.description
    label.color = dto.color

    labelRepository.save(label)
    return label
  }

  @Transactional
  fun deleteLabel(label: Label) {
    labelRepository.delete(label)
  }
}
