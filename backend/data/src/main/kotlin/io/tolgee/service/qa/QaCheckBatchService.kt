package io.tolgee.service.qa

import io.tolgee.model.enums.qa.QaCheckType

interface QaCheckBatchService {
  fun runChecksAndPersist(
    projectId: Long,
    keyId: Long,
    languageId: Long,
    checkTypes: List<QaCheckType>? = null,
    enabledCheckTypes: Set<QaCheckType>? = null,
  )

  /**
   * Returns the set of QA check types enabled for a given language in a project,
   * resolving per-language overrides against the project-level config.
   */
  fun getEnabledCheckTypesForLanguage(
    projectId: Long,
    languageId: Long,
  ): Set<QaCheckType> = emptySet()
}
