package io.tolgee.websocket

enum class Types() {
  TRANSLATION_DATA_MODIFIED;

  val typeName get() = name.lowercase().replace("_", "-")
}
