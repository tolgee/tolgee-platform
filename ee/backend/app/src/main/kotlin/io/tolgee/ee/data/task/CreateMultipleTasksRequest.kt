package io.tolgee.ee.data.task

import jakarta.validation.Valid

class CreateMultipleTasksRequest(
  @field:Valid
  var tasks: MutableSet<CreateTaskRequest> = mutableSetOf(),
)
