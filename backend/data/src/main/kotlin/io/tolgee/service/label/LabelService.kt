package io.tolgee.service.label

import io.tolgee.constants.Message
import io.tolgee.dtos.request.label.LabelRequest
import io.tolgee.exceptions.NotFoundException
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

  private fun getByProjectIdAndId(
    projectId: Long,
    labelId: Long,
  ): Label {
    return labelRepository.findByProjectIdAndId(projectId, labelId).orElseThrow { NotFoundException(Message.LABEL_NOT_FOUND) }
  }

  @Transactional
  fun createLabel(
    projectId: Long,
    request: LabelRequest,
    ): Label {
    val label = Label()
    updateFromRequest(label, request)
    label.project = entityManager.getReference(Project::class.java, projectId)

    labelRepository.save(label)
    return label
  }

  @Transactional
  fun updateLabel(
    projectId: Long,
    labelId: Long,
    request: LabelRequest,
  ): Label {
    val label = getByProjectIdAndId(projectId, labelId)
    updateFromRequest(label, request)

    labelRepository.save(label)
    return label
  }

  private fun updateFromRequest(
    label: Label,
    request: LabelRequest,
  ) {
    label.name = request.name
    label.description = request.description
    label.color = request.color
  }

  @Transactional
  fun deleteLabel(projectId: Long, labelId: Long) {
    val label = getByProjectIdAndId(projectId, labelId)
    labelRepository.delete(label)
  }
}
