package io.tolgee.configuration.tolgee

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "tolgee.socket-io")
class SocketIoProperties(
  val enabled: Boolean = false,
  val port: Int = 9090,
  val host: String? = null,
  val useRedis: Boolean = false
)
