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
    contextDescription: String?
  ) {
    throw UnsupportedOperationException("Not included in OSS")
  }

  override fun removeResults(projectId: Long, userId: Long) {
    throw UnsupportedOperationException("Not included in OSS")
  }

  override fun deleteResultsByLanguage(languageId: Long) {
    throw UnsupportedOperationException("Not included in OSS")
  }

  override fun deleteResultsByProject(projectId: Long) {
    throw UnsupportedOperationException("Not included in OSS")
  }

  override fun deleteResultsByUser(userId: Long) {
    throw UnsupportedOperationException("Not included in OSS")
  }

  override fun deleteResultsByKeys(keys: Collection<Long>) {
    throw UnsupportedOperationException("Not included in OSS")
  }
}
