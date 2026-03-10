package io.tolgee.service.qa

import io.tolgee.model.enums.qa.QaCheckType
import org.springframework.stereotype.Service

@Service
class QaCheckBatchServiceOssStub : QaCheckBatchService {
  override fun runChecksAndPersist(
    projectId: Long,
    translationId: Long,
    checkTypes: List<QaCheckType>?,
  ) {
    // No-op: QA checks are an EE feature
  }
}
