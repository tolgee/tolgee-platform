package io.tolgee.api.v2.controllers

import io.tolgee.contributors.ContributorActivityRecorder
import io.tolgee.development.testDataBuilder.data.ContributorsTestData
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.andPrettyPrint
import io.tolgee.testing.AuthorizedControllerTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import java.util.Date

@SpringBootTest
@AutoConfigureMockMvc
class InitialDataControllerTest : AuthorizedControllerTest() {
  private var communityTestData: ContributorsTestData? = null

  @AfterEach
  fun cleanup() {
    currentDateProvider.forcedDate = null
    communityTestData?.let { testDataService.cleanTestData(it.root) }
  }

  @Test
  fun `returns initial data when authenticated`() {
    performAuthGet("/v2/public/initial-data").andPrettyPrint.andIsOk.andAssertThatJson {
      node("serverConfiguration.authentication").isEqualTo(true)
      node("userInfo.name").isEqualTo("admin")
      node("preferredOrganization.name").isEqualTo("admin")
      node("hasCommunityContributions").isEqualTo(false)
    }
  }

  @Test
  fun `returns initial data when not authenticated`() {
    performGet("/v2/public/initial-data").andPrettyPrint.andIsOk.andAssertThatJson {
      node("serverConfiguration.authentication").isEqualTo(true)
      node("userInfo").isEqualTo(null)
      node("preferredOrganization").isEqualTo(null)
      node("hasCommunityContributions").isEqualTo(false)
    }
  }

  @Test
  fun `reports hasCommunityContributions for a non-member contributor`() {
    val testData = ContributorsTestData().also { communityTestData = it }
    testDataService.saveTestData(testData.root)
    executeInNewTransaction {
      ContributorActivityRecorder.record(
        entityManager,
        currentDateProvider,
        testData.publicProject.id,
        testData.contributor.id,
        Date(1_600_000_000_000),
      )
    }

    userAccount = testData.contributor
    performAuthGet("/v2/public/initial-data").andIsOk.andAssertThatJson {
      node("hasCommunityContributions").isEqualTo(true)
    }
  }
}
