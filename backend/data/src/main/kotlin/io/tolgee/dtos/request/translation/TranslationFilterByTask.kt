package io.tolgee.dtos.request.translation

import io.tolgee.constants.Message
import io.tolgee.exceptions.BadRequestException
import io.tolgee.model.enums.TaskFilterStatus
import io.tolgee.model.enums.TaskType

data class TranslationFilterByTask(
  val languageTag: String,
  val taskType: TaskType,
  val status: TaskFilterStatus,
) {
  companion object {
    private const val PARTS = 3

    /**
     * Spring hands a single `en,TRANSLATE,NEVER_IN_TASK` over as three separate list entries, while
     * several values arrive already comma-joined. Both shapes have to parse.
     */
    fun parseList(strings: List<String>): List<TranslationFilterByTask> {
      if (strings.all { it.contains(",") }) {
        return strings.map { parse(it.split(",")) }
      }
      if (strings.size % PARTS != 0) throw invalid()
      return strings.chunked(PARTS).map { parse(it) }
    }

    private fun parse(parts: List<String>): TranslationFilterByTask {
      if (parts.size != PARTS) throw invalid()
      return try {
        TranslationFilterByTask(
          languageTag = parts[0],
          taskType = TaskType.valueOf(parts[1]),
          status = TaskFilterStatus.valueOf(parts[2]),
        )
      } catch (e: IllegalArgumentException) {
        throw invalid()
      }
    }

    private fun invalid() = BadRequestException(Message.FILTER_BY_VALUE_TASK_NOT_VALID)
  }
}
