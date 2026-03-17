package io.tolgee.unit

import io.tolgee.batch.data.BatchJobType
import io.tolgee.configuration.tolgee.BatchJobTypeOverrideProperties
import io.tolgee.configuration.tolgee.BatchProperties
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class BatchPropertiesTest {
  @Test
  fun `isExclusive returns enum default when no override`() {
    val props = BatchProperties()

    assertThat(props.isExclusive(BatchJobType.MACHINE_TRANSLATE)).isTrue()
    assertThat(props.isExclusive(BatchJobType.AUTO_TRANSLATE)).isFalse()
    assertThat(props.isExclusive(BatchJobType.DELETE_KEYS)).isTrue()
    assertThat(props.isExclusive(BatchJobType.AUTOMATION)).isFalse()
  }

  @Test
  fun `isExclusive returns override value when configured`() {
    val props = BatchProperties()
    props.jobTypeOverrides =
      mapOf(
        BatchJobType.MACHINE_TRANSLATE to
          BatchJobTypeOverrideProperties().apply {
            exclusive = false
          },
      )

    assertThat(props.isExclusive(BatchJobType.MACHINE_TRANSLATE)).isFalse()
    // Other types still use enum default
    assertThat(props.isExclusive(BatchJobType.DELETE_KEYS)).isTrue()
    assertThat(props.isExclusive(BatchJobType.AUTO_TRANSLATE)).isFalse()
  }

  @Test
  fun `isExclusive can make non-exclusive type exclusive via override`() {
    val props = BatchProperties()
    props.jobTypeOverrides =
      mapOf(
        BatchJobType.AUTO_TRANSLATE to
          BatchJobTypeOverrideProperties().apply {
            exclusive = true
          },
      )

    assertThat(props.isExclusive(BatchJobType.AUTO_TRANSLATE)).isTrue()
  }

  @Test
  fun `isExclusive falls back to enum default when override has null exclusive`() {
    val props = BatchProperties()
    props.jobTypeOverrides =
      mapOf(
        BatchJobType.MACHINE_TRANSLATE to BatchJobTypeOverrideProperties(),
      )

    assertThat(props.isExclusive(BatchJobType.MACHINE_TRANSLATE)).isTrue()
  }
}
