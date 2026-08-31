package io.tolgee.unit

import io.tolgee.configuration.PostgresAutoStartConfiguration
import io.tolgee.configuration.tolgee.PostgresAutostartProperties
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.sql.SQLException

class PostgresStartingUpTest {
  private val configuration = PostgresAutoStartConfiguration(PostgresAutostartProperties())

  @Test
  fun `recognises a server that is still starting`() {
    assertThat(configuration.isStartingUp(SQLException("whatever the locale says", "57P03"))).isTrue()
  }

  @Test
  fun `recognises it through the pool's wrapper`() {
    val wrapped = RuntimeException("could not get connection", SQLException("starting up", "57P03"))

    assertThat(configuration.isStartingUp(wrapped)).isTrue()
  }

  @Test
  fun `does not mistake other database failures for a starting server`() {
    assertThat(configuration.isStartingUp(SQLException("password authentication failed", "28P01"))).isFalse()
    assertThat(configuration.isStartingUp(SQLException("relation does not exist", "42P01"))).isFalse()
    assertThat(configuration.isStartingUp(IllegalStateException("not a sql problem at all"))).isFalse()
  }
}
