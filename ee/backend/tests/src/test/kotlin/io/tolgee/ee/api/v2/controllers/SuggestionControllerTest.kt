package io.tolgee.ee.api.v2.controllers

import io.tolgee.ProjectAuthControllerTest
import io.tolgee.development.testDataBuilder.data.SuggestionsTestData
import io.tolgee.dtos.request.key.ComplexEditKeyDto
import io.tolgee.ee.data.translationSuggestion.CreateTranslationSuggestionRequest
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsBadRequest
import io.tolgee.fixtures.andIsForbidden
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.node
import io.tolgee.model.enums.SuggestionsMode
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class SuggestionControllerTest : ProjectAuthControllerTest("/v2/projects/") {
  lateinit var testData: SuggestionsTestData

  fun initTestData(suggestionsMode: SuggestionsMode = SuggestionsMode.ENABLED) {
    testData = SuggestionsTestData(suggestionsMode)
    projectSupplier = { testData.relatedProject.self }
    testDataService.saveTestData(testData.root)
    userAccount = testData.projectReviewer.self
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `returns list of suggestions`() {
    initTestData()
    performProjectAuthGet("languages/${testData.czechLanguage.id}/key/${testData.keys[0].self.id}/suggestion")
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
  fun `creates suggestion`() {
    initTestData()
    performProjectAuthPost(
      "languages/${testData.czechLanguage.id}/key/${testData.keys[0].self.id}/suggestion",
      CreateTranslationSuggestionRequest(
        translation = "New suggestion",
      ),
    ).andIsOk.andAssertThatJson {
      node("translation").isEqualTo("New suggestion")
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `refuses plain suggestion on plural key`() {
    initTestData()
    performProjectAuthPost(
      "languages/${testData.czechLanguage.id}/key/${testData.pluralKey.self.id}/suggestion",
      CreateTranslationSuggestionRequest(
        translation = "New suggestion",
      ),
    ).andIsBadRequest.andAssertThatJson {
      node("code").isEqualTo("invalid_plural_form")
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `accepts suggestion`() {
    initTestData()
    performProjectAuthPut(
      "languages/${testData.czechLanguage.id}/key/${testData.keys[0].self.id}/suggestion/${testData.czechSuggestions[0].self.id}/accept",
    ).andAssertThatJson {
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
  fun `can't accept plain suggestion on plural key`() {
    initTestData()
    userAccount = testData.user
    val firstKey = testData.keys[0].self
    performProjectAuthPut(
      "keys/${firstKey.id}/complex-update",
      ComplexEditKeyDto(
        name = firstKey.name,
        isPlural = true,
      ),
    ).andIsOk
    performProjectAuthPut(
      "languages/${testData.czechLanguage.id}/key/${testData.keys[0].self.id}/suggestion/${testData.czechSuggestions[0].self.id}/accept",
    ).andIsBadRequest
      .andAssertThatJson {
        node("code").isEqualTo("suggestion_must_be_plural")
      }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `can't accept plural suggestion on plain key`() {
    initTestData()
    userAccount = testData.user
    val pluralKey = testData.pluralKey.self
    performProjectAuthPut(
      "keys/${pluralKey.id}/complex-update",
      ComplexEditKeyDto(
        name = pluralKey.name,
        isPlural = false,
      ),
    ).andIsOk

    performProjectAuthPut(
      "languages/${testData.czechLanguage.id}/key/${pluralKey.id}/suggestion/${testData.pluralSuggestion.self.id}/accept",
    ).andIsBadRequest
      .andAssertThatJson {
        node("code").isEqualTo("suggestion_cant_be_plural")
      }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `accepts suggestion and declines other`() {
    initTestData()
    performProjectAuthPut(
      "languages/${testData.czechLanguage.id}/key/${testData.keys[0].self.id}/suggestion/${testData.czechSuggestions[0].self.id}/accept?declineOther=true",
    ).andAssertThatJson {
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
  fun `accepts suggestion and keeps the reviewed state`() {
    initTestData()
    val firstKey = testData.keys[0].self
    performProjectAuthPut(
      "languages/${testData.czechLanguage.id}/key/${firstKey.id}/suggestion/${testData.czechSuggestions[0].self.id}/accept",
    ).andIsOk
    performProjectAuthGet("/translations?sort=id&filterKeyId=${firstKey.id}")
      .andIsOk
      .andAssertThatJson {
        node("_embedded.keys[0].translations.cs") {
          node("text").isEqualTo("Navržený překlad 0-1")
          node("state").isEqualTo("REVIEWED")
        }
      }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `translator cannot accept`() {
    initTestData()
    userAccount = testData.projectTranslator.self
    performProjectAuthPut(
      "languages/${testData.czechLanguage.id}/key/${testData.keys[0].self.id}/suggestion/${testData.czechSuggestions[0].self.id}/accept",
    ).andIsForbidden
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `can delete his own suggestion`() {
    initTestData()
    userAccount = testData.projectTranslator.self
    performProjectAuthDelete(
      "languages/${testData.czechLanguage.id}/key/${testData.keys[0].self.id}/suggestion/${testData.czechSuggestions[0].self.id}",
    ).andIsOk
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `can't delete suggestion of someone else`() {
    initTestData()
    userAccount = testData.projectTranslator.self
    performProjectAuthDelete(
      "languages/${testData.czechLanguage.id}/key/${testData.keys[0].self.id}/suggestion/${testData.czechSuggestions[1].self.id}",
    ).andIsForbidden
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `czech reviewer can accept czech suggestion`() {
    initTestData()
    userAccount = testData.czechReviewer.self
    performProjectAuthPut(
      "languages/${testData.czechLanguage.id}/key/${testData.keys[0].self.id}/suggestion/${testData.czechSuggestions[0].self.id}/accept",
    ).andIsOk
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `czech reviewer can't accept english suggestion`() {
    initTestData()
    userAccount = testData.czechReviewer.self
    performProjectAuthPut(
      "languages/${testData.englishLanguage.id}/key/${testData.keys[0].self.id}/suggestion/${testData.englishSuggestions[0].self.id}/accept",
    ).andIsForbidden
  }

  @Test()
  @ProjectJWTAuthTestMethod
  fun `czech translator can create czech suggestion`() {
    initTestData()
    userAccount = testData.czechTranslator.self
    performProjectAuthPost(
      "languages/${testData.czechLanguage.id}/key/${testData.keys[0].self.id}/suggestion",
      CreateTranslationSuggestionRequest("Nový návrh překladu"),
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
      "languages/${testData.englishLanguage.id}/key/${testData.keys[0].self.id}/suggestion",
      CreateTranslationSuggestionRequest("New translation suggestion"),
    ).andIsForbidden
  }

  @Test()
  @ProjectJWTAuthTestMethod
  fun `czech translator can't accept any suggestion`() {
    initTestData()
    userAccount = testData.czechTranslator.self
    performProjectAuthPut(
      "languages/${testData.englishLanguage.id}/key/${testData.keys[0].self.id}/suggestion/${testData.englishSuggestions[0].self.id}/accept",
    ).andIsForbidden
    performProjectAuthPut(
      "languages/${testData.czechLanguage.id}/key/${testData.keys[0].self.id}/suggestion/${testData.czechSuggestions[0].self.id}/accept",
    ).andIsForbidden
  }
}
