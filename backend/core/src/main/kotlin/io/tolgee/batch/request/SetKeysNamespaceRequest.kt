package io.tolgee.batch.request

import io.tolgee.constants.ValidationConstants
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Size

class SetKeysNamespaceRequest {
  @NotEmpty
  var keyIds: List<Long> = listOf()

  @Size(max = ValidationConstants.MAX_NAMESPACE_LENGTH)
  var namespace: String? = null
}
