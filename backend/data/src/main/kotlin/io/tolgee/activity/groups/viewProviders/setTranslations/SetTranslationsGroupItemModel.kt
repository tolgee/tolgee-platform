package io.tolgee.activity.groups.viewProviders.setTranslations

import io.tolgee.activity.data.PropertyModification
import io.tolgee.activity.groups.baseModels.ActivityGroupKeyModel
import org.springframework.hateoas.server.core.Relation

@Relation(collectionRelation = "items", itemRelation = "item")
class SetTranslationsGroupItemModel(
  val id: Long,
  val text: PropertyModification<String>,
  val key: ActivityGroupKeyModel,
)
