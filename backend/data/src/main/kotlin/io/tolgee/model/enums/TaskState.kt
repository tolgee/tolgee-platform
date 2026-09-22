package io.tolgee.model.enums

enum class TaskState {
  NEW,
  IN_PROGRESS,
  FINISHED,
  CANCELED,
  ;

  companion object {
    val OPEN_STATES = listOf(NEW, IN_PROGRESS)

    @JvmStatic
    val OPEN_STATE_NAMES: List<String> = OPEN_STATES.map { it.name }
  }
}
