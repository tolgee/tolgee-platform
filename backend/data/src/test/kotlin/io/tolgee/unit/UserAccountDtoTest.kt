package io.tolgee.unit

import io.tolgee.dtos.cacheable.UserAccountDto
import io.tolgee.dtos.cacheable.isTokenInvalidated
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.Date

class UserAccountDtoTest {
  @Test
  fun `isTokenInvalidated does not reject a token minted in the same second as the cutoff`() {
    // Cutoff at .500 of a second; a token whose whole-second iat lands on that same second must survive — otherwise a
    // token legitimately minted right after the invalidation reads as "before" the millisecond-precise cutoff.
    val dto = dto(tokensValidNotBefore = Date(1_700_000_000_500))
    assertThat(dto.isTokenInvalidated(Instant.ofEpochSecond(1_700_000_000))).isFalse()
  }

  @Test
  fun `isTokenInvalidated rejects a token from an earlier second`() {
    val dto = dto(tokensValidNotBefore = Date(1_700_000_000_500))
    assertThat(dto.isTokenInvalidated(Instant.ofEpochSecond(1_699_999_999))).isTrue()
  }

  @Test
  fun `isTokenInvalidated allows every token when no cutoff is set`() {
    assertThat(dto(tokensValidNotBefore = null).isTokenInvalidated(Instant.ofEpochSecond(1))).isFalse()
    assertThat(dto(tokensValidNotBefore = null).isTokenInvalidated(null)).isFalse()
  }

  @Test
  fun `isTokenInvalidated fails closed when a cutoff is set but the token has no issue time`() {
    assertThat(dto(tokensValidNotBefore = Date(1_700_000_000_500)).isTokenInvalidated(null)).isTrue()
  }

  private fun dto(tokensValidNotBefore: Date?) =
    UserAccountDto(
      name = "n",
      username = "u",
      domain = null,
      role = null,
      id = 1,
      needsSuperJwt = false,
      avatarHash = null,
      deleted = false,
      tokensValidNotBefore = tokensValidNotBefore,
      emailVerified = true,
      thirdPartyAuth = null,
      ssoRefreshToken = null,
      ssoSessionExpiry = null,
    )
}
