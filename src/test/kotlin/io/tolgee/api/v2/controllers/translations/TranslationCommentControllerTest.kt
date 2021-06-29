package io.tolgee.api.v2.controllers.translations

import io.tolgee.annotations.ProjectJWTAuthTestMethod
import io.tolgee.controllers.ProjectAuthControllerTest
import io.tolgee.development.testDataBuilder.data.TranslationCommentsTestData
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.isValidId
import io.tolgee.fixtures.node
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
}
