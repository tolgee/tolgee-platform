package io.tolgee.api.v2.controllers.translations

import io.tolgee.ProjectAuthControllerTest
import io.tolgee.development.testDataBuilder.data.TranslationCommentsTestData
import io.tolgee.dtos.request.translation.comment.TranslationCommentDto
import io.tolgee.dtos.request.translation.comment.TranslationCommentWithLangKeyDto
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsBadRequest
import io.tolgee.fixtures.andIsCreated
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.andPrettyPrint
import io.tolgee.fixtures.isValidId
import io.tolgee.fixtures.node
import io.tolgee.model.enums.TranslationCommentState
import io.tolgee.model.enums.TranslationState
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import io.tolgee.testing.assertions.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import java.math.BigDecimal

@SpringBootTest
@AutoConfigureMockMvc
class TranslationCommentControllerTest : ProjectAuthControllerTest("/v2/projects/") {
  lateinit var testData: TranslationCommentsTestData

  @BeforeEach
  fun setup() {
    testData = TranslationCommentsTestData()
    testDataService.saveTestData(testData.root)
    this.projectSupplier = { testData.project }
    userAccount = testData.user
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `returns correct data`() {
    performProjectAuthGet("translations/${testData.translation.id}/comments").andAssertThatJson {
      node("page.totalElements").isEqualTo(2)
      node("_embedded.translationComments[0]") {
        node("id").isValidId
        node("text").isEqualTo("First comment")
        node("state").isEqualTo("NEEDS_RESOLUTION")
        node("author.username").isEqualTo("franta")
        node("createdAt").isNumber.isGreaterThan(BigDecimal(1624985181827))
        node("updatedAt").isNumber.isGreaterThan(BigDecimal(1624985181827))
      }
    }
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `returns correct single data`() {
    performProjectAuthGet("translations/${testData.translation.id}/comments/${testData.firstComment.id}")
      .andAssertThatJson {
        node("id").isValidId
        node("text").isEqualTo("First comment")
        node("state").isEqualTo("NEEDS_RESOLUTION")
        node("author.username").isEqualTo("franta")
        node("createdAt").isNumber.isGreaterThan(BigDecimal(1624985181827))
        node("updatedAt").isNumber.isGreaterThan(BigDecimal(1624985181827))
      }
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `creates comment`() {
    performProjectAuthPost(
      "translations/${testData.translation.id}/comments",
      TranslationCommentDto(
        text = "Test",
        state = TranslationCommentState.RESOLUTION_NOT_NEEDED,
      ),
    ).andIsCreated.andAssertThatJson {
      node("id").isValidId
      node("text").isEqualTo("Test")
      node("state").isEqualTo("RESOLUTION_NOT_NEEDED")
      node("author.username").isEqualTo("franta")
      node("createdAt").isNumber.isGreaterThan(BigDecimal(1624985181827))
      node("updatedAt").isNumber.isGreaterThan(BigDecimal(1624985181827))
    }
    assertThat(translationService.find(testData.translation.id)?.state).isEqualTo(TranslationState.REVIEWED)
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `creates comment with keyId and languageId`() {
    performProjectAuthPost(
      "translations/create-comment",
      TranslationCommentWithLangKeyDto(
        keyId = testData.bKey.id,
        testData.englishLanguage.id,
        text = "Test",
        state = TranslationCommentState.RESOLUTION_NOT_NEEDED,
      ),
    ).andIsCreated.andAssertThatJson {
      node("comment") {
        node("id").isValidId
        node("text").isEqualTo("Test")
        node("state").isEqualTo("RESOLUTION_NOT_NEEDED")
        node("author.username").isEqualTo("franta")
        node("createdAt").isNumber.isGreaterThan(BigDecimal(1624985181827))
        node("updatedAt").isNumber.isGreaterThan(BigDecimal(1624985181827))
      }
      node("translation") {
        node("id").isValidId
        node("text").isEqualTo(null)
        node("state").isEqualTo("UNTRANSLATED")
      }
    }
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `does not create comment if not valid state`() {
    performProjectAuthPost(
      "translations/${testData.translation.id}/comments",
      mapOf(
        "text" to "",
        "state" to "DUMMY",
      ),
    ).andIsBadRequest.andPrettyPrint.andAssertThatJson {
      node("params[0]").isString.startsWith("Cannot deserialize value of type")
    }
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `does not create comment if not valid text`() {
    performProjectAuthPost(
      "translations/${testData.translation.id}/comments",
      mapOf(
        "text" to "",
        "state" to "RESOLVED",
      ),
    ).andIsBadRequest.andPrettyPrint.andAssertThatJson {
      node("STANDARD_VALIDATION").isObject
    }
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `does not update comment if not valid text`() {
    performProjectAuthPut(
      "translations/${testData.translation.id}/comments/${testData.firstComment.id}",
      mapOf(
        "text" to "",
        "state" to "RESOLVED",
      ),
    ).andIsBadRequest.andPrettyPrint.andAssertThatJson {
      node("STANDARD_VALIDATION").isObject
    }
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `updates comment`() {
    performProjectAuthPut(
      "translations/${testData.translation.id}/comments/${testData.firstComment.id}",
      TranslationCommentDto(
        text = "Updated",
        state = TranslationCommentState.RESOLVED,
      ),
    ).andIsOk.andAssertThatJson {
      node("id").isValidId
      node("text").isEqualTo("Updated")
      node("state").isEqualTo("RESOLVED")
      node("author.username").isEqualTo("franta")
      node("createdAt").isNumber.isGreaterThan(BigDecimal(1624985181827))
      node("updatedAt").isNumber.isGreaterThan(BigDecimal(1624985181827))
    }
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `does not update when not author`() {
    userAccount = testData.pepa
    performProjectAuthPut(
      "translations/${testData.translation.id}/comments/${testData.firstComment.id}",
      TranslationCommentDto(
        text = "Updated",
        state = TranslationCommentState.RESOLVED,
      ),
    ).andIsBadRequest.andPrettyPrint
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `updates comment state`() {
    performProjectAuthPut(
      "translations/${testData.translation.id}/comments/${testData.firstComment.id}/set-state/RESOLVED",
      null,
    ).andIsOk.andAssertThatJson {
      node("text").isEqualTo("First comment")
      node("state").isEqualTo("RESOLVED")
    }
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `deletes comment`() {
    performProjectAuthDelete(
      "translations/${testData.translation.id}/comments/${testData.firstComment.id}",
      null,
    ).andIsOk

    assertThat(translationCommentService.find(testData.firstComment.id)).isNull()
    assertThat(translationCommentService.find(testData.secondComment.id)).isNotNull
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `does not delete when doesn't have edit scope and not author`() {
    userAccount = testData.pepa
    performProjectAuthDelete(
      "translations/${testData.translation.id}/comments/${testData.firstComment.id}",
      null,
    ).andIsBadRequest.andPrettyPrint
  }
}
