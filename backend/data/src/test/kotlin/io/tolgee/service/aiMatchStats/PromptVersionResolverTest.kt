package io.tolgee.service.aiMatchStats

import io.tolgee.testing.assert
import org.junit.jupiter.api.Test
import java.util.Date

class PromptVersionResolverTest {
  private val resolver = PromptVersionResolver(mapOf(5L to listOf(Date(10), Date(20), Date(30))))

  @Test
  fun `null prompt id resolves to null version`() {
    resolver.resolve(null, Date(15)).assert.isNull()
  }

  @Test
  fun `unknown prompt id resolves to null version`() {
    resolver.resolve(99, Date(15)).assert.isNull()
  }

  @Test
  fun `produced before the first boundary maps to the first version`() {
    resolver.resolve(5, Date(5)).assert.isEqualTo(Date(10))
  }

  @Test
  fun `produced between boundaries maps to the boundary in effect`() {
    resolver.resolve(5, Date(25)).assert.isEqualTo(Date(20))
  }

  @Test
  fun `produced on a boundary maps to that boundary`() {
    resolver.resolve(5, Date(20)).assert.isEqualTo(Date(20))
  }

  @Test
  fun `produced after the last boundary maps to the latest version`() {
    resolver.resolve(5, Date(100)).assert.isEqualTo(Date(30))
  }

  @Test
  fun `null produced time falls back to the first version`() {
    resolver.resolve(5, null).assert.isEqualTo(Date(10))
  }
}
