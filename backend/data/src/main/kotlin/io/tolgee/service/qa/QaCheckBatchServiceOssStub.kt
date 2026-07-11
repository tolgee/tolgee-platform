package io.tolgee.service.qa

import io.tolgee.batch.data.BatchTranslationTargetItem
import io.tolgee.model.enums.qa.QaCheckType
import org.springframework.stereotype.Service

@Service
class QaCheckBatchServiceOssStub : QaCheckBatchService {
  override fun runChecksAndPersist(
    projectId: Long,
    keyId: Long,
    languageId: Long,
    checkTypes: List<QaCheckType>?,
  ) {
    // No-op: QA checks are an EE feature
  }

  override fun runChecksAndPersistChunk(
    projectId: Long,
    checkTypes: List<QaCheckType>?,
    items: List<BatchTranslationTargetItem>,
    progressCallback: () -> Unit,
  ) {
    // No-op: QA checks are an EE feature
  }
}
