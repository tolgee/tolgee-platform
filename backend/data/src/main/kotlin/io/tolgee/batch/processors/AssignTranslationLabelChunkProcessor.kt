package io.tolgee.batch.processors

import io.tolgee.service.label.LabelService
import jakarta.persistence.EntityManager
import org.springframework.stereotype.Component

@Component
class AssignTranslationLabelChunkProcessor(
  entityManager: EntityManager,
  private val labelService: LabelService,
) : AbstractTranslationLabelChunkProcessor(entityManager) {
  override fun process(
    subChunk: List<Long>,
    languageIds: List<Long>,
    labelIds: List<Long>,
  ) {
    labelService.batchAssignLabels(subChunk, languageIds, labelIds)
  }
}
