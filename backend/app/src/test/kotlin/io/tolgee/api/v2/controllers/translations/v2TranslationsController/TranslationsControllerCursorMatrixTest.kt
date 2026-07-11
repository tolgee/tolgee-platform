package io.tolgee.api.v2.controllers.translations.v2TranslationsController

import io.tolgee.ProjectAuthControllerTest
import io.tolgee.development.testDataBuilder.data.TranslationsTestData
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.satisfies
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import io.tolgee.testing.assertions.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest

/**
 * Exhaustive cursor + sort tests covering combinations not exercised by [TranslationsControllerCursorTest].
 *
 * Locks down the behavior of cursor pagination across every sortable column type (String,
 * Long, Timestamp) and direction (ASC/DESC), with particular focus on translation-text sort
 * columns — those are implemented as scalar correlated subqueries in the query builder, so
 * regressions there are hard to spot without thorough coverage.
 */
@SpringBootTest
@AutoConfigureMockMvc
class TranslationsControllerCursorMatrixTest : ProjectAuthControllerTest("/v2/projects/") {
  lateinit var testData: TranslationsTestData

  @BeforeEach
  fun setup() {
    testData = TranslationsTestData()
    this.projectSupplier = { testData.project }
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `cursor with sort by translation text DESC returns all keys uniquely`() {
    testData.generateCursorTestData()
    testDataService.saveTestData(testData.root)
    userAccount = testData.user

    assertCursorReturnsAllKeysUnique("sort=translations.de.text,DESC&sort=keyName", expectedSize = 8)
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `cursor with sort by translation text ASC returns all keys uniquely`() {
    testData.generateCursorTestData()
    testDataService.saveTestData(testData.root)
    userAccount = testData.user

    assertCursorReturnsAllKeysUnique("sort=translations.de.text,ASC&sort=keyName", expectedSize = 8)
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `cursor with sort by english translation text returns all keys uniquely`() {
    testData.generateCursorTestData()
    testDataService.saveTestData(testData.root)
    userAccount = testData.user

    assertCursorReturnsAllKeysUnique("sort=translations.en.text&sort=keyName", expectedSize = 8)
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `cursor with sort by keyName DESC returns all keys uniquely`() {
    testData.generateCursorTestData()
    testDataService.saveTestData(testData.root)
    userAccount = testData.user

    assertCursorReturnsAllKeysUnique("sort=keyName,DESC", expectedSize = 8)
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `cursor with sort by keyId returns all keys uniquely`() {
    testData.generateCursorTestData()
    testDataService.saveTestData(testData.root)
    userAccount = testData.user

    assertCursorReturnsAllKeysUnique("sort=keyId", expectedSize = 8)
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `cursor with sort by keyId DESC returns all keys uniquely`() {
    testData.generateCursorTestData()
    testDataService.saveTestData(testData.root)
    userAccount = testData.user

    assertCursorReturnsAllKeysUnique("sort=keyId,DESC", expectedSize = 8)
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `sort by translation text DESC orders results correctly first page`() {
    testData.generateCursorTestData()
    testDataService.saveTestData(testData.root)
    userAccount = testData.user

    // generateCursorTestData creates:
    //   a -> "f", b -> "f", c -> "d", d -> "b", e -> "b", f -> "a"
    // BaseTestData adds (no german) "Z key" and "A key" with german translations.
    // Specifically "A key" has german text "Z translation".
    // DESC by german translation text ordering: "Z translation" > "f" > "f" > "d" > "b" > "b" > "a"
    // (then null/missing translations come last because of NullPrecedence.LAST in DESC mode)
    performProjectAuthGet("/translations?sort=translations.de.text,DESC&sort=keyName&size=4")
      .andIsOk
      .andAssertThatJson {
        node("_embedded.keys[0].keyName").isEqualTo("A key")
        node("_embedded.keys[1].keyName").isEqualTo("a")
        node("_embedded.keys[2].keyName").isEqualTo("b")
        node("_embedded.keys[3].keyName").isEqualTo("c")
      }
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `sort by translation text ASC orders results correctly first page`() {
    testData.generateCursorTestData()
    testDataService.saveTestData(testData.root)
    userAccount = testData.user

    // ASC by german translation text: nulls first (NullPrecedence.FIRST in ASC mode), then "a", "b", "b", "d", "f", "f", "Z translation"
    // The single null germ translation belongs to "Z key" (it has only english).
    performProjectAuthGet("/translations?sort=translations.de.text,ASC&sort=keyName&size=4")
      .andIsOk
      .andAssertThatJson {
        // Z key has no german translation -> sorted first under ASC NULLS FIRST
        node("_embedded.keys[0].keyName").isEqualTo("Z key")
        node("_embedded.keys[1].keyName").isEqualTo("f") // text "a"
        node("_embedded.keys[2].keyName").isEqualTo("d") // text "b"
        node("_embedded.keys[3].keyName").isEqualTo("e") // text "b"
      }
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `sort by createdAt with cursor returns all keys uniquely`() {
    testData.generateLotOfData(8)
    testDataService.saveTestData(testData.root)
    userAccount = testData.user

    // 8 generated keys + 2 base keys (A key, Z key) = 10 total
    assertCursorReturnsAllKeysUnique("sort=createdAt", expectedSize = 10)
    assertCursorReturnsAllKeysUnique("sort=createdAt,DESC", expectedSize = 10)
  }

  /**
   * Walks all pages using the cursor, asserting that:
   *   - No key appears twice across pages
   *   - The total number of distinct keys equals expectedSize
   */
  private fun assertCursorReturnsAllKeysUnique(
    sortQuery: String,
    expectedSize: Int,
  ) {
    val seenKeys = mutableListOf<String>()
    var cursor: String? = null
    val maxPages = expectedSize + 10 // safety bound to prevent infinite loops
    var pageCount = 0
    do {
      var url = "/translations?$sortQuery&size=2"
      if (cursor != null) {
        url += "&cursor=$cursor"
      }
      pageCount++
      assertThat(pageCount)
        .withFailMessage("Exceeded $maxPages pages — likely an infinite cursor loop")
        .isLessThanOrEqualTo(maxPages)

      performProjectAuthGet(url)
        .andIsOk
        .andAssertThatJson {
          try {
            node("nextCursor").isString.satisfies { cursor = it }
          } catch (_: AssertionError) {
            cursor = null
            node("_embedded").isAbsent()
            return@andAssertThatJson
          }

          // Collect every key on the page (page size is 2, but the last page may have fewer).
          // We extract keys first, then assert no duplicates outside the try/catch so that
          // duplicate-key errors propagate instead of being swallowed.
          val pageKeys = mutableListOf<String>()
          for (i in 0..1) {
            try {
              node("_embedded.keys[$i].keyName").isString.satisfies { pageKeys.add(it) }
            } catch (_: AssertionError) {
              // fewer than 2 items on this page — that's OK
              break
            }
          }
          for (key in pageKeys) {
            assertThat(seenKeys)
              .withFailMessage("Duplicate key '$key' seen on page $pageCount")
              .doesNotContain(key)
            seenKeys.add(key)
          }
        }
    } while (cursor != null)

    assertThat(seenKeys).hasSize(expectedSize)
  }
}
