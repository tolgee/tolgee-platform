package io.tolgee.api.v2.controllers.translations.v2TranslationsController

import io.tolgee.ProjectAuthControllerTest
import io.tolgee.development.testDataBuilder.data.TranslationsTestData
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsBadRequest
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.node
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
@AutoConfigureMockMvc
class TranslationsControllerPatternFilterTest : ProjectAuthControllerTest("/v2/projects/") {
  lateinit var testData: TranslationsTestData

  @BeforeEach
  fun setup() {
    testData = TranslationsTestData()
    testData.addPatternSearchData()
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
    this.projectSupplier = { testData.project }
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
        node("page.totalElements").isEqualTo(6)
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
        node("page.totalElements").isEqualTo(7)
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
        node("page.totalElements").isEqualTo(7)
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
        node("page.totalElements").isEqualTo(7)
      }
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `translation pattern with unknown language is ignored`() {
    performProjectAuthGet("/translations?languages=en&languages=de&filterTranslationPattern=xx,cart")
      .andIsOk
      .andAssertThatJson {
        node("page.totalElements").isEqualTo(8)
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
      .andIsBadRequest
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `rejects too long pattern`() {
    val pattern = "a".repeat(501)
    performProjectAuthGet("/translations?filterKeyPattern=$pattern")
      .andIsBadRequest
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `rejects translation pattern without language part`() {
    performProjectAuthGet("/translations?languages=en&filterTranslationPattern=cart")
      .andIsBadRequest
  }
}
