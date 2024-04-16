package io.tolgee.api.v2.controllers.slack

import io.tolgee.development.testDataBuilder.data.SlackTestData
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.node
import io.tolgee.service.slackIntegration.OrganizationSlackWorkspaceService
import io.tolgee.testing.AuthorizedControllerTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class OrganizationSlackControllerTest : AuthorizedControllerTest() {
  @Autowired
  lateinit var slackWorkspaceService: OrganizationSlackWorkspaceService
  lateinit var testData: SlackTestData

  @BeforeEach
  fun setUp() {
    testData = SlackTestData()
    testDataService.saveTestData(testData.root)
  }

  @Test
  fun `get all works`() {
    performAuthGet(
      "/v2/organizations/${testData.organization.id}/slack/workspaces",
    ).andIsOk.andAssertThatJson {
      node("_embedded.workspaces") {
        node("[0]") {
          node("id").isEqualTo(testData.slackWorkspace.id)
        }
      }
    }
  }

  @Test
  fun `delete one works`() {
    performAuthDelete(
      "/v2/organizations/${testData.organization.id}/slack/workspaces/${testData.slackWorkspace.id}",
    ).andIsOk
    io.tolgee.testing.assertions.Assertions.assertThat(slackWorkspaceService.find(testData.slackWorkspace.id)).isNull()
  }
}
