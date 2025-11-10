package io.tolgee.api.v2.controllers.translations.v2TranslationsController

import io.tolgee.ProjectAuthControllerTest
import io.tolgee.development.testDataBuilder.data.NamespacesTestData
import io.tolgee.development.testDataBuilder.data.TranslationsTestData
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.andPrettyPrint
import io.tolgee.fixtures.satisfies
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import io.tolgee.testing.assertions.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
@AutoConfigureMockMvc
class TranslationsControllerCursorTest : ProjectAuthControllerTest("/v2/projects/") {
  lateinit var testData: TranslationsTestData

  @BeforeEach
  fun setup() {
    testData = TranslationsTestData()
    this.projectSupplier = { testData.project }
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `works with cursor`() {
    testData.generateCursorTestData()
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
    var cursor = ""
    performProjectAuthGet("/translations?sort=translations.de.text&sort=keyName&size=4")
      .andPrettyPrint.andIsOk
      .andAssertThatJson {
        node("nextCursor").isString.satisfies { cursor = it }
      }

    performProjectAuthGet("/translations?sort=translations.de.text&size=4&sort=keyName&cursor=$cursor")
      .andPrettyPrint.andIsOk
      .andAssertThatJson {
        node("_embedded.keys[0].keyName").isEqualTo("c")
        node("_embedded.keys[3].keyName").isEqualTo("A key")
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
      .andPrettyPrint.andIsOk
      .andAssertThatJson {
        node("_embedded.keys[0].keyName").isEqualTo("Hello")
        node("nextCursor").isString.satisfies { cursor = it }
      }

    performProjectAuthGet("/translations?sort=translations.de.text&size=2&sort=keyName&search=hello&cursor=$cursor")
      .andPrettyPrint.andIsOk
      .andAssertThatJson {
        node("_embedded.keys").isArray.hasSize(1)
        node("_embedded.keys[0].keyName").isEqualTo("Hello 3")
      }
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `works with cursor and no sort specified`() {
    // Reference: https://github.com/tolgee/tolgee-platform/issues/1345
    testData.generateCursorTestData()
    testDataService.saveTestData(testData.root)
    userAccount = testData.user

    val seenKeys = mutableListOf<String>()
    var cursor: String? = null
    do {
      var url = "/translations?size=2"
      if (cursor != null) {
        url += "&cursor=$cursor"
      }

      performProjectAuthGet(url)
        .andAssertThatJson {
          try {
            node("nextCursor").isString.satisfies { cursor = it }
          } catch (_: AssertionError) {
            cursor = null
            node("_embedded").isAbsent()
            return@andAssertThatJson
          }

          node("_embedded.keys[0].keyName").isString.isNotIn(seenKeys).satisfies { seenKeys.add(it) }
          node("_embedded.keys[1].keyName").isString.isNotIn(seenKeys).satisfies { seenKeys.add(it) }
        }
    } while (cursor != null)

    assertThat(seenKeys).hasSize(8)
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `works with cursor on duplicate items sort`() {
    testData.generateCursorWithDupeTestData()
    testDataService.saveTestData(testData.root)
    userAccount = testData.user

    val seenKeys = mutableListOf<String>()
    var cursor: String? = null
    do {
      var url = "/translations?sort=translations.de.text&size=2"
      if (cursor != null) {
        url += "&cursor=$cursor"
      }

      performProjectAuthGet(url)
        .andAssertThatJson {
          try {
            node("nextCursor").isString.satisfies { cursor = it }
          } catch (_: AssertionError) {
            cursor = null
            node("_embedded").isAbsent()
            return@andAssertThatJson
          }

          node("_embedded.keys[0].keyName").isString.isNotIn(seenKeys).satisfies { seenKeys.add(it) }
          node("_embedded.keys[1].keyName").isString.isNotIn(seenKeys).satisfies { seenKeys.add(it) }
        }
    } while (cursor != null)

    assertThat(seenKeys).hasSize(8)
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `works with cursor and namespaces`() {
    val testData = NamespacesTestData()
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
    projectSupplier = { testData.projectBuilder.self }

    assertWithCursorReturnsAll("sort=keyNamespace&sort=keyName", 5)
    assertWithCursorReturnsAll("sort=keyNamespace,DESC&sort=keyName", 5)
    assertWithCursorReturnsAll("sort=keyNamespace,DESC&sort=keyName,DESC", 5)
    assertWithCursorReturnsAll("sort=keyNamespace,DESC&sort=keyName", 5)
  }

  private fun assertWithCursorReturnsAll(
    sortQuery: String,
    expectedSize: Int,
  ) {
    var pageCount = 0
    var cursor: String? = null
    do {
      var url = "/translations?$sortQuery&size=1"
      if (cursor != null) {
        url += "&cursor=$cursor"
      }

      performProjectAuthGet(url)
        .andPrettyPrint
        .andAssertThatJson {
          try {
            node("nextCursor").isString.satisfies { cursor = it }
          } catch (_: AssertionError) {
            cursor = null
            node("_embedded").isAbsent()
            return@andAssertThatJson
          }

          pageCount++
        }
    } while (cursor != null)

    assertThat(pageCount).isEqualTo(expectedSize)
  }
}
