package io.tolgee.dtos.request.translation.comment

import io.tolgee.model.enums.TranslationCommentState
import jakarta.validation.constraints.NotBlank
import org.hibernate.validator.constraints.Length

data class TranslationCommentDto(
  @field:Length(max = 10000)
  @field:NotBlank
  override var text: String = "",
  override var state: TranslationCommentState = TranslationCommentState.RESOLUTION_NOT_NEEDED,
) : ITranslationCommentDto
