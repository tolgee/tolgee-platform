package io.tolgee.service

import io.tolgee.AbstractSpringTest
import io.tolgee.development.testDataBuilder.data.PromptTestData
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.annotation.Transactional

@SpringBootTest(
  properties = [
    "spring.jpa.properties.hibernate.generate_statistics=true",
    "logging.level.org.hibernate.engine.internal.StatisticalLoggingSessionEventListener=WARN",
  ],
)
class UserAccountServiceTest : AbstractSpringTest() {
  @Test
  @Transactional
  fun `deletes language with ai results`() {
    val testData = PromptTestData()
    testDataService.saveTestData(testData.root)
    userAccountService.delete(testData.organizationMember.self.id)
  }
}
