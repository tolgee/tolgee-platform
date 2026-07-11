/**
 * Copyright (C) 2025 Tolgee s.r.o. and contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.tolgee.testing.ktlint

import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThatRule
import io.tolgee.testing.ktlint.rules.JakartaTransientInEntities
import org.junit.jupiter.api.Test

class JakartaTransientInEntitiesTest {
  private val wrappingRuleAssertThat = assertThatRule { JakartaTransientInEntities() }

  @Test
  fun `raises an error if using Kotlin transient in entity (property)`() {
    val code =
      """
      @Entity
      class MyEntity {
        @Transient
        var transientField: String? = null
      }
      """.trimIndent()
    wrappingRuleAssertThat(code)
      .hasLintViolationWithoutAutoCorrect(
        3,
        3,
        "Unexpected Kotlin-native @Transient. Import `jakarta.persistence.Transient`.",
      )
  }

  @Test
  fun `raises an error if using Kotlin transient in entity (constructor parameter)`() {
    val code =
      """
      @Entity
      class MyEntity(
        @Transient
        var transientField: String? = null
      )
      """.trimIndent()
    wrappingRuleAssertThat(code)
      .hasLintViolationWithoutAutoCorrect(
        3,
        3,
        "Unexpected Kotlin-native @Transient. Import `jakarta.persistence.Transient`.",
      )
  }

  @Test
  fun `raises an error if using Kotlin transient in entity (with specifier)`() {
    val code =
      """
      @Entity
      class MyEntity {
        @field:Transient
        var transientField: String? = null
      }
      """.trimIndent()
    wrappingRuleAssertThat(code)
      .hasLintViolationWithoutAutoCorrect(
        3,
        3,
        "Unexpected Kotlin-native @Transient. Import `jakarta.persistence.Transient`.",
      )
  }

  @Test
  fun `does not complain on Transient in non-entity classes`() {
    val code =
      """
      class MyEntity {
        @Transient
        var transientField: String? = null
      }
      """.trimIndent()
    wrappingRuleAssertThat(code)
      .hasNoLintViolations()
  }

  @Test
  fun `does not complain on correct Transient usage`() {
    val code =
      """
      import jakarta.persistence.Transient

      @Entity
      class MyEntity {
        @Transient
        var transientField: String? = null
      }
      """.trimIndent()
    wrappingRuleAssertThat(code)
      .hasNoLintViolations()
  }
}
