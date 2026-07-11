package io.tolgee.ee.api.v2.controllers.translationMemory

import io.tolgee.ProjectAuthControllerTest
import io.tolgee.constants.Feature
import io.tolgee.development.testDataBuilder.data.TranslationMemoryTestData
import io.tolgee.ee.component.PublicEnabledFeaturesProvider
import io.tolgee.ee.data.translationMemory.AssignSharedTranslationMemoryRequest
import io.tolgee.ee.data.translationMemory.UpdateProjectTranslationMemoryAssignmentRequest
import io.tolgee.fixtures.andIsForbidden
import io.tolgee.fixtures.andIsOk
import io.tolgee.model.enums.Scope
import io.tolgee.testing.annotations.ProjectApiKeyAuthTestMethod
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
@AutoConfigureMockMvc
class ProjectTranslationMemoryControllerPermissionsTest : ProjectAuthControllerTest("/v2/projects/") {
  @Autowired
  private lateinit var enabledFeaturesProvider: PublicEnabledFeaturesProvider

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
