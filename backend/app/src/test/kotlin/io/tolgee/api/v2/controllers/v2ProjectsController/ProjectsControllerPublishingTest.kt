package io.tolgee.api.v2.controllers.v2ProjectsController

import com.posthog.server.PostHog
import io.tolgee.ProjectAuthControllerTest
import io.tolgee.activity.data.ActivityType
import io.tolgee.activity.data.PropertyModification
import io.tolgee.constants.Message
import io.tolgee.development.testDataBuilder.data.ProjectPublishingTestData
import io.tolgee.dtos.request.project.SetProjectPublicRequest
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andHasErrorMessage
import io.tolgee.fixtures.andIsForbidden
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.assertPostHogEventReported
import io.tolgee.fixtures.node
import io.tolgee.fixtures.postHogEventsCapturedSoFar
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
  fun clean() {
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
