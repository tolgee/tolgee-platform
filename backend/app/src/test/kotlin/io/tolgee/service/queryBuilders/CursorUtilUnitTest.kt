package io.tolgee.service.queryBuilders

import io.tolgee.fixtures.node
import io.tolgee.model.enums.TranslationState
import io.tolgee.model.views.KeyWithTranslationsView
import io.tolgee.model.views.TranslationView
import io.tolgee.testing.assertions.Assertions.assertThat
import net.javacrumbs.jsonunit.assertj.assertThatJson
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.data.domain.Sort
import java.sql.Timestamp
import java.util.Base64

class CursorUtilUnitTest {
  lateinit var cursor: String

  @BeforeEach
  fun setup() {
    val item =
      KeyWithTranslationsView(
        keyId = 1,
        keyName = "Super key",
        keyIsPlural = false,
        keyPluralArgName = null,
        keyNamespaceId = null,
        keyNamespace = null,
        keyDescription = "Super key description",
        screenshotCount = 1,
        translations =
          mutableMapOf(
            "en" to
              TranslationView(
                id = 1,
                text = "Super key translated \uD83C\uDF8C",
                state = TranslationState.TRANSLATED,
                outdated = false,
                auto = false,
                mtProvider = null,
                commentCount = 0,
                unresolvedCommentCount = 1,
                activeSuggestionCount = 0,
                totalSuggestionCount = 0,
              ),
          ),
        createdAt = Timestamp(1000003022),
        contextPresent = false,
      )
    cursor =
      CursorUtil.getCursor(
        item,
        Sort.by(
          Sort.Order.asc("translations.en.text"),
          Sort.Order.desc("keyName"),
        ),
      )
  }

  @Test
  fun `generates cursor`() {
    val decoded = String(Base64.getDecoder().decode(cursor))
    assertThatJson(decoded).apply {
      node("keyName").isEqualTo("{\"direction\":\"DESC\",\"value\":\"Super key\"}")
      node("translations\\.en\\.text") {
        node("direction").isEqualTo("ASC")
        node("value").isEqualTo("Super key translated 🎌")
      }
    }
  }

  @Test
  fun `parses cursor`() {
    val parsed = CursorUtil.parseCursor(cursor)
    assertThat(parsed["keyName"]?.direction).isEqualTo(Sort.Direction.DESC)
    assertThat(parsed["keyName"]?.value).isEqualTo("Super key")
    assertThat(parsed.entries).hasSize(2)
  }
}
