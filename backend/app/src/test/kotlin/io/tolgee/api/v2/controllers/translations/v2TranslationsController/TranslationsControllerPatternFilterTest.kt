package io.tolgee.api.v2.controllers.translations.v2TranslationsController

import io.tolgee.ProjectAuthControllerTest
import io.tolgee.development.testDataBuilder.data.ScopedSearchTestData
import io.tolgee.fixtures.andAssertError
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsBadRequest
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.node
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
@AutoConfigureMockMvc
class TranslationsControllerPatternFilterTest : ProjectAuthControllerTest("/v2/projects/") {
  lateinit var testData: ScopedSearchTestData

  @BeforeEach
  fun setup() {
    testData = ScopedSearchTestData()
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
    this.projectSupplier = { testData.project }
  }

  @AfterEach
  fun clean() {
    testDataService.cleanTestData(testData.root)
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `filters key by contains pattern`() {
    performProjectAuthGet("/translations?filterKeyPattern=cart")
      .andIsOk
      .andAssertThatJson {
        node("_embedded.keys").isArray.hasSize(3)
        node("page.totalElements").isEqualTo(3)
      }
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `filters key by starts-with pattern`() {
    performProjectAuthGet("/translations?filterKeyPattern=cart*")
      .andIsOk
      .andAssertThatJson {
        node("_embedded.keys") {
          isArray.hasSize(2)
          node("[0].keyName").isEqualTo("cart.title")
          node("[1].keyName").isEqualTo("cart_subtitle")
        }
      }
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `filters key by ends-with pattern`() {
    performProjectAuthGet("/translations?filterKeyPattern=*cart")
      .andIsOk
      .andAssertThatJson {
        node("_embedded.keys") {
          isArray.hasSize(1)
          node("[0].keyName").isEqualTo("my.cart")
        }
      }
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `matches locale-special characters case-insensitively`() {
    performProjectAuthGet("/translations?filterKeyPattern=straße")
      .andIsOk
      .andAssertThatJson {
        node("_embedded.keys") {
          isArray.hasSize(1)
          node("[0].keyName").isEqualTo("straße")
        }
      }
    performProjectAuthGet("/translations?filterKeyPattern=STRAßE")
      .andIsOk
      .andAssertThatJson {
        node("page.totalElements").isEqualTo(1)
      }
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `key pattern is case-insensitive`() {
    performProjectAuthGet("/translations?filterKeyPattern=CART*")
      .andIsOk
      .andAssertThatJson {
        node("_embedded.keys").isArray.hasSize(2)
      }
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `filters key by negated pattern`() {
    performProjectAuthGet("/translations?filterNoKeyPattern=cart*")
      .andIsOk
      .andAssertThatJson {
        node("page.totalElements").isEqualTo(8)
      }
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `escapes LIKE metacharacters in patterns`() {
    performProjectAuthGet("/translations?filterKeyPattern=100%_done")
      .andIsOk
      .andAssertThatJson {
        node("_embedded.keys") {
          isArray.hasSize(1)
          node("[0].keyName").isEqualTo("100%_done")
        }
      }
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `escapes LIKE metacharacters also in patterns with wildcards`() {
    performProjectAuthGet("/translations?filterKeyPattern=*%_done")
      .andIsOk
      .andAssertThatJson {
        node("_embedded.keys") {
          isArray.hasSize(1)
          node("[0].keyName").isEqualTo("100%_done")
        }
      }
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `escapes percent as a literal`() {
    performProjectAuthGet("/translations?filterKeyPattern=100%done")
      .andIsOk
      .andAssertThatJson {
        node("page.totalElements").isEqualTo(0)
      }
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `escapes backslash as a literal`() {
    performProjectAuthGet("/translations?filterKeyPattern=back\\slash")
      .andIsOk
      .andAssertThatJson {
        node("_embedded.keys") {
          isArray.hasSize(1)
          node("[0].keyName").isEqualTo("back\\\\slash")
        }
      }
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `pattern of a single backslash does not fail`() {
    performProjectAuthGet("/translations?filterKeyPattern=%5C")
      .andIsOk
      .andAssertThatJson {
        node("page.totalElements").isEqualTo(0)
      }
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `filters by description pattern`() {
    performProjectAuthGet("/translations?filterDescriptionPattern=cart")
      .andIsOk
      .andAssertThatJson {
        node("_embedded.keys") {
          isArray.hasSize(1)
          node("[0].keyName").isEqualTo("cart.title")
        }
      }
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `negated description pattern includes keys without description`() {
    performProjectAuthGet("/translations?filterNoDescriptionPattern=legacy")
      .andIsOk
      .andAssertThatJson {
        node("page.totalElements").isEqualTo(9)
      }
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `filters by namespace pattern`() {
    performProjectAuthGet("/translations?filterNamespacePattern=sho*")
      .andIsOk
      .andAssertThatJson {
        node("_embedded.keys") {
          isArray.hasSize(1)
          node("[0].keyName").isEqualTo("checkout.title")
        }
      }
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `negated namespace pattern includes keys in default namespace`() {
    performProjectAuthGet("/translations?filterNoNamespacePattern=sho*")
      .andIsOk
      .andAssertThatJson {
        node("page.totalElements").isEqualTo(9)
      }
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `filters by translation pattern in any language`() {
    performProjectAuthGet("/translations?languages=en&languages=de&filterTranslationPattern=*,Warenkorb")
      .andIsOk
      .andAssertThatJson {
        node("_embedded.keys") {
          isArray.hasSize(1)
          node("[0].keyName").isEqualTo("cart.title")
        }
      }
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `filters by translation pattern in specific language`() {
    performProjectAuthGet("/translations?languages=en&languages=de&filterTranslationPattern=de,Warenkorb")
      .andIsOk
      .andAssertThatJson {
        node("page.totalElements").isEqualTo(1)
      }
    performProjectAuthGet("/translations?languages=en&languages=de&filterTranslationPattern=en,Warenkorb")
      .andIsOk
      .andAssertThatJson {
        node("page.totalElements").isEqualTo(0)
      }
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `negated translation pattern matches keys without the translation`() {
    performProjectAuthGet("/translations?languages=en&languages=de&filterNoTranslationPattern=de,Warenkorb")
      .andIsOk
      .andAssertThatJson {
        node("page.totalElements").isEqualTo(9)
      }
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `rejects any-language translation pattern when no language is resolvable`() {
    performProjectAuthGet("/translations?languages=xx&filterTranslationPattern=*,cart")
      .andIsBadRequest.andAssertError
      .hasCode("filter_pattern_language_not_valid")
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `rejects a translation pattern language that is a project language but not requested`() {
    performProjectAuthGet("/translations?languages=en&filterTranslationPattern=de,Warenkorb")
      .andIsBadRequest
      .andAssertError
      .hasCode("filter_pattern_language_not_valid")
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `rejects translation pattern with unknown language`() {
    performProjectAuthGet("/translations?languages=en&languages=de&filterTranslationPattern=xx,cart")
      .andIsBadRequest.andAssertError
      .hasCode("filter_pattern_language_not_valid")
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `matches translation pattern language tag case-insensitively`() {
    performProjectAuthGet("/translations?languages=en&languages=de&filterTranslationPattern=DE,Warenkorb")
      .andIsOk
      .andAssertThatJson {
        node("page.totalElements").isEqualTo(1)
      }
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `negated any-language translation pattern matches keys without any matching translation`() {
    performProjectAuthGet("/translations?languages=en&languages=de&filterNoTranslationPattern=*,cart")
      .andIsOk
      .andAssertThatJson {
        node("page.totalElements").isEqualTo(7)
      }
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `field pattern value may contain commas`() {
    performProjectAuthGet("/translations?filterKeyPattern=comma,key")
      .andIsOk
      .andAssertThatJson {
        node("_embedded.keys") {
          isArray.hasSize(1)
          node("[0].keyName").isEqualTo("comma,key")
        }
      }
    // comma-split binding would yield ["key", "comma"] and still match comma,key
    performProjectAuthGet("/translations?filterKeyPattern=key,comma")
      .andIsOk
      .andAssertThatJson {
        node("page.totalElements").isEqualTo(0)
      }
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `description and namespace pattern values may contain commas`() {
    // comma-split binding would AND ["shopping", "heading"] and match cart.title
    performProjectAuthGet("/translations?filterDescriptionPattern=shopping,heading")
      .andIsOk
      .andAssertThatJson {
        node("page.totalElements").isEqualTo(0)
      }
    performProjectAuthGet("/translations?filterNoDescriptionPattern=shopping,heading")
      .andIsOk
      .andAssertThatJson {
        node("page.totalElements").isEqualTo(10)
      }
    performProjectAuthGet("/translations?filterNamespacePattern=sh,op")
      .andIsOk
      .andAssertThatJson {
        node("page.totalElements").isEqualTo(0)
      }
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `translation pattern value may contain commas`() {
    performProjectAuthGet("/translations?languages=en&filterTranslationPattern=en,cart, with comma")
      .andIsOk
      .andAssertThatJson {
        node("_embedded.keys") {
          isArray.hasSize(1)
          node("[0].keyName").isEqualTo("my.cart")
        }
      }
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `repeated patterns apply with AND semantics`() {
    performProjectAuthGet("/translations?filterKeyPattern=cart*&filterKeyPattern=*.title")
      .andIsOk
      .andAssertThatJson {
        node("_embedded.keys") {
          isArray.hasSize(1)
          node("[0].keyName").isEqualTo("cart.title")
        }
      }
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `combines with full-text search`() {
    performProjectAuthGet("/translations?search=shopping&filterKeyPattern=cart*")
      .andIsOk
      .andAssertThatJson {
        node("_embedded.keys") {
          isArray.hasSize(1)
          node("[0].keyName").isEqualTo("cart.title")
        }
      }
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `select-all applies translation pattern filters`() {
    performProjectAuthGet(
      "/translations/select-all?languages=en&languages=de&filterTranslationPattern=de,Warenkorb",
    ).andIsOk
      .andAssertThatJson {
        node("ids") {
          isArray.hasSize(1)
          node("[0]").isEqualTo(testData.cartTitleKey.id)
        }
      }
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `select-all rejects translation pattern with unresolvable language`() {
    performProjectAuthGet("/translations/select-all?languages=en&filterTranslationPattern=xx,cart")
      .andIsBadRequest.andAssertError
      .hasCode("filter_pattern_language_not_valid")
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `trash endpoint narrows results by pattern`() {
    performProjectAuthGet("/keys/trash?filterKeyPattern=*cart")
      .andIsOk
      .andAssertThatJson {
        node("_embedded.keys") {
          isArray.hasSize(1)
          node("[0].name").isEqualTo("trashed.cart")
        }
      }
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `select-all applies pattern filters`() {
    performProjectAuthGet("/translations/select-all?filterDescriptionPattern=cart")
      .andIsOk
      .andAssertThatJson {
        node("ids") {
          isArray.hasSize(1)
          node("[0]").isEqualTo(testData.cartTitleKey.id)
        }
      }
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `rejects pattern with too many wildcards`() {
    performProjectAuthGet("/translations?filterKeyPattern=*a*a*a*a*a*a")
      .andIsBadRequest.andAssertError
      .hasCode("filter_pattern_not_valid")
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `rejects too long pattern`() {
    val pattern = "a".repeat(501)
    performProjectAuthGet("/translations?filterKeyPattern=$pattern")
      .andIsBadRequest.andAssertError
      .hasCode("filter_pattern_not_valid")
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `rejects empty patterns`() {
    performProjectAuthGet("/translations?filterKeyPattern=")
      .andIsBadRequest.andAssertError
      .hasCode("filter_pattern_not_valid")
    performProjectAuthGet("/translations?languages=en&filterTranslationPattern=en,")
      .andIsBadRequest.andAssertError
      .hasCode("filter_pattern_not_valid")
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `accepts patterns at the limits`() {
    val maxLength = "a".repeat(500)
    performProjectAuthGet("/translations?filterKeyPattern=$maxLength")
      .andIsOk
      .andAssertThatJson {
        node("page.totalElements").isEqualTo(0)
      }
    performProjectAuthGet("/translations?filterKeyPattern=*a*b*c*d*e")
      .andIsOk
      .andAssertThatJson {
        node("page.totalElements").isEqualTo(0)
      }
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `combines different pattern filter types with AND semantics`() {
    performProjectAuthGet(
      "/translations?languages=en&languages=de" +
        "&filterKeyPattern=cart*&filterTranslationPattern=de,Warenkorb",
    ).andIsOk
      .andAssertThatJson {
        node("_embedded.keys") {
          isArray.hasSize(1)
          node("[0].keyName").isEqualTo("cart.title")
        }
      }
    performProjectAuthGet(
      "/translations?languages=en&languages=de" +
        "&filterKeyPattern=cart*&filterNoTranslationPattern=de,Warenkorb",
    ).andIsOk
      .andAssertThatJson {
        node("_embedded.keys") {
          isArray.hasSize(1)
          node("[0].keyName").isEqualTo("cart_subtitle")
        }
      }
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `repeated translation patterns apply with AND semantics`() {
    performProjectAuthGet(
      "/translations?languages=en&languages=de" +
        "&filterTranslationPattern=en,Add to cart&filterTranslationPattern=de,Warenkorb",
    ).andIsOk
      .andAssertThatJson {
        node("page.totalElements").isEqualTo(1)
      }
    performProjectAuthGet(
      "/translations?languages=en&languages=de" +
        "&filterTranslationPattern=en,Checkout&filterTranslationPattern=de,Warenkorb",
    ).andIsOk
      .andAssertThatJson {
        node("page.totalElements").isEqualTo(0)
      }
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `rejects too many patterns`() {
    val params = (1..21).joinToString("&") { "filterKeyPattern=p$it" }
    performProjectAuthGet("/translations?$params")
      .andIsBadRequest.andAssertError
      .hasCode("filter_pattern_not_valid")
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `trash endpoint keeps commas in pattern values`() {
    performProjectAuthGet("/keys/trash?languages=en&filterTranslationPattern=en,cart, with comma")
      .andIsOk
      .andAssertThatJson {
        node("page.totalElements").isEqualTo(0)
      }
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `rejects translation pattern without language part`() {
    performProjectAuthGet("/translations?languages=en&filterTranslationPattern=cart")
      .andIsBadRequest.andAssertError
      .hasCode("filter_pattern_not_valid")
  }
}
