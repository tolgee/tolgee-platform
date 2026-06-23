package io.tolgee.configuration.tolgee

import io.tolgee.configuration.annotations.DocProperty

@DocProperty(
  prefix = "tolgee.websocket",
  description = "Configuration specific to the use of Websocket.",
  displayName = "Websocket",
)
class WebsocketProperties(
  @DocProperty(description = "Whether to use Redis for Websocket events")
  var useRedis: Boolean = false,
)
