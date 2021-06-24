package io.tolgee.api.v2.controllers

import io.tolgee.annotations.ProjectJWTAuthTestMethod
import io.tolgee.controllers.ProjectAuthControllerTest
import io.tolgee.development.testDataBuilder.data.TranslationsTestData
import io.tolgee.fixtures.*
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Test
import java.math.BigDecimal

@SpringBootTest
@AutoConfigureMockMvc
class V2TranslationsControllerTest : ProjectAuthControllerTest("/v2/projects/") {

    lateinit var testData: TranslationsTestData

    @BeforeMethod
    fun setup() {
        testData = TranslationsTestData()
        this.projectSupplier = { testData.project }
    }


    @ProjectJWTAuthTestMethod
    @Test
    fun `returns correct data`() {
        testDataService.saveTestData(testData.root)
        userAccount = testData.user
        performProjectAuthGet("/translations").andPrettyPrint.andIsOk.andAssertThatJson {
            node("page.totalElements").isNumber.isGreaterThan(BigDecimal(100))
            node("page.size").isEqualTo(20)
            node("_embedded.keys") {
                isArray.hasSize(20)
                node("[0]") {
                    node("keyName").isEqualTo("A key")
                    node("keyId").isValidId
                    node("translations.de") {
                        node("id").isValidId
                        node("text").isEqualTo("Z translation")
                    }
                    node("translations").isObject.doesNotContainKey("en")
                }
                node("[19]") {
                    node("keyName").isEqualTo("key 18")
                    node("keyId").isValidId
                    node("translations.de") {
                        node("id").isValidId
                        node("text").isEqualTo("I am key 18's german translation.")
                    }
                    node("translations.en") {
                        node("id").isValidId
                        node("text").isEqualTo("I am key 18's english translation.")
                    }
                }
            }
        }
    }

    @ProjectJWTAuthTestMethod
    @Test
    fun `sorts data by translation text`() {
        testDataService.saveTestData(testData.root)
        userAccount = testData.user
        performProjectAuthGet("/translations?sort=translations.de.text,asc").andPrettyPrint.andIsOk.andAssertThatJson {
            node("_embedded.keys[0].keyName").isEqualTo("Z key")
        }
    }

    @ProjectJWTAuthTestMethod
    @Test
    fun `selects languages`() {
        testDataService.saveTestData(testData.root)
        userAccount = testData.user
        performProjectAuthGet("/translations?languages=en").andPrettyPrint.andIsOk.andAssertThatJson {
            node("_embedded.keys[10].translations").isObject
                    .doesNotContainKey("de").containsKey("en")
        }
    }

    @ProjectJWTAuthTestMethod
    @Test
    fun `filters by keyName`() {
        testDataService.saveTestData(testData.root)
        userAccount = testData.user
        performProjectAuthGet("/translations?keyName=key 18").andPrettyPrint.andIsOk.andAssertThatJson {
            node("_embedded.keys") {
                isArray.hasSize(1)
                node("[0].keyName").isEqualTo("key 18")
            }
            node("page.totalElements").isEqualTo(1)
        }
    }

    @ProjectJWTAuthTestMethod
    @Test
    fun `filters by keyId`() {
        testDataService.saveTestData(testData.root)
        userAccount = testData.user
        performProjectAuthGet("/translations?keyName=key 18").andPrettyPrint.andIsOk.andAssertThatJson {
            node("_embedded.keys") {
                isArray.hasSize(1)
                node("[0].keyName").isEqualTo("key 18")
            }
            node("page.totalElements").isEqualTo(1)
        }
    }

    @ProjectJWTAuthTestMethod
    @Test
    fun `filters by keyName containing dot`() {
        testDataService.saveTestData(testData.root)
        userAccount = testData.user
        performProjectAuthGet("/translations?keyName=key.with.dots").andPrettyPrint.andIsOk.andAssertThatJson {
            node("_embedded.keys") {
                isArray.hasSize(1)
                node("[0].keyName").isEqualTo("key 18")
            }
            node("page.totalElements").isEqualTo(1)
        }
    }
}
