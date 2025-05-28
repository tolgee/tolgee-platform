package io.tolgee.service

import io.tolgee.exceptions.NotImplementedInOss
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
    throw NotImplementedInOss()
  }

  override fun removeResults(projectId: Long, userId: Long) {
    throw NotImplementedInOss()
  }

  override fun deleteResultsByLanguage(languageId: Long) {
    throw NotImplementedInOss()
  }

  override fun deleteResultsByProject(projectId: Long) {
    throw NotImplementedInOss()
  }

  override fun deleteResultsByUser(userId: Long) {
    throw NotImplementedInOss()
  }

  override fun deleteResultsByKeys(keys: Collection<Long>) {
    throw NotImplementedInOss()
  }
}
