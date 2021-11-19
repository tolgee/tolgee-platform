package io.tolgee.dtos.request

import io.tolgee.model.enums.TranslationCommentState

interface ITranslationCommentDto {
  var text: String
  var state: TranslationCommentState
}
