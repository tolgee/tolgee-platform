package io.tolgee.testing

import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.MapperFeature
import tools.jackson.databind.SerializationFeature
import tools.jackson.module.kotlin.jacksonMapperBuilder

/**
 * Restore updates the existing object graph in place (`readerForUpdating` + merging on) rather than
 * replacing nested objects, so a bean that captured a sub-properties instance by reference keeps
 * observing the restored values.
 */
object ConfigurationPropertiesSnapshot {
  private val mapper =
    jacksonMapperBuilder()
      .findAndAddModules()
      .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
      // Both are off by default since Jackson 3. Without them a `val` collection property is skipped
      // on restore, and a snapshot that no longer matches the bean is ignored instead of failing.
      .configure(MapperFeature.ALLOW_FINAL_FIELDS_AS_MUTATORS, true)
      .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true)
      .defaultMergeable(true)
      .apply {
        // Merging a collection/map appends to it; replace instead so a snapshotted baseline is
        // restored exactly rather than concatenated with the test's in-place additions. configOverride
        // is matched by the property's concrete type and does not cascade from Collection, so every
        // declared container type (List/Set/Map) is listed explicitly.
        listOf(Collection::class, List::class, Set::class, Map::class).forEach {
          withConfigOverride(it.java) { override -> override.setMergeable(false) }
        }
      }.build()

  fun snapshot(bean: Any): String = mapper.writeValueAsString(bean)

  fun restore(
    bean: Any,
    snapshot: String,
  ) {
    mapper.readerForUpdating(bean).readValue<Any>(snapshot)
  }
}
