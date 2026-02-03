package io.tolgee.model

interface EntityWithId {
  val id: Long

  var disableActivityLogging: Boolean
}
