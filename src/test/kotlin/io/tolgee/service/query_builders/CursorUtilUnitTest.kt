package io.tolgee.service.query_builders

import io.tolgee.fixtures.node
import io.tolgee.model.enums.TranslationState
import io.tolgee.model.views.KeyWithTranslationsView
import io.tolgee.model.views.TranslationView
import net.javacrumbs.jsonunit.assertj.assertThatJson
import org.springframework.data.domain.Sort
import org.testng.annotations.Test
import java.util.*

class CursorUtilUnitTest {

    @Test()
    fun `generates cursor`() {
        val item = KeyWithTranslationsView(
                keyId = 1,
                keyName = "Super key",
                screenshotCount = 1,
                translations = mutableMapOf(
                        "en" to TranslationView(
                                1,
                                "Super key translated \uD83C\uDF8C",
                                TranslationState.TRANSLATED
                        )
                )
        )
        val result = CursorUtil.getCursor(item, Sort.by(
                Sort.Order.asc("translations.en.text"),
                Sort.Order.desc("keyName")
        ))
        val decoded = String(Base64.getDecoder().decode(result))
        assertThatJson(decoded).apply {
            node("keyName").isEqualTo("{\"direction\":\"DESC\",\"value\":\"Super key\"}")
            node("translations\\.en\\.text") {
                node("direction").isEqualTo("ASC")
                node("value").isEqualTo("Super key translated 🎌")
            }
        }
    }
}
