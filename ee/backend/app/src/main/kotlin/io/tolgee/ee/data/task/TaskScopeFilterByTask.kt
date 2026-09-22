package io.tolgee.ee.data.task

import io.tolgee.constants.Message
import io.tolgee.exceptions.BadRequestException
import io.tolgee.model.enums.TaskFilterStatus
import io.tolgee.model.enums.TaskType

data class TaskScopeFilterByTask(
  val taskType: TaskType,
  val status: TaskFilterStatus,
) {
  companion object {
    private const val PARTS = 2

    fun parseList(strings: List<String>): List<TaskScopeFilterByTask> {
      if (strings.isEmpty()) return emptyList()
      if (strings.all { it.contains(",") }) {
        return strings.map { parse(it.split(",")) }
      }
      if (strings.size % PARTS != 0) throw invalid()
      return strings.chunked(PARTS).map { parse(it) }
    }

    private fun parse(parts: List<String>): TaskScopeFilterByTask {
      if (parts.size != PARTS) throw invalid()
      return try {
        TaskScopeFilterByTask(TaskType.valueOf(parts[0]), TaskFilterStatus.valueOf(parts[1]))
      } catch (e: IllegalArgumentException) {
        throw invalid()
      }
    }

    private fun invalid() = BadRequestException(Message.FILTER_BY_VALUE_TASK_NOT_VALID)
  }
}
