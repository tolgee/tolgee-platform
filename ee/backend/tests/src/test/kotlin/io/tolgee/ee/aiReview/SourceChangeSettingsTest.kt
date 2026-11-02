package io.tolgee.ee.aiReview

import io.tolgee.constants.Feature
import io.tolgee.dtos.cacheable.ProjectDto
import io.tolgee.ee.component.PublicEnabledFeaturesProvider
import io.tolgee.ee.development.AiReviewTestData
import io.tolgee.ee.service.aiReview.SourceChangeSettingsService
import io.tolgee.exceptions.BadRequestException
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsForbidden
import io.tolgee.fixtures.andIsOk
import io.tolgee.model.Project
import io.tolgee.model.UserAccount
import io.tolgee.model.enums.Scope
import io.tolgee.model.enums.SourceChangeAction
import io.tolgee.model.enums.TranslationProtection
import io.tolgee.service.aiReview.SourceChangeSettingsProvider
import io.tolgee.testing.AuthorizedControllerTest
import io.tolgee.testing.assert
import io.tolgee.util.executeInNewTransaction
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc

@SpringBootTest
@AutoConfigureMockMvc
class SourceChangeSettingsTest : AuthorizedControllerTest() {
  @Autowired
  private lateinit var enabledFeaturesProvider: PublicEnabledFeaturesProvider

  @Autowired
  private lateinit var sourceChangeSettingsProvider: SourceChangeSettingsProvider

  @Autowired
  private lateinit var sourceChangeSettingsService: SourceChangeSettingsService

  lateinit var testData: AiReviewTestData
  lateinit var promptsEditor: UserAccount
  lateinit var projectEditor: UserAccount

  private val url get() = "/v2/projects/${testData.project.id}/source-change-settings"

  @BeforeEach
  fun setup() {
    enabledFeaturesProvider.forceEnabled = setOf(Feature.AI_REVIEW)
    testData = AiReviewTestData()
    promptsEditor = addUserWithScopes("sc_prompts_editor", Scope.PROMPTS_VIEW, Scope.PROMPTS_EDIT)
    projectEditor = addUserWithScopes("sc_project_editor", Scope.PROMPTS_VIEW, Scope.PROJECT_EDIT)
  }

  @AfterEach
  fun cleanup() {
    testDataService.cleanTestData(testData.root)
    userAccount = null
    enabledFeaturesProvider.forceEnabled = null
  }

  private fun save() {
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
  }

  @Test
  fun `GET returns the defaults and needs only PROMPTS_VIEW`() {
    save()
    userAccount = testData.promptsViewer
    performAuthGet(url).andIsOk.andAssertThatJson {
      node("aiApproved").isEqualTo("RETRANSLATE")
      node("humanEdited").isEqualTo("MARK_OUTDATED")
      node("humanReviewed").isEqualTo("MARK_OUTDATED")
      node("aiApprovedLocked").isEqualTo(false)
      node("humanReviewedLocked").isEqualTo(false)
    }
  }

  @Test
  fun `PUT stores the values with PROMPTS_EDIT and PROJECT_EDIT`() {
    save()
    performAuthPut(url, body()).andIsOk.andAssertThatJson {
      node("aiApproved").isEqualTo("MARK_OUTDATED")
      node("humanEdited").isEqualTo("RETRANSLATE")
      node("humanReviewed").isEqualTo("RETRANSLATE")
    }
    val project = project()
    project.aiApprovedSourceChangeAction.assert.isEqualTo(SourceChangeAction.MARK_OUTDATED)
    project.humanEditedSourceChangeAction.assert.isEqualTo(SourceChangeAction.RETRANSLATE)
    project.humanReviewedSourceChangeAction.assert.isEqualTo(SourceChangeAction.RETRANSLATE)
  }

  @Test
  fun `PUT passes with PROMPTS_EDIT because the scope hierarchy grants PROJECT_EDIT`() {
    save()
    userAccount = promptsEditor
    performAuthPut(url, body()).andIsOk
  }

  @Test
  fun `PUT with only PROJECT_EDIT returns 403`() {
    save()
    userAccount = projectEditor
    performAuthPut(url, body()).andIsForbidden
  }

