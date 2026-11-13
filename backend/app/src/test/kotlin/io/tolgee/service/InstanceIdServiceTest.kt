package io.tolgee.service

import io.tolgee.AbstractSpringTest
import io.tolgee.testing.assert
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class InstanceIdServiceTest : AbstractSpringTest() {
  @Autowired
  lateinit var instanceIdService: InstanceIdService

  @Test
  fun `stores when the instance was created`() {
    val createdAt = instanceIdService.getInstanceCreatedAt()
    createdAt.assert.isNotNull
    instanceIdService.getInstanceCreatedAt().assert.hasSameTimeAs(createdAt)
  }
}
