package io.tolgee.activity.groups.dataProviders

import com.fasterxml.jackson.databind.ObjectMapper
import io.tolgee.activity.groups.ActivityGroupType
import io.tolgee.activity.groups.data.ModifiedEntityView
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable

class PagedRelevantModifiedEntitiesProvider(
  jooqContext: DSLContext,
  objectMapper: ObjectMapper,
  groupType: ActivityGroupType,
  entityClasses: List<String>,
  groupId: Long,
  private val pageable: Pageable,
) {
  private val baseProvider =
    RelevantModifiedEntitiesProvider(
      jooqContext = jooqContext,
      objectMapper = objectMapper,
      groupType = groupType,
      entityClasses = entityClasses,
      additionalFilter = { it.groupIdField.eq(groupId) },
    )

  fun provide(): Page<ModifiedEntityView> {
    val count = getCountQuery().fetchOne(0, Int::class.java) ?: 0
    val content = baseProvider.provide(pageable)
    return PageImpl(content, pageable, count.toLong())
  }

  private fun getCountQuery() = baseProvider.getQueryBase(DSL.count())
}
