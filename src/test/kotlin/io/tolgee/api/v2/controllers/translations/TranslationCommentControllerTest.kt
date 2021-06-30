package io.tolgee.api.v2.controllers.translations

import io.tolgee.annotations.ProjectJWTAuthTestMethod
import io.tolgee.assertions.Assertions.assertThat
import io.tolgee.controllers.ProjectAuthControllerTest
import io.tolgee.development.testDataBuilder.data.TranslationCommentsTestData
import io.tolgee.dtos.request.TranslationCommentDto
import io.tolgee.fixtures.*
import io.tolgee.model.enums.TranslationCommentState
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Test
import java.math.BigDecimal

@SpringBootTest
@AutoConfigureMockMvc
class TranslationCommentControllerTest : ProjectAuthControllerTest("/v2/projects/") {

    lateinit var testData: TranslationCommentsTestData

    @BeforeMethod
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
                node("state").isEqualTo("RESOLUTION_NOT_NEEDED")
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
                    node("state").isEqualTo("RESOLUTION_NOT_NEEDED")
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
                        state = TranslationCommentState.RESOLUTION_NOT_NEEDED
                )
        ).andIsCreated.andAssertThatJson {
            node("id").isValidId
            node("text").isEqualTo("Test")
            node("state").isEqualTo("RESOLUTION_NOT_NEEDED")
            node("author.username").isEqualTo("franta")
            node("createdAt").isNumber.isGreaterThan(BigDecimal(1624985181827))
            node("updatedAt").isNumber.isGreaterThan(BigDecimal(1624985181827))
        }
    }

    @ProjectJWTAuthTestMethod
    @Test
    fun `does not create comment if not valid state`() {
        performProjectAuthPost(
                "translations/${testData.translation.id}/comments",
                mapOf(
                        "text" to "",
                        "state" to "DUMMY"
                )
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
                        "state" to "RESOLVED"
                )
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
                        "state" to "RESOLVED"
                )
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
                        state = TranslationCommentState.RESOLVED
                )
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
                        state = TranslationCommentState.RESOLVED
                )
        ).andIsBadRequest.andPrettyPrint
    }

    @ProjectJWTAuthTestMethod
    @Test
    fun `updates comment state`() {
        performProjectAuthPut(
                "translations/${testData.translation.id}/comments/${testData.firstComment.id}/set-state/RESOLVED",
                null
        ).andAssertThatJson {
            node("text").isEqualTo("First comment")
            node("state").isEqualTo("RESOLVED")
        }
    }

    @ProjectJWTAuthTestMethod
    @Test
    fun `deletes comment`() {
        performProjectAuthDelete(
                "translations/${testData.translation.id}/comments/${testData.firstComment.id}",
                null
        ).andIsOk

        assertThat(translationCommentService.find(testData.firstComment.id)).isNull()
        assertThat(translationCommentService.find(testData.secondComment.id)).isNotNull
    }

    @ProjectJWTAuthTestMethod
    @Test
    fun `does not delete when not manager and not author`() {
        userAccount = testData.pepa
        performProjectAuthDelete(
                "translations/${testData.translation.id}/comments/${testData.firstComment.id}",
                null
        ).andIsBadRequest.andPrettyPrint
    }
}
