package io.tolgee.dtos.request

import io.swagger.v3.oas.annotations.media.Schema
import io.tolgee.constants.MtServiceType
import javax.validation.constraints.NotEmpty
import javax.validation.constraints.Size

class BatchTranslateRequest {
  @NotEmpty
  var keyIds: List<Long> = listOf()

  @Size(min = 1)
  var targetLanguageIds: List<Long> = listOf()

  var useMachineTranslation: Boolean = true

  var useTranslationMemory: Boolean = true

  @field:Schema(
    description = "Translation service provider to use for translation. When null, Tolgee will use the primary service."
  )
  var service: MtServiceType? = null
}
