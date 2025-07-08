package io.tolgee.ee.api.v2.controllers

import io.tolgee.ProjectAuthControllerTest
import io.tolgee.development.testDataBuilder.data.SuggestionsTestData
import io.tolgee.ee.data.translationSuggestion.CreateTranslationSuggestionRequest
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsForbidden
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.node
import io.tolgee.model.enums.SuggestionsMode
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class SuggestionControllerTest : ProjectAuthControllerTest("/v2/projects/") {
  lateinit var testData: SuggestionsTestData

  fun initTestData(suggestionsMode: SuggestionsMode = SuggestionsMode.DISABLED) {
    testData = SuggestionsTestData(suggestionsMode)
    projectSupplier = { testData.relatedProject.self }
    testDataService.saveTestData(testData.root)
    userAccount = testData.projectReviewer.self
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `returns list of suggestions`() {
    initTestData()
    performProjectAuthGet("language/${testData.czechLanguage.tag}/key/${testData.keys[0].self.id}/suggestion")
      .andAssertThatJson {
        node("_embedded.suggestions") {
          node("[0].translation").isEqualTo("Navržený překlad 0-1")
          node("[0].author.username").isEqualTo("translator@test.com")
          node("[0].author.name").isEqualTo("Project translator")
        }
        node("_embedded.suggestions") {
          node("[1].translation").isEqualTo("Navržený překlad 0-2")
          node("[1].author.username").isEqualTo("reviewer@test.com")
          node("[1].author.name").isEqualTo("Project reviewer")
        }
        node("page.totalElements").isNumber.isEqualTo(BigDecimal(2))
      }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `accepts suggestion`() {
    initTestData()
    performProjectAuthPut(
      "language/${testData.czechLanguage.tag}/key/${testData.keys[0].self.id}/suggestion/${testData.czechSuggestions[0].self.id}/accept"
    )
      .andAssertThatJson {
        node("accepted") {
          node("translation").isEqualTo("Navržený překlad 0-1")
          node("author.username").isEqualTo("translator@test.com")
          node("state").isEqualTo("ACCEPTED")
        }
        node("declined").isArray.hasSize(0)
      }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `accepts suggestion and declines other`() {
    initTestData()
    performProjectAuthPut(
      "language/${testData.czechLanguage.tag}/key/${testData.keys[0].self.id}/suggestion/${testData.czechSuggestions[0].self.id}/accept?declineOther=true"
    )
      .andAssertThatJson {
        node("accepted") {
          node("translation").isEqualTo("Navržený překlad 0-1")
          node("author.username").isEqualTo("translator@test.com")
          node("state").isEqualTo("ACCEPTED")
        }
        node("declined[0]").isEqualTo(testData.czechSuggestions[1].self.id)
        node("declined").isArray.hasSize(1)
      }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `translator cannot accept`() {
    initTestData()
    userAccount = testData.projectTranslator.self
    performProjectAuthPut(
      "language/${testData.czechLanguage.tag}/key/${testData.keys[0].self.id}/suggestion/${testData.czechSuggestions[0].self.id}/accept"
    ).andIsForbidden
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `can delete his own suggestion`() {
    initTestData()
    userAccount = testData.projectTranslator.self
    performProjectAuthDelete(
      "language/${testData.czechLanguage.tag}/key/${testData.keys[0].self.id}/suggestion/${testData.czechSuggestions[0].self.id}"
    ).andIsOk
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `can't delete suggestion of someone else`() {
    initTestData()
    userAccount = testData.projectTranslator.self
    performProjectAuthDelete(
      "language/${testData.czechLanguage.tag}/key/${testData.keys[0].self.id}/suggestion/${testData.czechSuggestions[1].self.id}"
    ).andIsForbidden
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `czech reviewer can accept czech suggestion`() {
    initTestData()
    userAccount = testData.czechReviewer.self
    performProjectAuthPut(
      "language/${testData.czechLanguage.tag}/key/${testData.keys[0].self.id}/suggestion/${testData.czechSuggestions[0].self.id}/accept"
    ).andIsOk
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `czech reviewer can't accept english suggestion`() {
    initTestData()
    userAccount = testData.czechReviewer.self
    performProjectAuthPut(
      "language/${testData.englishLanguage.tag}/key/${testData.keys[0].self.id}/suggestion/${testData.czechSuggestions[0].self.id}/accept"
    ).andIsForbidden
  }

  @Test()
  @ProjectJWTAuthTestMethod
  fun `czech translator can create czech suggestion`() {
    initTestData()
    userAccount = testData.czechTranslator.self
    performProjectAuthPost(
      "language/${testData.czechLanguage.tag}/key/${testData.keys[0].self.id}/suggestion",
      CreateTranslationSuggestionRequest("Nový návrh překladu")
    ).andAssertThatJson {
      node("translation").isEqualTo("Nový návrh překladu")
      node("state").isEqualTo("ACTIVE")
    }
  }

  @Test()
  @ProjectJWTAuthTestMethod
  fun `czech translator can't create english suggestion`() {
    initTestData()
    userAccount = testData.czechTranslator.self
    performProjectAuthPost(
      "language/${testData.englishLanguage.tag}/key/${testData.keys[0].self.id}/suggestion",
      CreateTranslationSuggestionRequest("New translation suggestion")
    ).andIsForbidden
  }

  @Test()
  @ProjectJWTAuthTestMethod
  fun `czech translator can't accept any suggestion`() {
    initTestData()
    userAccount = testData.czechTranslator.self
    performProjectAuthPut(
      "language/${testData.englishLanguage.tag}/key/${testData.keys[0].self.id}/suggestion/${testData.czechSuggestions[0].self.id}/accept"
    ).andIsForbidden
    performProjectAuthPut(
      "language/${testData.czechLanguage.tag}/key/${testData.keys[0].self.id}/suggestion/${testData.englishSuggestions[0].self.id}/accept"
    ).andIsForbidden
  }
}
