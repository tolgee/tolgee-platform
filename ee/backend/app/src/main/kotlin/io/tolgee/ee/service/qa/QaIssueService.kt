package io.tolgee.ee.service.qa

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.tolgee.model.qa.TranslationQaIssue
import io.tolgee.model.translation.Translation
import io.tolgee.repository.qa.TranslationQaIssueRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class QaIssueService(
  private val qaIssueRepository: TranslationQaIssueRepository,
  private val objectMapper: ObjectMapper,
) {
  @Transactional
  fun replaceIssuesForTranslation(
    translation: Translation,
    results: List<QaCheckResult>,
  ) {
    qaIssueRepository.deleteAllByTranslationId(translation.id)
    qaIssueRepository.flush()
    val entities =
      results.map { result ->
        TranslationQaIssue(
          type = result.type,
          message = result.message,
          replacement = result.replacement,
          positionStart = result.positionStart,
          positionEnd = result.positionEnd,
          params = result.params?.let { objectMapper.writeValueAsString(it) },
          translation = translation,
        )
      }
    qaIssueRepository.saveAll(entities)
  }

  fun getIssuesForTranslation(
    projectId: Long,
    translationId: Long,
  ): List<TranslationQaIssue> {
    return qaIssueRepository.findAllByProjectAndTranslation(projectId, translationId)
  }

  fun deserializeParams(paramsJson: String?): Map<String, String>? {
    return paramsJson?.let { objectMapper.readValue<Map<String, String>>(it) }
  }
}
