package io.tolgee.api.v2.controllers.v2ProjectsController

import com.posthog.server.PostHog
import io.tolgee.ProjectAuthControllerTest
import io.tolgee.activity.data.ActivityType
import io.tolgee.activity.data.PropertyModification
import io.tolgee.constants.Message
import io.tolgee.development.testDataBuilder.data.ProjectPublishingTestData
import io.tolgee.dtos.request.project.CommunityPageRequest
import io.tolgee.dtos.request.project.SetProjectPublicRequest
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andHasErrorMessage
import io.tolgee.fixtures.andIsBadRequest
import io.tolgee.fixtures.andIsForbidden
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.assertPostHogEventReported
import io.tolgee.fixtures.node
import io.tolgee.fixtures.postHogEventsCapturedSoFar
import io.tolgee.model.enums.SuggestionsMode
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import io.tolgee.testing.assert
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.HttpHeaders

@SpringBootTest
@AutoConfigureMockMvc
class ProjectsControllerPublishingTest : ProjectAuthControllerTest("/v2/projects/") {
  private lateinit var testData: ProjectPublishingTestData

  @Autowired
  private lateinit var postHog: PostHog

  @BeforeEach
  fun setup() {
    Mockito.reset(postHog)
    testData = ProjectPublishingTestData()
    testDataService.saveTestData(testData.root)
    projectSupplier = { testData.project }
  }

