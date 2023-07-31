package io.tolgee.batch.request

import io.tolgee.constants.ValidationConstants
import javax.validation.constraints.NotEmpty
import javax.validation.constraints.Size

class SetKeysNamespaceRequest {
  @NotEmpty
  var keyIds: List<Long> = listOf()

  @Size(max = ValidationConstants.MAX_NAMESPACE_LENGTH)
  var namespace: String? = null
}
