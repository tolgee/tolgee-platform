package io.tolgee.configuration

import io.hypersistence.utils.hibernate.type.util.ObjectMapperSupplier
import io.hypersistence.utils.hibernate.type.util.ObjectMapperWrapper
import tools.jackson.databind.ObjectMapper
import tools.jackson.databind.cfg.DateTimeFeature
import tools.jackson.databind.json.JsonMapper

/** Keeps JSONB dates as epoch millis (Jackson 3 defaults them off); registered via the `hypersistence.utils.jackson.object.mapper` property. */
class JsonbObjectMapperSupplier : ObjectMapperSupplier {
  override fun get(): ObjectMapper =
    (ObjectMapperWrapper.INSTANCE.objectMapper as JsonMapper)
      .rebuild()
      .enable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
      .build()
}
