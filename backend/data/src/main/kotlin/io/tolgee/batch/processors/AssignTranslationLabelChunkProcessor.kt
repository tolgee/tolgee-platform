package io.tolgee.batch.processors

import io.tolgee.batch.ProgressManager
import io.tolgee.service.label.LabelService
import jakarta.persistence.EntityManager
import org.springframework.stereotype.Component

@Component
class AssignTranslationLabelChunkProcessor(
  entityManager: EntityManager,
  progressManager: ProgressManager,
  private val labelService: LabelService,
) : AbstractTranslationLabelChunkProcessor(entityManager, progressManager) {
  override fun process(
    subChunk: List<Long>,
    languageIds: List<Long>,
    labelIds: List<Long>,
  ) {
    labelService.batchAssignLabels(subChunk, languageIds, labelIds)
  }
}
