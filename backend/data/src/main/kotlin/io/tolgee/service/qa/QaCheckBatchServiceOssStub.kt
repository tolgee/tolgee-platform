package io.tolgee.service.qa

import io.tolgee.model.enums.qa.QaCheckType
import org.springframework.stereotype.Service

@Service
class QaCheckBatchServiceOssStub : QaCheckBatchService {
  override fun runChecksAndPersist(
    projectId: Long,
    keyId: Long,
    languageId: Long,
    checkTypes: List<QaCheckType>?,
    enabledCheckTypes: Set<QaCheckType>?,
  ) {
    // No-op: QA checks are an EE feature
  }

  override fun getEnabledCheckTypesForLanguage(
    projectId: Long,
    languageId: Long,
  ): Set<QaCheckType> = emptySet()
}
