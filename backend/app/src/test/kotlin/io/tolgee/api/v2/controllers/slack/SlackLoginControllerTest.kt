package io.tolgee.api.v2.controllers.slack

import io.tolgee.development.testDataBuilder.data.SlackTestData
import io.tolgee.fixtures.andIsNotFound
import io.tolgee.fixtures.andIsOk
import io.tolgee.testing.AuthorizedControllerTest
import org.junit.jupiter.api.Test

class SlackLoginControllerTest : AuthorizedControllerTest() {
  @Test
  fun `user log in`() {
    val testData = SlackTestData()
    testDataService.saveTestData(testData.root)

    performAuthPost(
      "/v2/slack/user-login",
      mapOf(
        "slackId" to "TEST",
        "channelId" to "TEST",
        "workspaceId" to testData.slackWorkspace.id,
      ),
    ).andIsOk
  }

  @Test
  fun `user does not log in`() {
    performAuthPost(
      "/v2/slack/user-login",
      mapOf(
        "slackId" to "TEST",
        "channelId" to "TEST",
        "workspaceId" to 1,
      ),
    ).andIsNotFound
  }
}
