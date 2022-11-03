package io.tolgee.configuration.tolgee

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "tolgee.websocket")
class WebsocketProperties(
  var useRedis: Boolean = false,
)
