package io.tolgee.socketio

import org.springframework.boot.test.context.SpringBootTest
import org.testng.annotations.Test

@SpringBootTest(
  properties = [
    "tolgee.socket-io.use-redis=false",
    "tolgee.socket-io.enabled=true"
  ]
)
class SocketIoWithoutRedisTest : AbstractSocketIoTest() {

  @Test
  fun `event is dispatched on key edit`() {
    assertKeyModify()
  }

  @Test
  fun `event is dispatched on key delete`() {
    assertKeyDelete()
  }

  @Test
  fun `event is dispatched on key create`() {
    assertKeyCreate()
  }

  @Test
  fun `event is dispatched on translation edit`() {
    assertTranslationModify()
  }

  @Test
  fun `event is dispatched on translation delete`() {
    assertTranslationDelete()
  }

  @Test
  fun `event is dispatched on translation create`() {
    assertTranslationCreate()
  }
}
