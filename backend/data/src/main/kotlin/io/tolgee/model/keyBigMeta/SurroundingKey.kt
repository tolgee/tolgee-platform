package io.tolgee.model.keyBigMeta

import io.tolgee.model.key.KeyNameAndNamespace
import javax.validation.constraints.NotBlank

data class SurroundingKey(
  @field:NotBlank
  override val name: String = "",
  override val namespace: String? = null
) : KeyNameAndNamespace
