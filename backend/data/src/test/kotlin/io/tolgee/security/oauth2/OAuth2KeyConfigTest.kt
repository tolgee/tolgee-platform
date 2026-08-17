package io.tolgee.security.oauth2

import com.nimbusds.jose.jwk.JWKMatcher
import com.nimbusds.jose.jwk.JWKSelector
import com.nimbusds.jose.jwk.KeyType
import com.nimbusds.jose.jwk.source.JWKSource
import com.nimbusds.jose.proc.SecurityContext
import io.tolgee.component.LockingProvider
import io.tolgee.component.fileStorage.FileStorage
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.concurrent.atomic.AtomicReference

class OAuth2KeyConfigTest {
  private val stored = AtomicReference<ByteArray?>(null)

  private val fileStorage =
    mock<FileStorage> {
      on { fileExists(any()) } doAnswer { stored.get() != null }
      on { readFile(any()) } doAnswer { stored.get()!! }
    }.also { storage ->
      doAnswer {
        stored.set(it.getArgument(1))
        Unit
      }.whenever(storage).storeFile(any(), any())
    }

  private val lockingProvider =
    mock<LockingProvider> {
      on { withLocking(any<String>(), any<() -> Any>()) } doAnswer { it.getArgument<() -> Any>(1)() }
    }

  @Test
  fun `generates and persists a key on first boot, then reloads the same key without regenerating`() {
    val firstKid = firstKid(OAuth2KeyConfig(fileStorage, lockingProvider).jwkSource())
    verify(fileStorage, times(1)).storeFile(any(), any())
    // Generation must run under the cluster-wide lock, or concurrently-booting replicas each mint a different key.
    verify(lockingProvider).withLocking(eq(OAuth2KeyConfig.ACTIVE_KEY_FILE), any<() -> Any>())

    // A second construction (restart / new replica) with the key already on disk must reuse it, not regenerate — else
    // every already-issued token fails JWKS validation after each deploy.
    val secondKid = firstKid(OAuth2KeyConfig(fileStorage, lockingProvider).jwkSource())
    assertThat(secondKid).isEqualTo(firstKid)
    verify(fileStorage, times(1)).storeFile(any(), any())
  }

  private fun firstKid(source: JWKSource<SecurityContext>): String {
    val selector = JWKSelector(JWKMatcher.Builder().keyType(KeyType.RSA).build())
    return source.get(selector, null).first().keyID
  }
}
