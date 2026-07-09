package io.tolgee.activity.data

import tools.jackson.core.JsonParser
import tools.jackson.databind.DeserializationContext
import tools.jackson.databind.ValueDeserializer
import tools.jackson.databind.annotation.JsonDeserialize

/**
 * Backs `ActivityModifiedEntity.describingRelations`. Relation names live
 * in a per-entity-class [NameSchema]; persisted JSON shape is
 * `{"name": EntityDescriptionRef, ...}`.
 */
@JsonDeserialize(using = DescribingRelationsMapDeserializer::class)
class DescribingRelationsMap(
  schema: NameSchema,
) : CompactSharedMap<EntityDescriptionRef>(schema), Map<String, EntityDescriptionRef> {
  constructor() : this(NameSchema.anonymous())
  constructor(entityClass: String) : this(
    NameSchema.forEntityClass(entityClass, NameSchema.Companion.Scope.DESCRIBING_RELATIONS),
  )
}

class DescribingRelationsMapDeserializer : ValueDeserializer<DescribingRelationsMap>() {
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
