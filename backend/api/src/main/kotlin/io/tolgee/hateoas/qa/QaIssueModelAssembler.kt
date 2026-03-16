package io.tolgee.hateoas.qa

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.tolgee.model.qa.TranslationQaIssue
import org.springframework.stereotype.Component

@Component
class QaIssueModelAssembler(
  private val objectMapper: ObjectMapper,
) {
  fun toModel(entity: TranslationQaIssue): QaIssueModel {
    return QaIssueModel(
      id = entity.id,
      type = entity.type,
      message = entity.message,
      replacement = entity.replacement,
      positionStart = entity.positionStart,
      positionEnd = entity.positionEnd,
      state = entity.state,
      params = entity.params?.let { objectMapper.readValue<Map<String, String>>(it) },
      pluralVariant = entity.pluralVariant,
    )
  }
}
