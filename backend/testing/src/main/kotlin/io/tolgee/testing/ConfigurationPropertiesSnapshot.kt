package io.tolgee.testing

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper

/**
 * Restore updates the existing object graph in place (`readerForUpdating` + merging on) rather than
 * replacing nested objects, so a bean that captured a sub-properties instance by reference keeps
 * observing the restored values.
 */
object ConfigurationPropertiesSnapshot {
  private val mapper =
    jacksonObjectMapper()
      .findAndRegisterModules()
      .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
      .apply {
        setDefaultMergeable(true)
        // Merging a collection/map appends to it; replace instead so a snapshotted baseline is
        // restored exactly rather than concatenated with the test's in-place additions. configOverride
        // is matched by the property's concrete type and does not cascade from Collection, so every
        // declared container type (List/Set/Map) is listed explicitly.
        listOf(Collection::class, List::class, Set::class, Map::class).forEach {
          configOverride(it.java).mergeable = false
        }
      }

  fun snapshot(bean: Any): String = mapper.writeValueAsString(bean)

  fun restore(
    bean: Any,
    snapshot: String,
  ) {
    mapper.readerForUpdating(bean).readValue<Any>(snapshot)
  }
}
