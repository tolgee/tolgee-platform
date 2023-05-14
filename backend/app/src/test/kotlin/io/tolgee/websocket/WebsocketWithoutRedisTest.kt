package io.tolgee.websocket

import io.tolgee.testing.ContextRecreatingTest
import org.junit.jupiter.api.TestInstance
import org.springframework.boot.test.context.SpringBootTest

@ContextRecreatingTest
@SpringBootTest(
  properties = [
    "tolgee.websocket.use-redis=false",
  ],
  webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
// @Disabled("Test if it works when disabled")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class WebsocketWithoutRedisTest : AbstractWebsocketTest()
