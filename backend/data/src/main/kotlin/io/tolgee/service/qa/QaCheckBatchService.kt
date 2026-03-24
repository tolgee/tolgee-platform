package io.tolgee.service.qa

import io.tolgee.model.enums.qa.QaCheckType

interface QaCheckBatchService {
  fun runChecksAndPersist(
    projectId: Long,
    keyId: Long,
    languageId: Long,
    checkTypes: List<QaCheckType>? = null,
  )
}
