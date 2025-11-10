package io.tolgee.service.label

import io.tolgee.exceptions.NotImplementedInOss
import io.tolgee.model.translation.Label
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import java.util.Optional

@Service
class LabelServiceOssStub : LabelService {
  override fun getProjectLabels(
    projectId: Long,
    pageable: Pageable,
    search: String?,
  ): Page<Label> {
    throw NotImplementedInOss()
  }

  override fun find(labelId: Long): Optional<Label> {
    throw NotImplementedInOss()
  }

  override fun getByTranslationIdsIndexed(translationIds: List<Long>): Map<Long, List<Label>> {
    return emptyMap()
  }

  override fun batchAssignLabels(
    keyIds: List<Long>,
    languageIds: List<Long>,
    labelIds: List<Long>,
  ) {
    throw NotImplementedInOss()
  }

  override fun batchUnassignLabels(
    keyIds: List<Long>,
    languageIds: List<Long>,
    labelIds: List<Long>,
  ) {
    throw NotImplementedInOss()
  }

  override fun deleteLabelsByProjectId(projectId: Long) {}

  override fun getProjectIdsForLabelIds(labelIds: List<Long>): List<Long> {
    throw NotImplementedInOss()
  }
}
