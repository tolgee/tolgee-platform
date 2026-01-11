package io.tolgee.service.label

import io.tolgee.model.translation.Label
import jakarta.transaction.Transactional
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.util.Optional

interface LabelService {
  fun getProjectLabels(
    projectId: Long,
    pageable: Pageable,
    search: String? = null,
  ): Page<Label>

  fun find(labelId: Long): Optional<Label>

  fun getByTranslationIdsIndexed(translationIds: List<Long>): Map<Long, List<Label>>

  @Transactional
  fun batchAssignLabels(
    keyIds: List<Long>,
    languageIds: List<Long>,
    labelIds: List<Long>,
  )

  @Transactional
  fun batchUnassignLabels(
    keyIds: List<Long>,
    languageIds: List<Long>,
    labelIds: List<Long>,
  )

  fun deleteLabelsByProjectId(projectId: Long)

  fun getProjectIdsForLabelIds(labelIds: List<Long>): List<Long>
}
