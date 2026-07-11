package io.tolgee.development.testDataBuilder.data

import io.tolgee.development.testDataBuilder.builders.LanguageBuilder
import io.tolgee.model.Language
import io.tolgee.model.key.Key
import java.util.Date

/**
 * Fixture for scoped/pattern search: keys that differ by name, description,
 * namespace, and per-language translation so a single filter dimension can be
 * exercised in isolation. Shared by the backend controller test and the E2E
 * data controller.
 */
class ScopedSearchTestData : BaseTestData() {
  lateinit var germanLanguageBuilder: LanguageBuilder
  val germanLanguage: Language get() = germanLanguageBuilder.self
  lateinit var cartTitleKey: Key

  init {
    projectBuilder.apply {
      germanLanguageBuilder =
        addLanguage {
          name = "German"
          tag = "de"
          originalName = "Deutsch"
        }

      cartTitleKey =
        addKey { name = "cart.title" }
          .build {
            setDescription("shopping cart heading")
            addTranslation {
              language = englishLanguage
              text = "Add to cart"
            }
            addTranslation {
              language = germanLanguage
              text = "Warenkorb hinzufügen"
            }
          }.self
      addKey { name = "cart_subtitle" }.build {
        addTranslation {
          language = englishLanguage
          text = "Cart subtitle"
        }
      }
      addKey("shop", "checkout.title").build {
        setDescription("legacy checkout")
        addTranslation {
          language = englishLanguage
          text = "Checkout"
        }
      }
      addKey { name = "my.cart" }.build {
        addTranslation {
          language = englishLanguage
          text = "cart, with comma"
        }
      }
      addKey { name = "100%_done" }
      addKey { name = "100%xdone" }
      addKey { name = "comma,key" }
      addKey { name = "back\\slash" }
      addKey { name = "backslash" }
      addKey { name = "straße" }
      addKey {
        name = "trashed.cart"
        deletedAt = Date(1759834567)
      }.build {
        addTranslation {
          language = germanLanguage
          text = "Warenkorb im Papierkorb"
        }
      }
      addKey {
        name = "trashed.other"
        deletedAt = Date(1759834567)
      }
    }
  }
}
