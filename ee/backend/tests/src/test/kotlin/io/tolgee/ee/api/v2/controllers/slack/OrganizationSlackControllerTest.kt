package io.tolgee.ee.api.v2.controllers.slack

import io.tolgee.configuration.tolgee.SlackProperties
import io.tolgee.constants.Feature
import io.tolgee.development.testDataBuilder.data.SlackTestData
import io.tolgee.dtos.cacheable.ProjectDto
import io.tolgee.ee.component.PublicEnabledFeaturesProvider
import io.tolgee.ee.service.slackIntegration.OrganizationSlackWorkspaceService
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsBadRequest
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.node
import io.tolgee.security.ProjectHolder
import io.tolgee.testing.AuthorizedControllerTest
import io.tolgee.util.executeInNewTransaction
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.anyString
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.web.client.RestTemplate

class OrganizationSlackControllerTest : AuthorizedControllerTest() {
  @Autowired
  lateinit var slackWorkspaceService: OrganizationSlackWorkspaceService

  @Autowired
  private lateinit var projectHolder: ProjectHolder

  lateinit var testData: SlackTestData

  @MockitoBean
  @Autowired
  lateinit var restTemplate: RestTemplate

  @Autowired
  lateinit var slackProperties: SlackProperties

  @Autowired
  private lateinit var enabledFeaturesProvider: PublicEnabledFeaturesProvider

  @BeforeEach
  fun setUp() {
    testData = SlackTestData()
    testDataService.saveTestData(testData.root)
    enabledFeaturesProvider.forceEnabled = setOf(Feature.SLACK_INTEGRATION)
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
    io.tolgee.testing.assertions.Assertions
      .assertThat(slackWorkspaceService.find(testData.slackWorkspace.id))
      .isNull()
  }

  @Test
  fun `fail if connect and already have connection`() {
    slackProperties.clientId = "clientId"
    slackProperties.clientSecret = "clientSecret"
    mockSlackRequest()

    performAuthPost(
      "/v2/organizations/${testData.organization.id}/slack/connect",
      mapOf(
        "code" to "testCode",
      ),
    ).andIsBadRequest
  }

  @Test
  fun `connection works`() {
    slackWorkspaceService.delete(testData.slackWorkspace)
    slackWorkspaceService.delete(testData.slackWorkspace2)

    Assertions.assertThat(slackWorkspaceService.findAllWorkspaces(testData.slackWorkspace.organization.id)).isEmpty()

    slackProperties.clientId = "clientId"
    slackProperties.clientSecret = "clientSecret"

    executeInNewTransaction(platformTransactionManager) {
      val projectDto = ProjectDto.fromEntity(testData.projectBuilder.self)
      projectHolder.project = projectDto
    }

    mockSlackRequest()

    performAuthPost(
      "/v2/organizations/${testData.organization.id}/slack/connect",
      mapOf(
        "code" to "testCode",
      ),
    ).andIsOk

    Assertions
      .assertThat(slackWorkspaceService.findAllWorkspaces(testData.slackWorkspace.organization.id))
      .isNotEmpty()
  }

  private fun mockSlackRequest() {
    val responseString =
      """
      {
          "access_token": "valid_token",
          "team": {
              "id": "${testData.slackWorkspace.slackTeamId}",
              "name": "Test Team"
          },
          "error": null
      }
      """.trimIndent()

    val response = ResponseEntity(responseString, HttpStatus.OK)

    `when`(
      restTemplate.exchange(
        anyString(),
        any<HttpMethod>(),
        any(),
        eq(String::class.java),
      ),
    ).thenReturn(response)
  }
}