  @Test
  fun `effective values are the defaults when AI review is off`() {
    testData.project.aiApprovedSourceChangeAction = SourceChangeAction.MARK_OUTDATED
    testData.project.humanEditedSourceChangeAction = SourceChangeAction.RETRANSLATE
    testData.project.humanReviewedSourceChangeAction = SourceChangeAction.RETRANSLATE
    save()
    enabledFeaturesProvider.forceEnabled = emptySet()

    val effective = effective()
    effective.aiApproved.assert.isEqualTo(SourceChangeAction.RETRANSLATE)
    effective.humanEdited.assert.isEqualTo(SourceChangeAction.MARK_OUTDATED)
    effective.humanReviewed.assert.isEqualTo(SourceChangeAction.MARK_OUTDATED)
  }

  @Test
  fun `under Protect reviewed humanReviewed is MARK_OUTDATED and locked`() {
    testData.project.translationProtection = TranslationProtection.PROTECT_REVIEWED
    testData.project.humanReviewedSourceChangeAction = SourceChangeAction.RETRANSLATE
    save()

    performAuthGet(url).andIsOk.andAssertThatJson {
      node("humanReviewed").isEqualTo("MARK_OUTDATED")
      node("humanReviewedLocked").isEqualTo(true)
    }
    effective().humanReviewed.assert.isEqualTo(SourceChangeAction.MARK_OUTDATED)
    project().humanReviewedSourceChangeAction.assert.isEqualTo(SourceChangeAction.RETRANSLATE)
  }

  @Test
  fun `under Protect reviewed aiApproved is RETRANSLATE and locked`() {
    testData.project.translationProtection = TranslationProtection.PROTECT_REVIEWED
    testData.project.aiApprovedSourceChangeAction = SourceChangeAction.MARK_OUTDATED
    save()

    performAuthGet(url).andIsOk.andAssertThatJson {
      node("aiApproved").isEqualTo("RETRANSLATE")
      node("aiApprovedLocked").isEqualTo(true)
    }
    effective().aiApproved.assert.isEqualTo(SourceChangeAction.RETRANSLATE)
    project().aiApprovedSourceChangeAction.assert.isEqualTo(SourceChangeAction.MARK_OUTDATED)
  }

  @Test
  fun `the service refuses to store values without AI review`() {
    save()
    enabledFeaturesProvider.forceEnabled = emptySet()
    assertThatThrownBy {
      executeInNewTransaction(platformTransactionManager) {
        sourceChangeSettingsService.update(
          testData.project.id,
          aiApproved = SourceChangeAction.MARK_OUTDATED,
          humanEdited = SourceChangeAction.RETRANSLATE,
          humanReviewed = SourceChangeAction.RETRANSLATE,
        )
      }
    }.isInstanceOf(BadRequestException::class.java)
  }

  @Test
  fun `PUT is logged with the old and new values`() {
    save()
    performAuthPut(url, body()).andIsOk

    performAuthGet("/v2/projects/${testData.project.id}/activity").andIsOk.andAssertThatJson {
      node("_embedded.activities[0].type").isEqualTo("AI_REVIEW_SETTINGS_UPDATE")
      node("_embedded.activities[0].modifiedEntities.Project[0].modifications.aiApprovedSourceChangeAction.old")
        .isEqualTo("RETRANSLATE")
      node("_embedded.activities[0].modifiedEntities.Project[0].modifications.aiApprovedSourceChangeAction.new")
        .isEqualTo("MARK_OUTDATED")
      node("_embedded.activities[0].modifiedEntities.Project[0].modifications.humanReviewedSourceChangeAction.old")
        .isEqualTo("MARK_OUTDATED")
      node("_embedded.activities[0].modifiedEntities.Project[0].modifications.humanReviewedSourceChangeAction.new")
        .isEqualTo("RETRANSLATE")
    }
  }

  private fun body() =
    mapOf(
      "aiApproved" to "MARK_OUTDATED",
      "humanEdited" to "RETRANSLATE",
      "humanReviewed" to "RETRANSLATE",
    )

  private fun effective() =
    executeInNewTransaction(platformTransactionManager) {
      sourceChangeSettingsProvider.getEffective(ProjectDto.fromEntity(project()))
    }

  private fun project(): Project =
    executeInNewTransaction(platformTransactionManager) {
      entityManager.find(Project::class.java, testData.project.id)
    }

  private fun addUserWithScopes(
    name: String,
    vararg scopes: Scope,
  ): UserAccount =
    testData.root
      .addUserAccount { username = name }
      .self
      .also { user ->
        testData.projectBuilder.addPermission {
          this.user = user
          type = null
          this.scopes = scopes.toList().toTypedArray()
        }
      }
}
