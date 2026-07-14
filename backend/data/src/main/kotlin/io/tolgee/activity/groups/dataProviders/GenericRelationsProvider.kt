package io.tolgee.activity.groups.dataProviders

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.tolgee.activity.groups.data.DescribingEntityView
import io.tolgee.activity.groups.data.ModifiedEntityView
import org.jooq.DSLContext
import org.jooq.JSON
import org.jooq.impl.DSL

/**
 * Resolves whatever describing relations the given modified entities happen to have,
 * without requiring a per-group-type mapping.
 */
class GenericRelationsProvider(
  private val jooqContext: DSLContext,
  private val objectMapper: ObjectMapper,
  private val entities: List<ModifiedEntityView>,
) {
  private val entityIdField = DSL.field("ade.entity_id", Long::class.java)
  private val entityClassField = DSL.field("ade.entity_class", String::class.java)
  private val dataField = DSL.field("ade.data", JSON::class.java)
  private val describingRelationsField = DSL.field("ade.describing_relations", JSON::class.java)
  private val activityRevisionIdField = DSL.field("ade.activity_revision_id", Long::class.java)

  /**
   * @return modified entity -> (relation field name -> describing entity)
   */
  fun provide(): Map<ModifiedEntityView, Map<String, DescribingEntityView>> {
    if (refs.isEmpty()) {
      return emptyMap()
    }

    val views =
      jooqContext
        .select(entityIdField, entityClassField, dataField, describingRelationsField, activityRevisionIdField)
        .from(DSL.table("activity_describing_entity").`as`("ade"))
        .where(
          DSL.or(
            refs.map { (entity, _, ref) ->
              entityClassField
                .eq(ref.entityClass)
                .and(entityIdField.eq(ref.entityId))
                .and(activityRevisionIdField.eq(entity.activityRevisionId))
            },
          ),
        ).fetch()
        .map {
          DescribingEntityView(
            entityId = it.get(entityIdField),
            entityClass = it.get(entityClassField),
            data = objectMapper.readValue(it.get(dataField).data()),
            describingRelations = objectMapper.readValue(it.get(describingRelationsField).data()),
            activityRevisionId = it.get(activityRevisionIdField),
          )
        }

    return refs
      .groupBy { it.first }
      .mapValues { (_, entityRefs) ->
        entityRefs
          .mapNotNull { (entity, field, ref) ->
            val view =
              views.find {
                it.entityId == ref.entityId &&
                  it.entityClass == ref.entityClass &&
                  it.activityRevisionId == entity.activityRevisionId
              } ?: return@mapNotNull null
            field to view
          }.toMap()
      }
  }

  private val refs by lazy {
    entities.flatMap { entity ->
      entity.describingRelations.mapNotNull { (field, ref) ->
        ref ?: return@mapNotNull null
        Triple(entity, field, ref)
      }
    }
  }
}
