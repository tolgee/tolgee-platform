package io.tolgee.ee.api.v2.controllers.translationMemory

import io.tolgee.ProjectAuthControllerTest
import io.tolgee.constants.Feature
import io.tolgee.development.testDataBuilder.data.TranslationMemoryTestData
import io.tolgee.ee.component.PublicEnabledFeaturesProvider
import io.tolgee.ee.data.translationMemory.AssignSharedTranslationMemoryRequest
import io.tolgee.ee.data.translationMemory.UpdateProjectTranslationMemoryAssignmentRequest
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsBadRequest
import io.tolgee.fixtures.andIsForbidden
import io.tolgee.fixtures.andIsOk
import io.tolgee.model.enums.Scope
import io.tolgee.repository.translationMemory.TranslationMemoryEntryRepository
import io.tolgee.testing.annotations.ProjectApiKeyAuthTestMethod
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
@AutoConfigureMockMvc
class ProjectTranslationMemoryControllerTest : ProjectAuthControllerTest("/v2/projects/") {
  @Autowired
  private lateinit var enabledFeaturesProvider: PublicEnabledFeaturesProvider

  @Autowired
  private lateinit var translationMemoryEntryRepository: TranslationMemoryEntryRepository

  lateinit var testData: TranslationMemoryTestData

  @BeforeEach
  fun setup() {
    enabledFeaturesProvider.forceEnabled = setOf(Feature.TRANSLATION_MEMORY)
    testData = TranslationMemoryTestData()
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
    projectSupplier = { testData.projectWithTm }
  }

