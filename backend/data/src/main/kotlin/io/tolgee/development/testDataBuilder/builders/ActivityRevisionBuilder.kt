package io.tolgee.development.testDataBuilder.builders

import io.tolgee.activity.data.RevisionType
import io.tolgee.model.activity.ActivityRevision

class ActivityRevisionBuilder(
  val projectBuilder: ProjectBuilder,
) : BaseEntityDataBuilder<ActivityRevision, ActivityRevisionBuilder>() {
  override var self: ActivityRevision = ActivityRevision()

  class ModifiedEntityRef(
    val entityClass: String,
    val revisionType: RevisionType,
    val modifications: Map<String, Pair<Any?, Any?>>,
    val entityIdProvider: () -> Long,
  )

  val modifiedEntities = mutableListOf<ModifiedEntityRef>()

  var authorIdProvider: () -> Long? = { null }

  fun addModifiedEntity(
    entityClass: String,
    revisionType: RevisionType = RevisionType.MOD,
    modifications: Map<String, Pair<Any?, Any?>> = emptyMap(),
    entityIdProvider: () -> Long,
  ) {
    modifiedEntities += ModifiedEntityRef(entityClass, revisionType, modifications, entityIdProvider)
  }
}
