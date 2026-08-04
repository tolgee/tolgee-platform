package io.tolgee.api.v2.controllers

import io.tolgee.development.testDataBuilder.data.ContributorsTestData
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsOk
import io.tolgee.testing.AuthorizedControllerTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import java.util.Date

@SpringBootTest
@AutoConfigureMockMvc
class InitialDataCommunityContributionsTest : AuthorizedControllerTest() {
  private lateinit var testData: ContributorsTestData

  @BeforeEach
  fun setup() {
    testData = ContributorsTestData()
    testDataService.saveTestData(testData.root)
  }

  @AfterEach
  fun cleanup() {
    currentDateProvider.forcedDate = null
    testDataService.cleanTestData(testData.root)
  }

  @Test
  fun `reports community contributions for a non-member contributor`() {
    recordProjectActivity(testData.publicProject.id, testData.contributor.id, Date(1_600_000_000_000))

    userAccount = testData.contributor
    performAuthGet("/v2/public/initial-data").andIsOk.andAssertThatJson {
      node("hasCommunityContributions").isEqualTo(true)
    }
  }

  @Test
  fun `reports no community contributions for a user who never contributed`() {
    userAccount = testData.noneMember
    performAuthGet("/v2/public/initial-data").andIsOk.andAssertThatJson {
      node("hasCommunityContributions").isEqualTo(false)
    }
  }

  @Test
  fun `reports no community contributions for a contributor to a private project only`() {
    recordProjectActivity(testData.project.id, testData.contributor.id, Date(1_600_000_000_000))

    userAccount = testData.contributor
    performAuthGet("/v2/public/initial-data").andIsOk.andAssertThatJson {
      node("hasCommunityContributions").isEqualTo(false)
    }
  }
}
