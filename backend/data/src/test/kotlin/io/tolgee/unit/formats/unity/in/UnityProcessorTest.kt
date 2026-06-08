package io.tolgee.unit.formats.unity.`in`

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.tolgee.formats.unity.`in`.UnityProcessor
import io.tolgee.testing.assert
import io.tolgee.util.FileProcessorContextMockUtil
import io.tolgee.util.assertKey
import io.tolgee.util.assertSingle
import io.tolgee.util.assertSinglePlural
import io.tolgee.util.assertTranslations
import io.tolgee.util.custom
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class UnityProcessorTest {
  private lateinit var mockUtil: FileProcessorContextMockUtil

  @BeforeEach
  fun setup() {
    mockUtil = FileProcessorContextMockUtil()
    mockUtil.mockIt("collection.unity", "src/test/resources/import/unity/collection.unity")
  }

  @Test
  fun `imports literal and smart single translations`() {
    process()
    mockUtil.fileProcessorContext.assertTranslations("en", "greeting").assertSingle { hasText("Hello") }
    mockUtil.fileProcessorContext.assertTranslations("cs", "greeting").assertSingle { hasText("Ahoj") }
    mockUtil.fileProcessorContext.assertTranslations("en", "welcome").assertSingle { hasText("Hi {name}") }
  }

  @Test
  fun `imports a smart plural as an ICU plural`() {
    process()
    mockUtil.fileProcessorContext
      .assertTranslations("en", "apples")
      .assertSinglePlural {
        hasText(
          """
          {value, plural,
          one {# apple}
          other {# apples}
          }
          """.trimIndent(),
        )
      }
  }

  @Test
  fun `preserves unity identity in key custom`() {
    process()
    mockUtil.fileProcessorContext.assertKey("greeting") {
      custom!!["_unityKeyId"].assert.isEqualTo(12345L)
      custom!!["_unitySharedTableDataGuid"].assert.isEqualTo("1234567890abcdef1234567890abcdef")
      custom!!["_unityIsSmart"].assert.isEqualTo(false)
    }
  }

  private fun process() {
    UnityProcessor(mockUtil.fileProcessorContext, jacksonObjectMapper()).process()
  }
}
