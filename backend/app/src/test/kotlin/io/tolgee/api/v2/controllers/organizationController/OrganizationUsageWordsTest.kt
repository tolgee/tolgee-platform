package io.tolgee.api.v2.controllers.organizationController

import io.tolgee.development.testDataBuilder.data.OrganizationStatsWordCountTestData
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.node
import io.tolgee.service.organization.OrganizationStatsService
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc

/**
 * The usage endpoint is the whole cloud-side surface of word pricing — the bars, the plan-limit
 * dialog and the critical warning all read these three fields.
 */
@SpringBootTest
@AutoConfigureMockMvc
class OrganizationUsageWordsTest : BaseOrganizationControllerTest() {
  @Autowired
  private lateinit var organizationStatsService: OrganizationStatsService

  private lateinit var testData: OrganizationStatsWordCountTestData

  @BeforeEach
  fun saveWordFixture() {
    testData = OrganizationStatsWordCountTestData()
    testDataService.saveTestData(testData.root)
  }

  @AfterEach
  fun cleanup() {
    testDataService.cleanTestData(testData.root)
  }

  @Test
  fun `reports the organization's own word count, not another figure`() {
    val organization = testData.multiLangOrg
    val expected = organizationStatsService.getWordCount(organization.id)
    loginAsUser(testData.multiLangUser.username)

    performAuthGet("/v2/organizations/${organization.id}/usage")
      .andIsOk
      .andAssertThatJson {
        node("currentWords").isEqualTo(expected)
        // No billing module in this build, so the base provider reports words as unmetered.
        node("includedWords").isEqualTo(-1)
        node("wordsLimit").isEqualTo(-1)
      }
  }
}
