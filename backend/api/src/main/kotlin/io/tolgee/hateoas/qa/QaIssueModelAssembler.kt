package io.tolgee.hateoas.qa

import io.tolgee.model.qa.TranslationQaIssue
import org.springframework.stereotype.Component

@Component
class QaIssueModelAssembler {
  fun toModel(entity: TranslationQaIssue): QaIssueModel {
    return QaIssueModel(
      id = entity.id,
      type = entity.type,
      message = entity.message,
      replacement = entity.replacement,
      positionStart = entity.positionStart,
      positionEnd = entity.positionEnd,
      state = entity.state,
      params = entity.params,
      pluralVariant = entity.pluralVariant,
    )
  }
}
