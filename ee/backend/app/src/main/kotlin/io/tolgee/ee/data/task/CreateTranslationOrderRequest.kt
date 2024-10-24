package io.tolgee.ee.data.task

class CreateTranslationOrderRequest(
  var agencyId: Long,
  var tasks: MutableSet<CreateTaskRequest> = mutableSetOf(),
)
