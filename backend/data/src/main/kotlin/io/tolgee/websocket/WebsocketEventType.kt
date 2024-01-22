package io.tolgee.websocket

enum class WebsocketEventType() {
  TRANSLATION_DATA_MODIFIED,
  BATCH_JOB_PROGRESS,
  ;

  val typeName get() = name.lowercase().replace("_", "-")
}
