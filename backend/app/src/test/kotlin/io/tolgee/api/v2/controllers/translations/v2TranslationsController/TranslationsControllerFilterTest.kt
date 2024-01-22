package io.tolgee.api.v2.controllers.translations.v2TranslationsController

import io.tolgee.ProjectAuthControllerTest
import io.tolgee.development.testDataBuilder.data.NamespacesTestData
import io.tolgee.development.testDataBuilder.data.TranslationSourceChangeStateTestData
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
class TranslationsControllerFilterTest : ProjectAuthControllerTest("/v2/projects/") {
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
  fun `filters by complex keyName with dot and commas`() {
    testData.generateLotOfData()
    val keyName = testData.addSentenceKey().self.name
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
    performProjectAuthGet("/translations?filterKeyName=$keyName")
      .andIsOk.andAssertThatJson {
        node("_embedded.keys") {
          isArray.hasSize(1)
          node("[0].keyName").isEqualTo(keyName)
        }
        node("page.totalElements").isEqualTo(1)
      }
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `filters by multiple keyNames`() {
    testData.generateLotOfData()
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
    performProjectAuthGet("/translations?filterKeyName=key 18&filterKeyName=key 20")
      .andPrettyPrint.andIsOk.andAssertThatJson {
        node("_embedded.keys") {
          isArray.hasSize(2)
        }
      }
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `filters by namespace`() {
    val testData = NamespacesTestData()
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
    projectSupplier = { testData.projectBuilder.self }
    performProjectAuthGet("/translations?filterNamespace=&filterNamespace=ns-2")
      .andIsOk.andAssertThatJson {
        node("_embedded.keys") {
          isArray.hasSize(3)
        }
      }
    performProjectAuthGet("/translations?filterNamespace=ns-2&filterNamespace=ns-1")
      .andIsOk.andAssertThatJson {
        node("_embedded.keys") {
          isArray.hasSize(3)
        }
      }
    performProjectAuthGet("/translations?filterNamespace=ns-2&filterNamespace=")
      .andIsOk.andAssertThatJson {
        node("_embedded.keys") {
          isArray.hasSize(3)
        }
      }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `it filters by empty namespace`() {
    val testData = NamespacesTestData()
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
    projectSupplier = { testData.projectBuilder.self }
    performProjectAuthGet("/translations?filterNamespace=").andPrettyPrint.andIsOk.andAssertThatJson {
      node("_embedded.keys") {
        isArray.hasSize(2)
      }
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `it doesn't filter when no namespace is provided`() {
    val testData = NamespacesTestData()
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
    projectSupplier = { testData.projectBuilder.self }
    performProjectAuthGet("/translations").andPrettyPrint.andIsOk.andAssertThatJson {
      node("_embedded.keys") {
        isArray.hasSize(5)
      }
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
    performProjectAuthGet("/translations?sort=keyName&filterTag=Cool tag&filterTag=Another cool tag")
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

  @ProjectJWTAuthTestMethod
  @Test
  fun `filters by outdated`() {
    val testData = TranslationSourceChangeStateTestData()
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
    projectSupplier = { testData.projectBuilder.self }
    performProjectAuthGet("/translations?filterOutdatedLanguage=de").andIsOk.andAssertThatJson {
      node("_embedded.keys") {
        isArray.hasSize(1)
        node("[0].keyName").isEqualTo("A key")
        node("[0].translations.de.outdated").isEqualTo(true)
      }
    }
    performProjectAuthGet("/translations?filterNotOutdatedLanguage=de").andIsOk.andAssertThatJson {
      node("_embedded.keys") {
        isArray.hasSize(1)
        node("[0].keyName").isEqualTo("B key")
        node("[0].translations.de.outdated").isEqualTo(false)
      }
    }
    performProjectAuthGet("/translations?filterNotOutdatedLanguage=de&filterOutdatedLanguage=de")
      .andIsOk.andAssertThatJson {
        node("_embedded.keys").isArray.hasSize(2)
      }
  }
}