  @AfterEach
  fun cleanup() {
    testDataService.cleanTestData(testData.root)
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `reports a business event for each flip`() {
    userAccount = testData.owner
    performProjectAuthPut("/publishing", SetProjectPublicRequest(public = true)).andIsOk
    assertPostHogEventReported(postHog, "PROJECT_PUBLIC_SETTINGS_CHANGE") {
      it["public"].assert.isEqualTo(true)
      (it["\$groups"] as Map<*, *>)["project"].assert.isEqualTo(testData.project.id)
      it["organizationId"].assert.isEqualTo(testData.project.organizationOwner.id)
    }
    performProjectAuthPut("/publishing", SetProjectPublicRequest(public = false)).andIsOk
    assertPostHogEventReported(postHog, "PROJECT_PUBLIC_SETTINGS_CHANGE") {
      it["public"].assert.isEqualTo(false)
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `setting the current value again reports no business event`() {
    userAccount = testData.owner
    assertThat(testData.project.public).isFalse()
    performProjectAuthPut("/publishing", SetProjectPublicRequest(public = false)).andIsOk
    performProjectAuthPut("/publishing", SetProjectPublicRequest(public = true)).andIsOk
    assertPostHogEventReported(postHog, "PROJECT_PUBLIC_SETTINGS_CHANGE") { it["public"].assert.isEqualTo(true) }
    performProjectAuthPut("/publishing", SetProjectPublicRequest(public = true)).andIsOk
    performProjectAuthPut("/publishing", SetProjectPublicRequest(public = false)).andIsOk
    assertPostHogEventReported(postHog, "PROJECT_PUBLIC_SETTINGS_CHANGE") { it["public"].assert.isEqualTo(false) }
    postHogEventsCapturedSoFar(postHog, "PROJECT_PUBLIC_SETTINGS_CHANGE").assert.isEqualTo(2)
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `owner makes the project public`() {
    userAccount = testData.owner
    performProjectAuthPut("/publishing", SetProjectPublicRequest(public = true)).andIsOk.andAssertThatJson {
      node("public").isEqualTo(true)
      node("useQaChecks").isEqualTo(false)
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `owner makes the project private again`() {
    userAccount = testData.owner
    performProjectAuthPut("/publishing", SetProjectPublicRequest(public = true)).andIsOk
    performProjectAuthPut("/publishing", SetProjectPublicRequest(public = false)).andIsOk.andAssertThatJson {
      node("public").isEqualTo(false)
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `no-op flip from default keeps it private`() {
    userAccount = testData.owner
    performProjectAuthPut("/publishing", SetProjectPublicRequest(public = false)).andIsOk.andAssertThatJson {
      node("public").isEqualTo(false)
    }
    publicModifications().assert.isEmpty()
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `no-op flip from public keeps it public`() {
    userAccount = testData.owner
    performProjectAuthPut("/publishing", SetProjectPublicRequest(public = true)).andIsOk
    performProjectAuthPut("/publishing", SetProjectPublicRequest(public = true)).andIsOk.andAssertThatJson {
      node("public").isEqualTo(true)
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `non-owner with MANAGE cannot publish`() {
    userAccount = testData.manager
    performProjectAuthPut("/publishing", SetProjectPublicRequest(public = true))
      .andIsForbidden
      .andHasErrorMessage(Message.USER_IS_NOT_OWNER_OF_ORGANIZATION)
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `server admin can publish`() {
    userAccount = testData.serverAdmin
    performProjectAuthPut("/publishing", SetProjectPublicRequest(public = true)).andIsOk.andAssertThatJson {
      node("public").isEqualTo(true)
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `records an activity revision for the flip`() {
    userAccount = testData.owner
    performProjectAuthPut("/publishing", SetProjectPublicRequest(public = true)).andIsOk
    assertPublicActivityRecorded()
  }

  private fun assertPublicActivityRecorded() {
    val publicModifications = publicModifications()
    publicModifications.assert.hasSize(1)
    val publicModification = publicModifications.single()["public"]!!
    publicModification.old.assert.isEqualTo(false)
    publicModification.new.assert.isEqualTo(true)
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `opening the project to the community turns suggestions on and closing leaves them on`() {
    userAccount = testData.owner
    performProjectAuthPut("/publishing", SetProjectPublicRequest(public = true)).andIsOk.andAssertThatJson {
      node("suggestionsMode").isEqualTo("ENABLED")
    }
    performProjectAuthPut("/publishing", SetProjectPublicRequest(public = false)).andIsOk.andAssertThatJson {
      node("suggestionsMode").isEqualTo("ENABLED")
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `re-publishing an already public project does not touch suggestions`() {
    userAccount = testData.owner
    performProjectAuthPut("/publishing", SetProjectPublicRequest(public = true)).andIsOk
    executeInNewTransaction { projectService.get(testData.project.id).suggestionsMode = SuggestionsMode.DISABLED }
    performProjectAuthPut("/publishing", SetProjectPublicRequest(public = true)).andIsOk.andAssertThatJson {
      node("suggestionsMode").isEqualTo("DISABLED")
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `owner sets the community page and it survives closing the project`() {
    userAccount = testData.owner
    performProjectAuthPut("/publishing", SetProjectPublicRequest(public = true)).andIsOk
    performProjectAuthPut("/community-page", communityPage()).andIsOk.andAssertThatJson {
      node("communityDescription").isEqualTo("We translate **everything**.")
      node("communityGuidelines").isEqualTo("Be kind.")
      node("communityShared").isEqualTo(true)
      node("communityInContext").isEqualTo(true)
      node("communityAppUrl").isEqualTo("https://app.example.com")
      node("createdAt").isIntegralNumber
    }
    performProjectAuthPut("/publishing", SetProjectPublicRequest(public = false)).andIsOk
    performAuthGet("/v2/projects/${testData.project.id}").andIsOk.andAssertThatJson {
      node("communityDescription").isEqualTo("We translate **everything**.")
      node("communityShared").isEqualTo(true)
      node("communityAppUrl").isEqualTo("https://app.example.com")
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `blank community texts and URL are cleared`() {
    userAccount = testData.owner
    performProjectAuthPut("/community-page", communityPage()).andIsOk
    performProjectAuthPut("/community-page", CommunityPageRequest(communityDescription = " ", communityAppUrl = " "))
      .andIsOk
      .andAssertThatJson {
        node("communityDescription").isNull()
        node("communityGuidelines").isNull()
        node("communityShared").isEqualTo(false)
        node("communityAppUrl").isNull()
      }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `community page rejects an overlong text and a non-http URL`() {
    userAccount = testData.owner
    performProjectAuthPut("/community-page", communityPage(communityDescription = "x".repeat(10001))).andIsBadRequest
    performProjectAuthPut("/community-page", communityPage(communityDescription = "x".repeat(10000))).andIsOk
    performProjectAuthPut("/community-page", communityPage(communityAppUrl = "javascript:alert(1)")).andIsBadRequest
    performProjectAuthPut("/community-page", communityPage(communityAppUrl = "ftp://files.example.com")).andIsBadRequest
    performProjectAuthPut("/community-page", communityPage(communityAppUrl = "http://app.example.com")).andIsOk
    performProjectAuthPut("/community-page", communityPage(communityAppUrl = " https://app.example.com "))
      .andIsOk
      .andAssertThatJson { node("communityAppUrl").isEqualTo("https://app.example.com") }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `publishing and the community page both need a super token`() {
    val headers = HttpHeaders()
    headers.setBearerAuth(jwtService.emitToken(testData.owner.id, isSuper = false))
    val url = "/v2/projects/${testData.project.id}"
    performPut("$url/publishing", SetProjectPublicRequest(public = true), headers)
      .andIsForbidden
      .andHasErrorMessage(Message.EXPIRED_SUPER_JWT_TOKEN)
    performPut("$url/community-page", communityPage(), headers)
      .andIsForbidden
      .andHasErrorMessage(Message.EXPIRED_SUPER_JWT_TOKEN)
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `server admin can set the community page`() {
    userAccount = testData.serverAdmin
    performProjectAuthPut("/community-page", communityPage()).andIsOk.andAssertThatJson {
      node("communityShared").isEqualTo(true)
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `non-owner with MANAGE cannot set the community page`() {
    userAccount = testData.manager
    performProjectAuthPut("/community-page", communityPage())
      .andIsForbidden
      .andHasErrorMessage(Message.USER_IS_NOT_OWNER_OF_ORGANIZATION)
  }

  private fun communityPage(
    communityDescription: String = "We translate **everything**.",
    communityAppUrl: String = "https://app.example.com",
  ) = CommunityPageRequest(
    communityDescription = communityDescription,
    communityGuidelines = "Be kind.",
    communityShared = true,
    communityInContext = true,
    communityAppUrl = communityAppUrl,
  )

  @Suppress("UNCHECKED_CAST")
  private fun publicModifications(): List<Map<String, PropertyModification>> {
    val result =
      entityManager
        .createQuery(
          """select ame.modifications from ActivityRevision ar
            |join ar.modifiedEntities ame
            |where ar.type = :type and ar.projectId = :projectId
          """.trimMargin(),
        ).setParameter("type", ActivityType.EDIT_PROJECT)
        .setParameter("projectId", testData.project.id)
        .resultList as List<Map<String, PropertyModification>>
    return result.filter { it.containsKey("public") }
  }
}
