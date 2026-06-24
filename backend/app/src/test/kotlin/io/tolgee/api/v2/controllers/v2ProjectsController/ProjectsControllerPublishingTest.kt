package io.tolgee.api.v2.controllers.v2ProjectsController

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
import io.tolgee.fixtures.node
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import io.tolgee.testing.assert
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
@AutoConfigureMockMvc
class ProjectsControllerPublishingTest : ProjectAuthControllerTest("/v2/projects/") {
  private lateinit var testData: ProjectPublishingTestData

  @BeforeEach
  fun setup() {
    testData = ProjectPublishingTestData()
    testDataService.saveTestData(testData.root)
    projectSupplier = { testData.project }
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
