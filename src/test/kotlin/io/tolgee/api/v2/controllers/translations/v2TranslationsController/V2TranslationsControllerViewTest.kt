package io.tolgee.api.v2.controllers.translations.v2TranslationsController

import io.tolgee.annotations.ProjectApiKeyAuthTestMethod
import io.tolgee.annotations.ProjectJWTAuthTestMethod
import io.tolgee.controllers.ProjectAuthControllerTest
import io.tolgee.development.testDataBuilder.data.TranslationsTestData
import io.tolgee.fixtures.*
import io.tolgee.model.enums.ApiScope
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
          node("screenshotCount").isEqualTo(0)
          node("translations.de") {
            node("id").isValidId
            node("text").isEqualTo("Z translation")
            node("state").isEqualTo("REVIEWED")
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

  @ProjectJWTAuthTestMethod
  @Test
  fun `filters by keyName`() {
    testData.generateLotOfData()
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
    performProjectAuthGet("/translations?filterKeyName=key 18").andPrettyPrint.andIsOk.andAssertThatJson {
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
    testData.generateLotOfData()
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
    performProjectAuthGet("/translations?filterKeyId=${testData.aKey.id}").andPrettyPrint.andIsOk.andAssertThatJson {
      node("_embedded.keys") {
        isArray.hasSize(1)
        node("[0].keyName").isEqualTo("A key")
      }
      node("page.totalElements").isEqualTo(1)
    }
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `filters by keyName containing dot`() {
    testData.addKeyWithDot()
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
    performProjectAuthGet("/translations?filterKeyName=key.with.dots").andPrettyPrint.andIsOk.andAssertThatJson {
      node("_embedded.keys") {
        isArray.hasSize(1)
        node("[0].keyName").isEqualTo("key.with.dots")
      }
      node("page.totalElements").isEqualTo(1)
    }
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `filters untranslated any`() {
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
    performProjectAuthGet("/translations?filterUntranslatedAny=true")
      .andPrettyPrint.andIsOk.andAssertThatJson {
        node("page.totalElements").isEqualTo(2)
      }
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `filters translated any`() {
    testData.addKeyWithDot()
    testData.generateLotOfData()
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
    performProjectAuthGet("/translations?filterTranslatedAny=true&search=dot")
      .andPrettyPrint.andIsOk.andAssertThatJson {
        node("page.totalElements").isEqualTo(0)
      }
    performProjectAuthGet("/translations?filterTranslatedAny=true")
      .andPrettyPrint.andIsOk.andAssertThatJson {
        node("page.totalElements").isNumber.isGreaterThan(BigDecimal(90))
      }
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `filters translated in lang`() {
    testData.addKeyWithDot()
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
    performProjectAuthGet("/translations?filterTranslatedInLang=en")
      .andPrettyPrint.andIsOk.andAssertThatJson {
        node("_embedded.keys[0].translations.en").isNotNull
        node("page.totalElements").isEqualTo(1)
      }
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `filters untranslated in lang`() {
    testData.addKeyWithDot()
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
    performProjectAuthGet("/translations?filterUntranslatedInLang=en")
      .andPrettyPrint.andIsOk.andAssertThatJson {
        node("_embedded.keys[0].translations.de").isNotNull
        node("page.totalElements").isEqualTo(2)
      }
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `filters by hasScreenshot`() {
    testData.addKeysWithScreenshots()
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
    performProjectAuthGet("/translations?filterHasScreenshot=true")
      .andPrettyPrint.andIsOk.andAssertThatJson {
        node("_embedded.keys[0].screenshotCount").isEqualTo(2)
        node("page.totalElements").isEqualTo(2)
      }
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `filters by hasNoScreenshot`() {
    testData.addKeysWithScreenshots()
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
    performProjectAuthGet("/translations?filterHasNoScreenshot=true")
      .andPrettyPrint.andIsOk.andAssertThatJson {
        node("_embedded.keys[0].screenshotCount").isEqualTo(0)
        node("page.totalElements").isEqualTo(2)
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
