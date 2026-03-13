package io.tolgee.unit

import io.tolgee.exceptions.BadRequestException
import io.tolgee.model.key.Key
import io.tolgee.service.translation.validateCharLimit
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows

class CharLimitUtilTest {
  @Test
  fun `throws when translation exceeds char limit`() {
    val key = createKey("test-key", maxCharLimit = 5)
    assertThrows<BadRequestException> {
      validateCharLimit(key, mapOf("en" to "Hello World"))
    }
  }

  @Test
  fun `does not throw when translation is within limit`() {
    val key = createKey("test-key", maxCharLimit = 20)
    assertDoesNotThrow {
      validateCharLimit(key, mapOf("en" to "Hello"))
    }
  }

  @Test
  fun `does not throw when maxCharLimit is null`() {
    val key = createKey("test-key", maxCharLimit = null)
    assertDoesNotThrow {
      validateCharLimit(key, mapOf("en" to "This can be any length"))
    }
  }

  @Test
  fun `does not throw when maxCharLimit is zero`() {
    val key = createKey("test-key", maxCharLimit = 0)
    assertDoesNotThrow {
      validateCharLimit(key, mapOf("en" to "This can be any length"))
    }
  }

  @Test
  fun `html tags are not counted toward char limit`() {
    val key = createKey("test-key", maxCharLimit = 5)
    assertDoesNotThrow {
      validateCharLimit(key, mapOf("en" to "<b>Hello</b>"))
    }
  }

  @Test
  fun `variables are not counted toward char limit`() {
    val key = createKey("test-key", maxCharLimit = 6)
    assertDoesNotThrow {
      validateCharLimit(key, mapOf("en" to "Hello {name}"))
    }
  }

  @Test
  fun `plural hash is not counted toward char limit`() {
    val key = createKey("test-key", maxCharLimit = 15, isPlural = true)
    assertDoesNotThrow {
      validateCharLimit(
        key,
        mapOf("en" to "{count, plural, one {# item is here} other {# items are here}}"),
      )
    }
  }

  @Test
  fun `throws when plural form exceeds char limit`() {
    val key = createKey("test-key", maxCharLimit = 5, isPlural = true)
    assertThrows<BadRequestException> {
      validateCharLimit(
        key,
        mapOf("en" to "{count, plural, one {# item is here} other {# items are here}}"),
      )
    }
  }

  @Test
  fun `skips null translation values`() {
    val key = createKey("test-key", maxCharLimit = 5)
    assertDoesNotThrow {
      validateCharLimit(key, mapOf("en" to null, "de" to "Hi"))
    }
  }

  private fun createKey(
    name: String,
    maxCharLimit: Int? = null,
    isPlural: Boolean = false,
  ): Key {
    val key = Key(name = name)
    key.maxCharLimit = maxCharLimit
    key.isPlural = isPlural
    return key
  }
}
