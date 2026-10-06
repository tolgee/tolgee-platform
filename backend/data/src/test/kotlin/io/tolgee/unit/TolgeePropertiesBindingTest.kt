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
    val props =
      Binder(MapConfigurationPropertySource(mapOf("tolgee.max-screenshots-per-key" to "5")))
        .bind("tolgee", Bindable.ofInstance(TolgeeProperties()))
        .get()

    assertThat(props.maxScreenshotsPerKey).isEqualTo(5)
  }
}
