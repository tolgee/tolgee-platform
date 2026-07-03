package io.tolgee.testing

import io.tolgee.configuration.tolgee.GithubAuthenticationProperties
import io.tolgee.configuration.tolgee.PostgresAutostartProperties.PostgresAutostartMode
import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.configuration.tolgee.machineTranslation.LlmProperties
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.entry
import org.junit.jupiter.api.Test

class ConfigurationPropertiesSnapshotTest {
  @Test
  fun `restores mutated scalar, nested, collection and map properties`() {
    val properties = TolgeeProperties()
    properties.machineTranslation.freeCreditsAmount = 500
    properties.machineTranslation.google.apiKey = "snapshot-key"
    properties.internal.fakeMtProviders = true
    properties.slack.clientSecret = "snapshot-secret"
    properties.authentication.blockedEmailDomains = mutableListOf("blocked.example")
    properties.authentication.oauth2.scopes
      .add("openid")
    properties.llm.fallbacks = mutableMapOf("a" to "b")
    properties.llm.providers = mutableListOf(LlmProperties.LlmProvider(name = "snapshot-provider"))

    val snapshot = ConfigurationPropertiesSnapshot.snapshot(properties)

    properties.machineTranslation.freeCreditsAmount = 999
    properties.machineTranslation.google.apiKey = "mutated"
    properties.internal.fakeMtProviders = false
    properties.slack.clientSecret = "mutated"
    properties.authentication.blockedEmailDomains.add("leaked.example")
    properties.authentication.oauth2.scopes
      .add("leaked")
    properties.llm.fallbacks = mutableMapOf("x" to "y")
    properties.llm.providers = mutableListOf()

    ConfigurationPropertiesSnapshot.restore(properties, snapshot)

    assertThat(properties.machineTranslation.freeCreditsAmount).isEqualTo(500)
    assertThat(properties.machineTranslation.google.apiKey).isEqualTo("snapshot-key")
    assertThat(properties.internal.fakeMtProviders).isTrue()
    assertThat(properties.slack.clientSecret).isEqualTo("snapshot-secret")
    assertThat(properties.authentication.blockedEmailDomains).containsExactly("blocked.example")
    assertThat(properties.authentication.oauth2.scopes).containsExactly("openid")
    assertThat(properties.llm.fallbacks).containsExactly(entry("a", "b"))
    assertThat(properties.llm.providers).hasSize(1)
    assertThat(
      properties.llm.providers
        .first()
        .name,
    ).isEqualTo("snapshot-provider")
  }

  @Test
  fun `restores an enum-typed config property`() {
    val properties = TolgeeProperties()
    properties.postgresAutostart.mode = PostgresAutostartMode.EMBEDDED

    val snapshot = ConfigurationPropertiesSnapshot.snapshot(properties)
    properties.postgresAutostart.mode = PostgresAutostartMode.DOCKER

    ConfigurationPropertiesSnapshot.restore(properties, snapshot)

    assertThat(properties.postgresAutostart.mode).isEqualTo(PostgresAutostartMode.EMBEDDED)
  }

  @Test
  fun `restores nested config field values while keeping the nested instance, so captured references stay live`() {
    val properties = TolgeeProperties()
    val capturedRoot = properties
    val capturedAuth = properties.authentication
    val capturedGithub = properties.authentication.github

    val snapshot = ConfigurationPropertiesSnapshot.snapshot(properties)
    properties.authentication.github.clientSecret = "mutated"

    ConfigurationPropertiesSnapshot.restore(properties, snapshot)

    assertThat(properties).isSameAs(capturedRoot)
    assertThat(properties.authentication).isSameAs(capturedAuth)
    assertThat(properties.authentication.github).isSameAs(capturedGithub)
    assertThat(capturedGithub.clientSecret).isNull()
  }

  @Test
  fun `round-trips the full default tree, restoring fields and recomputing setter-less getters`() {
    val properties = TolgeeProperties()
    val cloudflare = properties.contentDelivery.cachePurging.cloudflare
    cloudflare.apiKey = "key"
    cloudflare.urlPrefix = "https://cdn.example.com"
    cloudflare.zoneId = "zone"
    assertThat(cloudflare.enabled).isTrue()

    val snapshot = ConfigurationPropertiesSnapshot.snapshot(properties)
    cloudflare.apiKey = null
    cloudflare.urlPrefix = null
    cloudflare.zoneId = null
    assertThat(cloudflare.enabled).isFalse()

    ConfigurationPropertiesSnapshot.restore(properties, snapshot)

    assertThat(cloudflare.apiKey).isEqualTo("key")
    assertThat(cloudflare.zoneId).isEqualTo("zone")
    assertThat(cloudflare.enabled).isTrue()
  }

  @Test
  fun `a nested config object reassigned wholesale after snapshot leaves a captured reference stale`() {
    val properties = TolgeeProperties()
    val capturedOriginal = properties.authentication.github
    capturedOriginal.clientSecret = "snapshot-value"

    val snapshot = ConfigurationPropertiesSnapshot.snapshot(properties)
    val reassigned = GithubAuthenticationProperties()
    properties.authentication.github = reassigned
    capturedOriginal.clientSecret = "leaked"

    ConfigurationPropertiesSnapshot.restore(properties, snapshot)

    assertThat(properties.authentication.github).isSameAs(reassigned)
    assertThat(properties.authentication.github.clientSecret).isEqualTo("snapshot-value")
    assertThat(capturedOriginal.clientSecret).isEqualTo("leaked")
  }
}
