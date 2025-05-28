package io.tolgee.service

interface AiPlaygroundResultService {
  fun setResult(
    projectId: Long,
    userId: Long,
    keyId: Long,
    languageId: Long,
    translation: String?,
    contextDescription: String?,
  )

  fun removeResults(
    projectId: Long,
    userId: Long,
  )

  fun deleteResultsByLanguage(languageId: Long)

  fun deleteResultsByProject(projectId: Long)

  fun deleteResultsByUser(userId: Long)

  fun deleteResultsByKeys(keys: Collection<Long>)
}
