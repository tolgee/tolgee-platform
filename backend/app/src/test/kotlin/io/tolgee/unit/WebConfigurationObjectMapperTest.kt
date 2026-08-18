package io.tolgee.unit

import io.tolgee.configuration.WebConfiguration
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock

class WebConfigurationObjectMapperTest {
  class WithPrimitives(
    val count: Long,
    val flag: Boolean,
  )

  private val objectMapper = WebConfiguration(mock(), mock()).objectMapper()

  @Test
  fun `leaves absent primitives at their default instead of failing`() {
    val parsed = objectMapper.readValue("""{}""", WithPrimitives::class.java)

    assertThat(parsed.count).isEqualTo(0)
    assertThat(parsed.flag).isFalse()
  }

  @Test
  fun `still binds primitives that are present`() {
    val parsed = objectMapper.readValue("""{"count": 7, "flag": true}""", WithPrimitives::class.java)

    assertThat(parsed.count).isEqualTo(7)
    assertThat(parsed.flag).isTrue()
  }

  @Test
  fun `ignores unknown properties`() {
    val parsed = objectMapper.readValue("""{"count": 7, "flag": true, "surprise": "x"}""", WithPrimitives::class.java)

    assertThat(parsed.count).isEqualTo(7)
  }
}
