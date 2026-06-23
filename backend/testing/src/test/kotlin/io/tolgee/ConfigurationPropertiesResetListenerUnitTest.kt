package io.tolgee

import com.example.ExternalConfigProperties
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.boot.context.properties.ConfigurationProperties

class ConfigurationPropertiesResetListenerUnitTest {
  private val listener = ConfigurationPropertiesResetListener()

  private class Unserializable {
    @Suppress("unused")
    val boom: String get() = throw IllegalArgumentException("getter blows up")
  }

  @ConfigurationProperties(prefix = "test.own")
  private class OwnRoot

  private data class Holder(
    val value: String,
  )

  @Test
  fun `wraps a snapshot failure with the offending bean name and class, chaining the cause`() {
    assertThatThrownBy {
      listener.snapshotBean("badBean", Unserializable())
    }.isInstanceOf(IllegalStateException::class.java)
      .hasMessageContaining("badBean")
      .hasMessageContaining(Unserializable::class.java.name)
      .hasCauseInstanceOf(Exception::class.java)
  }

  @Test
  fun `returns the json for a serializable bean`() {
    assertThat(listener.snapshotBean("ok", Holder("v"))).contains("\"value\":\"v\"")
  }

  @Test
  fun `wraps a restore failure with the offending bean name, and fails loud on an unknown property`() {
    assertThatThrownBy {
      listener.restoreBean("badBean", Holder("v"), """{"value":"v","ghost":1}""")
    }.isInstanceOf(IllegalStateException::class.java)
      .hasMessageContaining("badBean")
      .hasMessageContaining(Holder::class.java.name)
      .hasCauseInstanceOf(Exception::class.java)
  }

  @Test
  fun `selects only io_tolgee beans annotated on their own class`() {
    assertThat(listener.isResettableRoot(OwnRoot())).isTrue()
    assertThat(listener.isResettableRoot(ExternalConfigProperties())).isFalse()
    assertThat(listener.isResettableRoot(Holder("v"))).isFalse()
  }
}
