package io.tolgee.unit

import io.tolgee.configuration.annotations.DocProperty
import io.tolgee.configuration.tolgee.TolgeeProperties
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.context.properties.bind.DataObjectPropertyName
import java.lang.reflect.Modifier

class TolgeePropertiesDocPrefixTest {
  @Test
  fun `every documented prefix is the key the properties class is bound at`() {
    val mismatches = mutableListOf<String>()
    collectMismatches(TolgeeProperties::class.java, "tolgee", mismatches)

    assertThat(mismatches).isEmpty()
  }

  private fun collectMismatches(
    type: Class<*>,
    boundAt: String,
    mismatches: MutableList<String>,
  ) {
    val documented = type.getAnnotation(DocProperty::class.java)?.prefix.orEmpty()
    if (documented.isNotEmpty() && documented != boundAt) {
      mismatches += "${type.simpleName} is documented at $documented but bound at $boundAt"
    }
    type.declaredFields
      .filter { !Modifier.isStatic(it.modifiers) && !it.type.isEnum }
      .filter { it.type.name.startsWith("io.tolgee.configuration.") }
      .forEach {
        collectMismatches(it.type, "$boundAt.${DataObjectPropertyName.toDashedForm(it.name)}", mismatches)
      }
  }
}
