package io.tolgee.configuration.tolgee

import io.tolgee.configuration.annotations.DocProperty
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "tolgee.websocket")
@DocProperty(description = "Configuration specific to the use of Websocket.", displayName = "Websocket")
class WebsocketProperties(
  @DocProperty(description = "Whether to use Redis for Websocket events")
  var useRedis: Boolean = false,
)
