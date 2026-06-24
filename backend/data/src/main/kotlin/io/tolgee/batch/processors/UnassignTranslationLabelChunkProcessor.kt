package io.tolgee.batch.processors

import com.fasterxml.jackson.databind.ObjectMapper
import io.tolgee.batch.ProgressManager
import io.tolgee.service.label.LabelService
import jakarta.persistence.EntityManager
import org.springframework.stereotype.Component

@Component
class UnassignTranslationLabelChunkProcessor(
  entityManager: EntityManager,
  progressManager: ProgressManager,
  private val labelService: LabelService,
  objectMapper: ObjectMapper,
) : AbstractTranslationLabelChunkProcessor(entityManager, progressManager, objectMapper) {
  override fun process(
    subChunk: List<Long>,
    languageIds: List<Long>,
    labelIds: List<Long>,
  ) {
    labelService.batchUnassignLabels(subChunk, languageIds, labelIds)
  }
}
