package io.tolgee.unit

import io.tolgee.configuration.tolgee.TolgeeProperties
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.context.properties.bind.Bindable
import org.springframework.boot.context.properties.bind.Binder
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource

class TolgeePropertiesBindingTest {
  @Test
  fun `binds max screenshots per key`() {
    assertThat(bind("tolgee.max-screenshots-per-key" to "5").maxScreenshotsPerKey).isEqualTo(5)
  }

  @Test
  fun `binds global sso force`() {
    assertThat(bind("tolgee.authentication.sso-global.force" to "true").authentication.ssoGlobal.force).isTrue()
  }

  private fun bind(property: Pair<String, String>): TolgeeProperties =
    Binder(MapConfigurationPropertySource(mapOf(property)))
      .bind("tolgee", Bindable.ofInstance(TolgeeProperties()))
      .get()
}
