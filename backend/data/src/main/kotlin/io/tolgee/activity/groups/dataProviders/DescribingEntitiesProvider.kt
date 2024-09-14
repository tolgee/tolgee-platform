package io.tolgee.activity.groups.dataProviders

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.tolgee.activity.groups.data.DescribingEntityView
import io.tolgee.activity.groups.data.DescribingMapping
import io.tolgee.activity.groups.data.ModifiedEntityView
import org.jooq.DSLContext
import org.jooq.JSON
import org.jooq.impl.DSL

class DescribingEntitiesProvider(
  private val jooqContext: DSLContext,
  private val objectMapper: ObjectMapper,
  private val describingMapping: List<DescribingMapping>,
  private val entities: Iterable<ModifiedEntityView>,
) {
  private val describingEntityTable = DSL.table("activity_describing_entity").`as`("ade")
  private val entityIdField = DSL.field("ade.entity_id", Long::class.java)
  private val entityClassField = DSL.field("ade.entity_class", String::class.java)
  private val dataField = DSL.field("ade.data", JSON::class.java)
  private val describingRelationsField = DSL.field("ade.describing_relations", JSON::class.java)
  private val activityRevisionIdField = DSL.field("ade.activity_revision_id", Long::class.java)

  fun provide(): Map<ModifiedEntityView, List<DescribingEntityView>> {
    val views =
      query.fetch()
        .map {
          DescribingEntityView(
            entityId = it.get(entityIdField),
            entityClass = it.get(entityClassField),
            data = objectMapper.readValue(it.get(dataField).data()),
            describingRelations = objectMapper.readValue(it.get(describingRelationsField).data()),
            activityRevisionId = it.get(activityRevisionIdField),
          )
        }

    val grouped = refs.groupBy { it.first }

    return grouped.mapValues { (_, refs) ->
      refs.mapNotNull { (entity, ref) ->
        views.find {
          it.entityId == ref.entityId &&
            it.entityClass == ref.entityClass &&
            it.activityRevisionId == entity.activityRevisionId
        }
      }
    }
  }

  val query by lazy {
    jooqContext
      .select(entityIdField, entityClassField, dataField, describingRelationsField, activityRevisionIdField)
      .from(describingEntityTable)
      .where(condition)
  }

  val condition by lazy {
    DSL.or(
      refs.map { (entity, ref) ->
        DSL.condition(entityClassField.eq(ref.entityClass))
          .and(entityIdField.eq(ref.entityId))
          .and(activityRevisionIdField.eq(entity.activityRevisionId))
      },
    )
  }

  private val refs by lazy {
    describingMapping.flatMap { mapping ->
      entities.mapNotNull { entity ->
        if (entity.entityClass != mapping.entityClass.simpleName) {
          return@mapNotNull null
        }
        val ref = entity.describingRelations[mapping.field] ?: return@mapNotNull null
        return@mapNotNull entity to ref
      }
    }
  }
}
