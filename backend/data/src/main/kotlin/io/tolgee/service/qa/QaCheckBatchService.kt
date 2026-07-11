package io.tolgee.service.qa

import io.tolgee.batch.data.BatchTranslationTargetItem
import io.tolgee.model.enums.qa.QaCheckType

interface QaCheckBatchService {
  fun runChecksAndPersist(
    projectId: Long,
    keyId: Long,
    languageId: Long,
    checkTypes: List<QaCheckType>? = null,
  )

  fun runChecksAndPersistChunk(
    projectId: Long,
    checkTypes: List<QaCheckType>? = null,
    items: List<BatchTranslationTargetItem>,
    progressCallback: () -> Unit = {},
  )
}
