package io.tolgee.ee.unit.eeSubscription

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.tolgee.hateoas.limits.SelfHostedUsageLimitsModel
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/**
 * An instance can run a newer version than the licensing server it talks to, so the licence
 * response may predate any field added here. A missing field must not fail the whole model —
 * that would break the entire licence check, not just the fields it lacks.
 */
class SelfHostedUsageLimitsModelDeserializationTest {
  private val objectMapper = jacksonObjectMapper()

  @Test
  fun `deserializes a licence response that predates the words limit`() {
    val json =
      """
      {
        "keys": { "included": 100, "limit": 100 },
        "seats": { "included": 10, "limit": 10 },
        "mtCreditsInCents": { "included": 5000, "limit": 5000 }
      }
      """.trimIndent()

    val limits = objectMapper.readValue(json, SelfHostedUsageLimitsModel::class.java)

    // Unlimited is the safe absence value: the instance must not start blocking edits
    // because the server it asked did not know about words yet.
    assertThat(limits.words.included).isEqualTo(-1)
    assertThat(limits.words.limit).isEqualTo(-1)
    assertThat(limits.keys.included).isEqualTo(100)
  }

  @Test
  fun `reads the words limit when the server sends one`() {
    val json =
      """
      {
        "keys": { "included": 100, "limit": 100 },
        "seats": { "included": 10, "limit": 10 },
        "mtCreditsInCents": { "included": 5000, "limit": 5000 },
        "words": { "included": 50000, "limit": 60000 }
      }
      """.trimIndent()

    val limits = objectMapper.readValue(json, SelfHostedUsageLimitsModel::class.java)

    assertThat(limits.words.included).isEqualTo(50000)
    assertThat(limits.words.limit).isEqualTo(60000)
  }
}
