package io.tolgee.configuration.tolgee

class SocketIoProperties(
  val enabled: Boolean = false,
  val port: Int = 9090,
  val host: String? = null,
  val useRedis: Boolean = false
)
