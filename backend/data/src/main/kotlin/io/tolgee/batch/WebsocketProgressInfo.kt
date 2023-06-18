package io.tolgee.batch

data class WebsocketProgressInfo(
  val jobId: Long,
  val processed: Long,
  val total: Long,
)
