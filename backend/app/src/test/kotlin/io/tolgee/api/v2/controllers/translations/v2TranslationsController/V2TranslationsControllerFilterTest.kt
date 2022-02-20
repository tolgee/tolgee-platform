package io.tolgee.api.v2.controllers.translations.v2TranslationsController

import io.tolgee.controllers.ProjectAuthControllerTest
import io.tolgee.development.testDataBuilder.data.TranslationsTestData
import io.tolgee.fixtures.andAssertError
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsBadRequest
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.andPrettyPrint
import io.tolgee.fixtures.node
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import java.math.BigDecimal

@SpringBootTest
@AutoConfigureMockMvc
class V2TranslationsControllerFilterTest : ProjectAuthControllerTest("/v2/projects/") {

  lateinit var testData: TranslationsTestData

  @BeforeEach
  fun setup() {
    testData = TranslationsTestData()
    this.projectSupplier = { testData.project }
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
    testData.generateLotOfData()
    testData.addKeysWithScreenshots()
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
    performProjectAuthGet("/translations?languages=en&filterHasScreenshot=true")
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

  @ProjectJWTAuthTestMethod
  @Test
  fun `filters by state`() {
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
    performProjectAuthGet("/translations?filterState=de,REVIEWED")
      .andPrettyPrint.andIsOk.andAssertThatJson {
        node("_embedded.keys[0].keyName").isEqualTo("A key")
        node("page.totalElements").isEqualTo(1)
      }
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `filters by multiple states`() {
    testData.addTranslationsWithStates()
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
    performProjectAuthGet("/translations?filterState=de,TRANSLATED&filterState=en,REVIEWED")
      .andPrettyPrint.andIsOk.andAssertThatJson {
        node("_embedded.keys[0].keyName").isEqualTo("state test key 2")
        node("page.totalElements").isEqualTo(1)
      }
    performProjectAuthGet("/translations?filterState=de,REVIEWED&filterState=en,REVIEWED")
      .andPrettyPrint.andIsOk.andAssertThatJson {
        node("_embedded.keys[0].keyName").isEqualTo("state test key")
        node("page.totalElements").isEqualTo(1)
      }
    performProjectAuthGet("/translations?filterState=de,UNTRANSLATED&filterState=en,REVIEWED")
      .andPrettyPrint.andIsOk.andAssertThatJson {
        node("_embedded.keys[0].keyName").isEqualTo("state test key 3")
        node("page.totalElements").isEqualTo(1)
      }
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `filters by untranslated state`() {
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
    performProjectAuthGet("/translations?filterState=de,UNTRANSLATED")
      .andPrettyPrint.andIsOk.andAssertThatJson {
        node("_embedded.keys[0].keyName").isEqualTo("Z key")
        node("page.totalElements").isEqualTo(1)
      }
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `filters by tag`() {
    testData.addFewKeysWithTags()
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
    performProjectAuthGet("/translations?filterTag=Cool tag")
      .andPrettyPrint.andIsOk.andAssertThatJson {
        node("_embedded.keys[0].keyName").isEqualTo("A key")
        node("_embedded.keys[0].keyTags[0].name").isEqualTo("Cool tag")
        node("_embedded.keys[1].keyTags[0].name").isEqualTo("Cool tag")
        node("_embedded.keys[2].keyTags[0].name").isEqualTo("Cool tag")
        node("page.totalElements").isEqualTo(3)
      }
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `filters by multiple tags`() {
    testData.addFewKeysWithTags()
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
    performProjectAuthGet("/translations?filterTag=Cool tag&filterTag=Another cool tag")
      .andPrettyPrint.andIsOk.andAssertThatJson {
        node("_embedded.keys[0].keyName").isEqualTo("A key")
        node("_embedded.keys[0].keyTags[0].name").isEqualTo("Cool tag")
        node("_embedded.keys[1].keyTags[0].name").isEqualTo("Another cool tag")
        node("_embedded.keys[2].keyTags[0].name").isEqualTo("Cool tag")
        node("page.totalElements").isEqualTo(4)
      }
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `validates filter state`() {
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
    performProjectAuthGet("/translations?filterState=de,REVIEWED,a")
      .andIsBadRequest.andAssertError.hasCode("filter_by_value_state_not_valid")

    performProjectAuthGet("/translations?filterState=de,REVIIIIIIEWED")
      .andIsBadRequest.andAssertError.hasCode("filter_by_value_state_not_valid")
  }
}
