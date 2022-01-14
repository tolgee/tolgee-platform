package io.tolgee.dtos.request.translation.comment

import io.tolgee.model.enums.TranslationCommentState
import org.hibernate.validator.constraints.Length
import javax.validation.constraints.NotBlank

data class TranslationCommentDto(
  @field:Length(max = 10000)
  @field:NotBlank
  override var text: String = "",

  override var state: TranslationCommentState = TranslationCommentState.RESOLUTION_NOT_NEEDED
) : ITranslationCommentDto
