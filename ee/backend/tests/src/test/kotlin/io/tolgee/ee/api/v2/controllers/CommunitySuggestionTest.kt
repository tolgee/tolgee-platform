package io.tolgee.ee.api.v2.controllers

import io.tolgee.ProjectAuthControllerTest
import io.tolgee.development.testDataBuilder.data.SuggestionsTestData
import io.tolgee.dtos.request.translation.SetTranslationsWithKeyDto
import io.tolgee.dtos.request.translation.comment.TranslationCommentWithLangKeyDto
import io.tolgee.ee.data.translationSuggestion.CreateTranslationSuggestionRequest
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsCreated
import io.tolgee.fixtures.andIsForbidden
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.node
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class CommunitySuggestionTest : ProjectAuthControllerTest("/v2/projects/") {
  private lateinit var testData: SuggestionsTestData

  @BeforeEach
  fun setup() {
    testData = SuggestionsTestData()
    projectSupplier = { testData.relatedProject.self }
    testDataService.saveTestData(testData.root)
    projectService.setPublic(testData.relatedProject.self.id, true)
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `community user can suggest on a public project`() {
    userAccount = testData.communityUser.self
    performProjectAuthPost(
      "languages/${testData.czechLanguage.id}/key/${testData.keys[0].self.id}/suggestion",
      CreateTranslationSuggestionRequest(translation = "Community suggested translation"),
    ).andIsOk
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `a language-restricted member can suggest outside their languages on a public project`() {
    userAccount = testData.czechTranslator.self
    performProjectAuthPost(
      "languages/${testData.englishLanguage.id}/key/${testData.keys[0].self.id}/suggestion",
      CreateTranslationSuggestionRequest(translation = "English suggestion from a Czech-only translator"),
    ).andIsOk
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `a view-restricted member can view outside their languages on a public project`() {
    userAccount = testData.czechTranslator.self
    performProjectAuthGet(
      "/translations?languages=en&languages=cs&filterKeyId=${testData.keys[0].self.id}",
    ).andIsOk.andAssertThatJson {
      node("_embedded.keys[0].translations.en.text").isEqualTo("Translation 0")
      node("_embedded.keys[0].translations.cs.text").isEqualTo("Překlad 0")
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `community user can comment on a public project`() {
    userAccount = testData.communityUser.self
    performProjectAuthPost(
      "translations/create-comment",
      TranslationCommentWithLangKeyDto(
        keyId = testData.keys[0].self.id,
        languageId = testData.czechLanguage.id,
        text = "Community comment",
      ),
    ).andIsCreated
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `community user cannot accept a suggestion`() {
    userAccount = testData.communityUser.self
    performProjectAuthPut(
      "languages/${testData.czechLanguage.id}/key/${testData.keys[0].self.id}" +
        "/suggestion/${testData.czechSuggestions[0].self.id}/accept",
      null,
    ).andIsForbidden
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `community user cannot reach branch merge sessions`() {
    userAccount = testData.communityUser.self
    performProjectAuthGet("branches/merge").andIsForbidden
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `a member without branch-management cannot start a branch merge`() {
    userAccount = testData.projectTranslator.self
    performProjectAuthPost("branches/merge/preview", mapOf<String, Any>()).andIsForbidden
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `community user cannot read blocking tasks`() {
    userAccount = testData.communityUser.self
    performProjectAuthGet("tasks/1/blocking-tasks").andIsForbidden
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `author emails are hidden from community but the name stays visible`() {
    val path = "languages/${testData.czechLanguage.id}/key/${testData.keys[0].self.id}/suggestion"

    userAccount = testData.communityUser.self
    performProjectAuthGet(path).andIsOk.andAssertThatJson {
      node("_embedded.suggestions[0].author.username").isEqualTo("")
      node("_embedded.suggestions[0].author.name").isEqualTo("Project translator")
    }

    userAccount = testData.user
    performProjectAuthGet(path).andIsOk.andAssertThatJson {
      node("_embedded.suggestions[0].author.username").isEqualTo("translator@test.com")
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `author emails are hidden from community in the embedded translations list`() {
    val path = "/translations?sort=id&filterKeyId=${testData.keys[0].self.id}"

    userAccount = testData.communityUser.self
    performProjectAuthGet(path).andIsOk.andAssertThatJson {
      node("_embedded.keys[0].translations.cs.suggestions[0].author.username").isEqualTo("")
      node("_embedded.keys[0].translations.cs.suggestions[0].author.name").isEqualTo("Project reviewer")
    }

    userAccount = testData.user
    performProjectAuthGet(path).andIsOk.andAssertThatJson {
      node("_embedded.keys[0].translations.cs.suggestions[0].author.username").isEqualTo("reviewer@test.com")
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `author emails are hidden from community in the activity feed and translation history`() {
    userAccount = testData.user
    performProjectAuthPut(
      "translations",
      SetTranslationsWithKeyDto(testData.keys[0].self.name, null, mutableMapOf("cs" to "Edited by member")),
    ).andIsOk

    val translationId =
      transactionTemplate.execute {
        translationService.find(testData.keys[0].self, testData.czechLanguage).get().id
      }!!

    userAccount = testData.communityUser.self
    performProjectAuthGet("activity").andIsOk.andAssertThatJson {
      node("_embedded.activities[0].author.username").isEqualTo("")
      node("_embedded.activities[0].author.name").isEqualTo("Tasks test user")
    }
    performProjectAuthGet("translations/$translationId/history").andIsOk.andAssertThatJson {
      node("_embedded.revisions[0].author.username").isEqualTo("")
      node("_embedded.revisions[0].author.name").isEqualTo("Tasks test user")
    }

    userAccount = testData.user
    performProjectAuthGet("activity").andIsOk.andAssertThatJson {
      node("_embedded.activities[0].author.username").isEqualTo("suggestionsuggestionsTestUser")
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `comment author emails are hidden from community`() {
    val translationId =
      transactionTemplate.execute {
        translationService.find(testData.keys[0].self, testData.czechLanguage).get().id
      }!!

    userAccount = testData.communityUser.self
    performProjectAuthGet("translations/$translationId/comments").andIsOk.andAssertThatJson {
      node("_embedded.translationComments[0].author.username").isEqualTo("")
      node("_embedded.translationComments[0].author.name").isEqualTo("Tasks test user")
    }

    userAccount = testData.user
    performProjectAuthGet("translations/$translationId/comments").andIsOk.andAssertThatJson {
      node("_embedded.translationComments[0].author.username").isEqualTo("suggestionsuggestionsTestUser")
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `suggesting does not turn the community user into a member`() {
    userAccount = testData.communityUser.self
    performProjectAuthPost(
      "languages/${testData.czechLanguage.id}/key/${testData.keys[0].self.id}/suggestion",
      CreateTranslationSuggestionRequest(translation = "Community suggested translation"),
    ).andIsOk
    performAuthGet("/v2/projects/${testData.relatedProject.self.id}").andIsOk.andAssertThatJson {
      node("computedPermission.origin").isEqualTo("COMMUNITY")
    }
  }
}
