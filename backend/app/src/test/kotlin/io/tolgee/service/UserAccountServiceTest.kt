package io.tolgee.service

import io.tolgee.AbstractSpringTest
import io.tolgee.development.testDataBuilder.builders.TestDataBuilder
import io.tolgee.development.testDataBuilder.data.ContributorsTestData
import io.tolgee.development.testDataBuilder.data.PromptTestData
import io.tolgee.testing.assertions.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.annotation.Transactional
import java.util.Date

@SpringBootTest(
  properties = [
    "spring.jpa.properties.hibernate.generate_statistics=true",
    "logging.level.org.hibernate.engine.internal.StatisticalLoggingSessionEventListener=WARN",
    "spring.jpa.show-sql=true",
    // keep in sync with OrganizationServiceTest properties to share Spring test context
    "tolgee.machine-translation.free-credits-amount=100000",
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

  @Test
  fun `findAllByUsername returns a soft-deleted row that findActive hides`() {
    val testData = ContributorsTestData()
    testDataService.saveTestData(testData.root)
    try {
      val username = testData.deletedContributor.username
      assertThat(userAccountService.findAllByUsername(username)).hasSize(1)
      assertThat(userAccountService.findActive(username)).isNull()
    } finally {
      testDataService.cleanTestData(testData.root)
    }
  }

  @Test
  fun `findAllByUsername returns both rows when an active and a soft-deleted user share a username`() {
    val username = "collision@contributors.com"
    val testData =
      TestDataBuilder().apply {
        addUserAccountWithoutOrganization {
          this.username = username
          deletedAt = Date()
        }
        addUserAccountWithoutOrganization {
          this.username = username
        }
      }
    testDataService.saveTestData(testData)
    try {
      assertThat(userAccountService.findAllByUsername(username)).hasSize(2)
      val active = userAccountService.findActive(username)
      assertThat(active).isNotNull
      assertThat(active!!.deletedAt).isNull()
    } finally {
      testDataService.cleanTestData(testData)
    }
  }
}
