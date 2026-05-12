package io.tolgee.activity.data

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.annotation.JsonDeserialize

/**
 * Backs `ActivityModifiedEntity.describingRelations`. Relation names live
 * in a per-entity-class [NameSchema]; persisted JSON shape is
 * `{"name": EntityDescriptionRef, ...}`.
 */
@JsonDeserialize(using = DescribingRelationsMapDeserializer::class)
class DescribingRelationsMap(
  schema: NameSchema,
) : CompactSharedMap<EntityDescriptionRef>(schema) {
  constructor() : this(NameSchema.anonymous())
  constructor(entityClass: String) : this(
    NameSchema.forEntityClass(entityClass, NameSchema.Companion.Scope.DESCRIBING_RELATIONS),
  )
}

class DescribingRelationsMapDeserializer : JsonDeserializer<DescribingRelationsMap>() {
  override fun deserialize(
    p: JsonParser,
    ctxt: DeserializationContext,
  ): DescribingRelationsMap {
    val mapType =
      ctxt.typeFactory.constructMapType(
        LinkedHashMap::class.java,
        String::class.java,
        EntityDescriptionRef::class.java,
      )
    val map: Map<String, EntityDescriptionRef> = ctxt.readValue(p, mapType)
    val result = DescribingRelationsMap()
    map.forEach { (k, v) -> result.put(k, v) }
    return result
  }
}
