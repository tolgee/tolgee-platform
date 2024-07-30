package io.tolgee.dtos.request.task

class CreateMultipleTasksRequest {
  val tasks: MutableSet<CreateTaskRequest> = mutableSetOf()
}
