package io.tolgee.activity.data

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.annotation.JsonDeserialize

/**
 * Backs `ActivityModifiedEntity.describingData`. Property names live in a
 * per-entity-class [NameSchema]; persisted JSON shape is `{"name": value, ...}`.
 */
@JsonDeserialize(using = DescribingDataMapDeserializer::class)
class DescribingDataMap(
  schema: NameSchema,
) : CompactSharedMap<Any?>(schema) {
  constructor() : this(NameSchema.anonymous())
  constructor(entityClass: String) : this(
    NameSchema.forEntityClass(entityClass, NameSchema.Companion.Scope.DESCRIBING_DATA),
  )
}

class DescribingDataMapDeserializer : JsonDeserializer<DescribingDataMap>() {
  override fun deserialize(
    p: JsonParser,
    ctxt: DeserializationContext,
  ): DescribingDataMap {
    val mapType =
      ctxt.typeFactory.constructMapType(
        LinkedHashMap::class.java,
        String::class.java,
        Any::class.java,
      )
    val map: Map<String, Any?> = ctxt.readValue(p, mapType)
    val result = DescribingDataMap()
    map.forEach { (k, v) -> result.put(k, v) }
    return result
  }
}