  @AfterEach
  fun cleanup() {
    testDataService.cleanTestData(testData.root)
    userAccount = null
    enabledFeaturesProvider.forceEnabled = null
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `lists project TM assignments`() {
    performAuthGet("/v2/projects/${project.id}/translation-memories")
      .andIsOk
      .andAssertThatJson {
        // projectTm + sharedTm + sharedTmWithPenalty + sharedTmWithOverride + sharedTmReviewedOnly
        // + multiProjectSharedTm
        node("_embedded.translationMemoryAssignments").isArray.hasSize(6)
      }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `assigns shared TM to project with custom config`() {
    val unassignedId = testData.unassignedSharedTm.id
    val request =
      AssignSharedTranslationMemoryRequest().apply {
        readAccess = true
        writeAccess = false
        priority = 5
      }
    performAuthPost("/v2/projects/${project.id}/translation-memories/$unassignedId", request)
      .andIsOk
      .andAssertThatJson {
        node("translationMemoryId").isEqualTo(unassignedId)
        node("readAccess").isEqualTo(true)
        node("writeAccess").isEqualTo(false)
        node("priority").isEqualTo(5)
      }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `assigning already-assigned TM returns 400`() {
    // testData.sharedTm is already assigned to projectWithTm
    performAuthPost(
      "/v2/projects/${project.id}/translation-memories/${testData.sharedTm.id}",
      AssignSharedTranslationMemoryRequest(),
    ).andIsBadRequest
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `assigning shared TM with mismatched base language is rejected`() {
    // testData.mismatchedBaseSharedTm has `fr` source; projectWithTm has `en`.
    performAuthPost(
      "/v2/projects/${project.id}/translation-memories/${testData.mismatchedBaseSharedTm.id}",
      AssignSharedTranslationMemoryRequest(),
    ).andIsBadRequest
      .andAssertThatJson {
        node("code").isEqualTo("translation_memory_base_language_mismatch")
      }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `unassigns shared TM from project`() {
    val preAssignedSharedTmId = testData.sharedTm.id
    performAuthDelete("/v2/projects/${project.id}/translation-memories/$preAssignedSharedTmId").andIsOk

    performAuthGet("/v2/projects/${project.id}/translation-memories")
      .andIsOk
      .andAssertThatJson {
        // projectTm + two penalty-fixture shared TMs + reviewed-only shared TM
        // + multiProjectSharedTm remain.
        node("_embedded.translationMemoryAssignments").isArray.hasSize(5)
        node("_embedded.translationMemoryAssignments[0].type").isEqualTo("PROJECT")
      }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `unassign leaves shared and project TM entries unchanged`() {
    val preAssignedSharedTmId = testData.sharedTm.id
    val projectTmId = testData.projectTm.id
    val beforeCount = translationMemoryEntryRepository.findByTranslationMemoryId(projectTmId).size

    performAuthDelete(
      "/v2/projects/${project.id}/translation-memories/$preAssignedSharedTmId",
    ).andIsOk

    // Shared TM entries untouched (test data has 3: Hello world de+fr, Thank you de)
    assertThat(translationMemoryEntryRepository.findByTranslationMemoryId(preAssignedSharedTmId)).hasSize(3)
    // Project TM entries unchanged
    assertThat(translationMemoryEntryRepository.findByTranslationMemoryId(projectTmId)).hasSize(beforeCount)
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `cannot unassign own project TM`() {
    val projectTmId = testData.projectTm.id
    performAuthDelete("/v2/projects/${project.id}/translation-memories/$projectTmId").andIsBadRequest
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `updates assignment priority and access flags`() {
    val preAssignedSharedTmId = testData.sharedTm.id
    val update =
      UpdateProjectTranslationMemoryAssignmentRequest().apply {
        priority = 10
        readAccess = false
      }
    performAuthPut("/v2/projects/${project.id}/translation-memories/$preAssignedSharedTmId", update)
      .andIsOk
      .andAssertThatJson {
        node("priority").isEqualTo(10)
        node("readAccess").isEqualTo(false)
        node("writeAccess").isEqualTo(true)
      }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `does not assign when feature disabled`() {
    enabledFeaturesProvider.forceEnabled = emptySet()
    val unassignedId = testData.unassignedSharedTm.id
    performAuthPost(
      "/v2/projects/${project.id}/translation-memories/$unassignedId",
      AssignSharedTranslationMemoryRequest(),
    ).andIsBadRequest
  }

  // ---------- Permission tests ----------

  @ProjectApiKeyAuthTestMethod(scopes = [Scope.TRANSLATIONS_VIEW])
  fun `TRANSLATIONS_VIEW scope can list assignments`() {
    performProjectAuthGet("translation-memories").andIsOk
  }

  @ProjectApiKeyAuthTestMethod(scopes = [Scope.TRANSLATIONS_VIEW])
  fun `TRANSLATIONS_VIEW scope cannot assign shared TM`() {
    performProjectAuthPost(
      "translation-memories/${testData.unassignedSharedTm.id}",
      AssignSharedTranslationMemoryRequest(),
    ).andIsForbidden
  }

  @ProjectApiKeyAuthTestMethod(scopes = [Scope.TRANSLATIONS_EDIT])
  fun `TRANSLATIONS_EDIT scope alone cannot assign shared TM`() {
    performProjectAuthPost(
      "translation-memories/${testData.unassignedSharedTm.id}",
      AssignSharedTranslationMemoryRequest(),
    ).andIsForbidden
  }

  @ProjectApiKeyAuthTestMethod(scopes = [Scope.TRANSLATIONS_VIEW])
  fun `TRANSLATIONS_VIEW scope cannot unassign shared TM`() {
    performProjectAuthDelete("translation-memories/${testData.sharedTm.id}").andIsForbidden
  }

  @ProjectApiKeyAuthTestMethod(scopes = [Scope.TRANSLATIONS_VIEW])
  fun `TRANSLATIONS_VIEW scope cannot update assignment`() {
    performProjectAuthPut(
      "translation-memories/${testData.sharedTm.id}",
      UpdateProjectTranslationMemoryAssignmentRequest().apply { priority = 7 },
    ).andIsForbidden
  }

  @ProjectApiKeyAuthTestMethod(scopes = [Scope.PROJECT_EDIT])
  fun `PROJECT_EDIT scope can assign shared TM`() {
    performProjectAuthPost(
      "translation-memories/${testData.unassignedSharedTm.id}",
      AssignSharedTranslationMemoryRequest(),
    ).andIsOk
  }
}
