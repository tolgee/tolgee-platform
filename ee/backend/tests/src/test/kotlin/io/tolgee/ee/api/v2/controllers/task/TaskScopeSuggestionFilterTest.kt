package io.tolgee.ee.api.v2.controllers.task

import io.tolgee.ProjectAuthControllerTest
import io.tolgee.constants.Feature
import io.tolgee.development.testDataBuilder.builders.KeyBuilder
import io.tolgee.development.testDataBuilder.data.TaskTestData
import io.tolgee.ee.component.PublicEnabledFeaturesProvider
import io.tolgee.ee.data.task.CalculateScopeRequest
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsOk
import io.tolgee.model.enums.TaskType
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class TaskScopeSuggestionFilterTest : ProjectAuthControllerTest("/v2/projects/") {
  lateinit var testData: TaskTestData

  @Autowired
  private lateinit var enabledFeaturesProvider: PublicEnabledFeaturesProvider

  private lateinit var suggestedKey: KeyBuilder

  // the id only exists once the test data is saved
  private val suggestedKeyId get() = suggestedKey.self.id

  @BeforeEach
  fun setup() {
    testData = TaskTestData()
    testData.projectBuilder.apply {
      suggestedKey =
        addKey(null, "suggested in czech only") {
          addTranslation("en", "Translation")
          addTranslation("cs", "Překlad")
          addSuggestion {
            language = testData.czechLanguage
            author = testData.user
            translation = "Návrh"
          }
        }
    }
    projectSupplier = { testData.projectBuilder.self }
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
    enabledFeaturesProvider.forceEnabled = setOf(Feature.TASKS)
  }

  @AfterEach
  fun cleanup() {
    testDataService.cleanTestData(testData.root)
    userAccount = null
    enabledFeaturesProvider.forceEnabled = null
  }

  private fun scopeRequest(languageId: Long) =
    CalculateScopeRequest(
      languageId = languageId,
      type = TaskType.TRANSLATE,
      keys = mutableSetOf(suggestedKeyId),
    )

  @Test
  @ProjectJWTAuthTestMethod
  fun `filterHasSuggestions keeps the key only for the language it was suggested in`() {
    performProjectAuthPost(
      "tasks/calculate-scope?filterHasSuggestions=true",
      scopeRequest(testData.czechLanguage.id),
    ).andIsOk.andAssertThatJson { node("keyCount").isEqualTo(1) }

    performProjectAuthPost(
      "tasks/calculate-scope?filterHasSuggestions=true",
      scopeRequest(testData.englishLanguage.id),
    ).andIsOk.andAssertThatJson { node("keyCount").isEqualTo(0) }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `filterHasNoSuggestions drops the key only for the language it was suggested in`() {
    performProjectAuthPost(
      "tasks/calculate-scope?filterHasNoSuggestions=true",
      scopeRequest(testData.czechLanguage.id),
    ).andIsOk.andAssertThatJson { node("keyCount").isEqualTo(0) }

    performProjectAuthPost(
      "tasks/calculate-scope?filterHasNoSuggestions=true",
      scopeRequest(testData.englishLanguage.id),
    ).andIsOk.andAssertThatJson { node("keyCount").isEqualTo(1) }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `without the filter the key is in scope for both languages`() {
    performProjectAuthPost(
      "tasks/calculate-scope",
      scopeRequest(testData.czechLanguage.id),
    ).andIsOk.andAssertThatJson { node("keyCount").isEqualTo(1) }

    performProjectAuthPost(
      "tasks/calculate-scope",
      scopeRequest(testData.englishLanguage.id),
    ).andIsOk.andAssertThatJson { node("keyCount").isEqualTo(1) }
  }
}
