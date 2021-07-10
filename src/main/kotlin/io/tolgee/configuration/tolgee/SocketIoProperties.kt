package io.tolgee.configuration.tolgee

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "tolgee.socket-io")
class SocketIoProperties(
  var enabled: Boolean = false,
  var port: Int = 9090,
  var host: String? = null,
  var useRedis: Boolean = false
)
