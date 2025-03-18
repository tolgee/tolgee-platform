package io.tolgee.api.v2.controllers.translations.v2TranslationsController

import io.tolgee.ProjectAuthControllerTest
import io.tolgee.development.testDataBuilder.data.TranslationsTestData
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.andPrettyPrint
import io.tolgee.fixtures.node
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
@AutoConfigureMockMvc
class TranslationsControllerFiltersCombinationTest : ProjectAuthControllerTest("/v2/projects/") {
  lateinit var testData: TranslationsTestData

  @BeforeEach
  fun setup() {
    testData = TranslationsTestData()
    this.projectSupplier = { testData.project }
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `filters keys with screenshots which AND not translated`() {
    testData.addKeysWithScreenshots()
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
    performProjectAuthGet(
      "/translations?filterState=de,UNTRANSLATED&filterHasScreenshot=true",
    ).andPrettyPrint.andIsOk.andAssertThatJson {
      node("_embedded.keys") {
        isArray.hasSize(2)
      }
      node("page.totalElements").isEqualTo(2)
    }
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `filters reviewed keys in en OR de`() {
    testData.addTranslationsWithStates()
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
    performProjectAuthGet(
      "/translations?filterState=de,REVIEWED&filterState=en,REVIEWED",
    ).andPrettyPrint.andIsOk.andAssertThatJson {
      node("page.totalElements").isEqualTo(4)
    }
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `filters auto translated OR disabled`() {
    testData.addTranslationsWithStates()
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
    performProjectAuthGet(
      "/translations?filterAutoTranslatedInLang=en&filterState=de,DISABLED",
    ).andPrettyPrint.andIsOk.andAssertThatJson {
      node("page.totalElements").isEqualTo(3)
      node("_embedded.keys[0].keyName").isEqualTo("Z key")
      node("_embedded.keys[1].keyName").isEqualTo("state test key 5")
      node("_embedded.keys[2].keyName").isEqualTo("state test key 6")
    }
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `filters disabled OR with unresolved comment`() {
    testData.addTranslationsWithStates()
    testData.addCommentStatesData()
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
    performProjectAuthGet(
      "/translations?filterState=de,DISABLED&filterHasUnresolvedCommentsInLang=de",
    ).andPrettyPrint.andIsOk.andAssertThatJson {
      node("page.totalElements").isEqualTo(2)
      node("_embedded.keys[0].keyName").isEqualTo("state test key 6")
      node("_embedded.keys[1].keyName").isEqualTo("commented_key")
    }
  }
}
