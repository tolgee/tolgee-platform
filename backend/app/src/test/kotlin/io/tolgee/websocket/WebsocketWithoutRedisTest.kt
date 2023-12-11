package io.tolgee.websocket

import org.junit.jupiter.api.TestInstance
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest(
  properties = [
    "tolgee.websocket.use-redis=false",
    "is-test-with-random-port=true"
  ],
  webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class WebsocketWithoutRedisTest : AbstractWebsocketTest()
