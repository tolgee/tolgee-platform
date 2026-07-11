package io.tolgee.ee.data.task

import jakarta.validation.Valid

class CreateTranslationOrderRequest(
  var agencyId: Long,
  @field:Valid
  var tasks: MutableSet<CreateTaskRequest> = mutableSetOf(),
  var sendReadOnlyInvitation: Boolean,
)
