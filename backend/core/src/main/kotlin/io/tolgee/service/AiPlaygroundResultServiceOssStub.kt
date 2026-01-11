package io.tolgee.service

import org.springframework.stereotype.Service

@Service
class AiPlaygroundResultServiceOssStub : AiPlaygroundResultService {
  override fun setResult(
    projectId: Long,
    userId: Long,
    keyId: Long,
    languageId: Long,
    translation: String?,
    contextDescription: String?,
  ) {
    // No-op: OSS doesn't have AI Playground functionality, so there are no results to set
  }

  override fun removeResults(
    projectId: Long,
    userId: Long,
  ) {
    // No-op: OSS doesn't have AI Playground functionality, so there are no results to remove
  }

  override fun deleteResultsByLanguage(languageId: Long) {
    // No-op: OSS doesn't have AI Playground functionality, so there are no results to delete
  }

  override fun deleteResultsByProject(projectId: Long) {
    // No-op: OSS doesn't have AI Playground functionality, so there are no results to delete
  }

  override fun deleteResultsByUser(userId: Long) {
    // No-op: OSS doesn't have AI Playground functionality, so there are no results to delete
  }

  override fun deleteResultsByKeys(keys: Collection<Long>) {
    // No-op: OSS doesn't have AI Playground functionality, so there are no results to delete
  }
}
