package io.tolgee.api.v2.controllers.translations.v2TranslationsController

import io.tolgee.controllers.ProjectAuthControllerTest
import io.tolgee.development.testDataBuilder.data.TranslationsTestData
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsForbidden
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.andPrettyPrint
import io.tolgee.fixtures.isValidId
import io.tolgee.fixtures.node
import io.tolgee.model.enums.ApiScope
import io.tolgee.testing.annotations.ProjectApiKeyAuthTestMethod
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Test
import java.math.BigDecimal

@SpringBootTest
@AutoConfigureMockMvc
class V2TranslationsControllerViewTest : ProjectAuthControllerTest("/v2/projects/") {

  lateinit var testData: TranslationsTestData

  @BeforeMethod
  fun setup() {
    testData = TranslationsTestData()
    this.projectSupplier = { testData.project }
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `returns correct data`() {
    testData.generateLotOfData()
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
    performProjectAuthGet("/translations").andPrettyPrint.andIsOk.andAssertThatJson {
      node("page.totalElements").isNumber.isGreaterThan(BigDecimal(100))
      node("page.size").isEqualTo(20)
      node("selectedLanguages") {
        isArray.hasSize(2)
        node("[0].originalName").isEqualTo("English")
        node("[1].tag").isEqualTo("de")
      }
      node("_embedded.keys") {
        isArray.hasSize(20)
        node("[0]") {
          node("keyName").isEqualTo("A key")
          node("keyId").isValidId
          node("keyTags").isArray.hasSize(1)
          node("keyTags[0].name").isEqualTo("Cool tag")
          node("screenshotCount").isEqualTo(0)
          node("translations.de") {
            node("id").isValidId
            node("text").isEqualTo("Z translation")
            node("state").isEqualTo("REVIEWED")
            node("commentCount").isEqualTo(1)
          }
          node("translations").isObject.doesNotContainKey("en")
        }
        node("[19]") {
          node("keyName").isEqualTo("key 18")
          node("keyId").isValidId
          node("translations.de") {
            node("id").isValidId
            node("text").isEqualTo("I am key 18's german translation.")
            node("state").isEqualTo("TRANSLATED")
            node("commentCount").isEqualTo(0)
          }
          node("translations.en") {
            node("id").isValidId
            node("text").isEqualTo("I am key 18's english translation.")
            node("state").isEqualTo("TRANSLATED")
          }
        }
      }
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `returns correct comment counts`() {
    testData.generateCommentTestData()
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
    performProjectAuthGet("/translations").andPrettyPrint.andIsOk.andAssertThatJson {
      node("_embedded.keys[2].translations.de.commentCount").isNumber.isEqualTo(BigDecimal(5))
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `returns correct screenshot data`() {
    testData.addKeysWithScreenshots()
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
    performProjectAuthGet("/translations").andPrettyPrint.andIsOk.andAssertThatJson {
      node("_embedded.keys") {
        node("[0].screenshots").isArray.hasSize(0)
        node("[3].screenshots").isArray.hasSize(2)
        node("[3].screenshots[1].fileUrl").isString.endsWith(".jpg").startsWith("http://local")
      }
    }
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `works with cursor`() {
    testData.generateCursorTestData()
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
    var cursor = ""
    performProjectAuthGet("/translations?sort=translations.de.text&sort=keyName&size=4")
      .andPrettyPrint.andIsOk.andAssertThatJson {
        node("nextCursor").isString.satisfies { cursor = it }
      }

    performProjectAuthGet("/translations?sort=translations.de.text&size=4&sort=keyName&cursor=$cursor")
      .andPrettyPrint.andIsOk.andAssertThatJson {
        node("_embedded.keys[0].keyName").isEqualTo("f")
        node("_embedded.keys[3].keyName").isEqualTo("c")
      }
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `works with cursor and search`() {
    testData.generateCursorSearchData()
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
    var cursor = ""
    performProjectAuthGet("/translations?sort=translations.de.text&sort=keyName&size=2&search=hello")
      .andPrettyPrint.andIsOk.andAssertThatJson {
        node("_embedded.keys[0].keyName").isEqualTo("Hello")
        node("nextCursor").isString.satisfies { cursor = it }
      }

    performProjectAuthGet("/translations?sort=translations.de.text&size=2&sort=keyName&search=hello&cursor=$cursor")
      .andPrettyPrint.andIsOk.andAssertThatJson {
        node("_embedded.keys").isArray.hasSize(1)
        node("_embedded.keys[0].keyName").isEqualTo("Hello 3")
      }
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `sorts data by translation text`() {
    testData.generateLotOfData()
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
    performProjectAuthGet("/translations?sort=translations.de.text,asc")
      .andPrettyPrint.andIsOk.andAssertThatJson {
        node("_embedded.keys[0].keyName").isEqualTo("Z key")
      }
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `selects languages`() {
    testData.generateLotOfData()
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
    performProjectAuthGet("/translations?languages=en").andPrettyPrint.andIsOk.andAssertThatJson {
      node("_embedded.keys[10].translations").isObject
        .doesNotContainKey("de").containsKey("en")
      node("selectedLanguages") {
        isArray.hasSize(1)
        node("[0].tag").isEqualTo("en")
      }
    }
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `selects multiple languages`() {
    testData.generateLotOfData()
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
    performProjectAuthGet("/translations?languages=en&languages=de").andPrettyPrint.andIsOk.andAssertThatJson {
      node("_embedded.keys[10].translations").isObject
        .containsKey("de").containsKey("en")
      node("selectedLanguages") {
        isArray.hasSize(2)
        node("[0].tag").isEqualTo("de")
        node("[1].tag").isEqualTo("en")
      }
    }
  }

  @ProjectApiKeyAuthTestMethod
  @Test
  fun `works with API key`() {
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
    performProjectAuthGet("/translations").andPrettyPrint.andIsOk
      .andAssertThatJson {
        node("page.totalElements").isEqualTo(2)
      }
  }

  @ProjectApiKeyAuthTestMethod(scopes = [])
  @Test
  fun `returns all translations map forbidden`() {
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
    performProjectAuthGet("/translations/en,de").andPrettyPrint.andIsForbidden
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `returns all translations map`() {
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
    performProjectAuthGet("/translations/en,de").andPrettyPrint.andIsOk
      .andAssertThatJson { node("de").isObject }
  }

  @ProjectApiKeyAuthTestMethod(scopes = [ApiScope.TRANSLATIONS_VIEW])
  @Test
  fun `returns all translations map API key`() {
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
    performProjectAuthGet("/translations/en,de").andPrettyPrint.andIsOk
  }
}
