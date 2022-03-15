package io.tolgee.socketio

import io.tolgee.CleanDbBeforeClass
import io.tolgee.testing.ContextRecreatingTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.boot.test.context.SpringBootTest

@ContextRecreatingTest
@SpringBootTest(
  properties = [
    "tolgee.socket-io.use-redis=false",
    "tolgee.socket-io.enabled=true"
  ]
)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@CleanDbBeforeClass
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
