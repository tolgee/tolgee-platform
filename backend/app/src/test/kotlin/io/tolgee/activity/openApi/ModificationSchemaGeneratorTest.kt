package io.tolgee.activity.openApi

import io.tolgee.configuration.openApi.activity.ModificationsSchemaGenerator
import io.tolgee.model.key.Key
import org.junit.jupiter.api.Test

class ModificationSchemaGeneratorTest {
  @Test
  fun `generates for key`() {
    val schema = ModificationsSchemaGenerator().getModificationSchema(Key::class)
  }
}
