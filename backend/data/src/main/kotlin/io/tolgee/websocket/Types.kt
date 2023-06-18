package io.tolgee.websocket

enum class Types() {
  TRANSLATION_DATA_MODIFIED,
  BATCH_OPERATION_PROGRESS;

  val typeName get() = name.lowercase().replace("_", "-")
}
