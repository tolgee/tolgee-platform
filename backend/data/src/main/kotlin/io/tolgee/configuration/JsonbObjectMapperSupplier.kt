package io.tolgee.configuration

import io.hypersistence.utils.hibernate.type.util.ObjectMapperSupplier
import io.hypersistence.utils.hibernate.type.util.ObjectMapperWrapper
import tools.jackson.databind.ObjectMapper
import tools.jackson.databind.cfg.DateTimeFeature
import tools.jackson.databind.json.JsonMapper

/**
 * Serializes dates in JSONB columns as epoch millis. Jackson 3 flipped
 * [DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS] to disabled by default, which would write dates as
 * ISO strings and change persisted JSONB (e.g. activity modification values). Registered through
 * the `hypersistence.utils.jackson.object.mapper` property in `hypersistence-utils.properties`.
 */
class JsonbObjectMapperSupplier : ObjectMapperSupplier {
  override fun get(): ObjectMapper =
    (ObjectMapperWrapper.INSTANCE.objectMapper as JsonMapper)
      .rebuild()
      .enable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
      .build()
}
