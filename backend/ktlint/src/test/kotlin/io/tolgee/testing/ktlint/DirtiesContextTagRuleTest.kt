package io.tolgee.testing.ktlint

import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThatRule
import io.tolgee.testing.ktlint.rules.DirtiesContextTagRule
import org.junit.jupiter.api.Test

class DirtiesContextTagRuleTest {
  private val wrappingRuleAssertThat = assertThatRule { DirtiesContextTagRule() }

  @Test
  fun `raises an error if DirtiesContext class test doesn't have ContextRecreatingTest annotation`() {
    val code =
      """
      @DirtiesContext
      class MyTest {
      }
      """.trimIndent()
    wrappingRuleAssertThat(code)
      .hasLintViolationWithoutAutoCorrect(
        1,
        1,
        DirtiesContextTagRule.ERROR_MESSAGE,
      )
  }

  @Test
  fun `raises an error if DirtiesContext method isn't inside the class with ContextRecreatingTest annotation`() {
    val code =
      """
      class MyTest {
        @Test
        @DirtiesContext
        fun myTestMethod() {
        }
      }
      """.trimIndent()
    wrappingRuleAssertThat(code)
      .hasLintViolationWithoutAutoCorrect(
        2,
        3,
        DirtiesContextTagRule.ERROR_MESSAGE,
      )
  }

  @Test
  fun `no error if DirtiesContext test has ContextRecreatingTest annotation`() {
    val code =
      """
      @DirtiesContext
      @ContextRecreatingTest
      class MyTest {
      }
      """.trimIndent()
    wrappingRuleAssertThat(code).hasNoLintViolations()
  }

  @Test
  fun `no error if DirtiesContext method is inside the class with ContextRecreatingTest annotation`() {
    val code =
      """
      @ContextRecreatingTest
      class MyTest {
        @Test
        @DirtiesContext
        fun myTestMethod() {
        }
      }
      """.trimIndent()
    wrappingRuleAssertThat(code).hasNoLintViolations()
  }
}
