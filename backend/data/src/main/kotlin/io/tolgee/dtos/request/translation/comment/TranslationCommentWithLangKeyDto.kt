package io.tolgee.dtos.request.translation.comment

import io.tolgee.model.enums.TranslationCommentState
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import org.hibernate.validator.constraints.Length

data class TranslationCommentWithLangKeyDto(
  @field:NotNull
  val keyId: Long = 0,
  @field:NotNull
  val languageId: Long = 0,
  @field:Length(max = 10000)
  @field:NotBlank
  override var text: String = "",
  override var state: TranslationCommentState = TranslationCommentState.RESOLUTION_NOT_NEEDED,
) : ITranslationCommentDto
