package io.tolgee.ee.data.task

class CreateMultipleTasksRequest(
  var tasks: MutableSet<CreateTaskRequest> = mutableSetOf(),
)
