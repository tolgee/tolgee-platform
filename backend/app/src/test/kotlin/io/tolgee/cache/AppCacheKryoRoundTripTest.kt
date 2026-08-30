package io.tolgee.cache

import io.tolgee.component.EnumNameKryo5Codec
import io.tolgee.dtos.cacheable.AppDto
import io.tolgee.dtos.cacheable.AppInstallDto
import io.tolgee.model.enums.Scope
import io.tolgee.testing.assert
import org.junit.jupiter.api.Test
import org.redisson.client.handler.State

/**
 * Runs every app-request cache value through the exact codec Redisson stores them with in
 * production ([EnumNameKryo5Codec]). [io.tolgee.api.v2.controllers.apps.AppCacheWithRedisTest]
 * exercises the same path against a live Redis, but needs a redis:6 container; this test needs no
 * container, so a codec regression is caught even where Docker cannot reach the image registry.
 */
class AppCacheKryoRoundTripTest {
  private val codec = EnumNameKryo5Codec()

  @Suppress("UNCHECKED_CAST")
  private fun <T> roundTrip(value: T): T {
    val encoded = codec.valueEncoder.encode(value)
    return codec.valueDecoder.decode(encoded, State()) as T
  }

  @Test
  fun `AppDto round-trips through the redis codec`() {
    val decoded = roundTrip(AppDto(id = 7, tokensInvalidBefore = 1_700_000_000_000L))
    decoded.id.assert.isEqualTo(7L)
    decoded.tokensInvalidBefore.assert.isEqualTo(1_700_000_000_000L)

    roundTrip(AppDto(id = 8, tokensInvalidBefore = null)).tokensInvalidBefore.assert.isNull()
  }

  @Test
  fun `AppInstallDto round-trips through the redis codec including its scope array`() {
    val decoded =
      roundTrip(
        AppInstallDto(
          id = 1,
          appEntityId = 2,
          organizationId = 3,
          principalUserId = 4,
          grantedScopes = arrayOf(Scope.TRANSLATIONS_VIEW, Scope.KEYS_EDIT),
        ),
      )
    decoded.id.assert.isEqualTo(1L)
    decoded.appEntityId.assert.isEqualTo(2L)
    decoded.organizationId.assert.isEqualTo(3L)
    decoded.principalUserId.assert.isEqualTo(4L)
    decoded.grantedScopes
      .toList()
      .assert
      .containsExactly(Scope.TRANSLATIONS_VIEW, Scope.KEYS_EDIT)
  }

  @Test
  fun `the enabled-project-id set round-trips through the redis codec`() {
    roundTrip(setOf(10L, 20L, 30L)).assert.containsExactlyInAnyOrder(10L, 20L, 30L)
  }
}
