package io.tolgee.service.qa

import io.tolgee.model.enums.qa.QaCheckType

interface QaCheckBatchService {
  fun runChecksAndPersist(
    projectId: Long,
    translationId: Long,
    checkTypes: List<QaCheckType>? = null,
  )
}
