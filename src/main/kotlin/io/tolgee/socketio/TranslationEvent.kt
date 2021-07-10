package io.tolgee.socketio

enum class TranslationEvent {
  KEY_CREATED,
  KEY_MODIFIED,
  KEY_DELETED,
  TRANSLATION_CREATED,
  TRANSLATION_MODIFIED,
  TRANSLATION_DELETED
  ;

  val eventName: String
    get() = name.lowercase()
}
