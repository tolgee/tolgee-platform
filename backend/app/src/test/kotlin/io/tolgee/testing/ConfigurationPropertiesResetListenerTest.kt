package io.tolgee.testing

import io.tolgee.AbstractSpringTest
import io.tolgee.configuration.tolgee.GithubAuthenticationProperties
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder

@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class ConfigurationPropertiesResetListenerTest : AbstractSpringTest() {
  private var capturedGithub: GithubAuthenticationProperties? = null

  @Test
  @Order(1)
  fun `mutation is visible within the test`() {
    assertThat(internalProperties.fakeMtProviders).isTrue()
    internalProperties.fakeMtProviders = false
    assertThat(internalProperties.fakeMtProviders).isFalse()
  }

  @Test
  @Order(2)
  fun `prior mutation is rolled back to the yaml baseline, not the kotlin default`() {
    assertThat(internalProperties.fakeMtProviders).isTrue()
  }

  @Test
  @Order(3)
  fun `mutates a sub-properties object captured by reference`() {
    capturedGithub = tolgeeProperties.authentication.github
    tolgeeProperties.authentication.github.clientId = "leaked"
  }

  @Test
  @Order(4)
  fun `a reference captured in a prior test still observes the restored value`() {
    val captured = capturedGithub!!
    assertThat(tolgeeProperties.authentication.github).isSameAs(captured)
    assertThat(captured.clientId).isEqualTo("fake_id")
  }
}
