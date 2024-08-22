package io.tolgee.hateoas.activity

import io.tolgee.activity.groups.ActivityGroupType
import io.tolgee.hateoas.userAccount.SimpleUserAccountModel
import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation
import java.io.Serializable

@Suppress("unused")
@Relation(collectionRelation = "groups", itemRelation = "group")
class ActivityGroupModel(
  val id: Long,
  val timestamp: Long,
  val type: ActivityGroupType,
  val author: SimpleUserAccountModel?,
  val data: Any?,
  var mentionedLanguageIds: List<Long>,
) : RepresentationModel<ActivityGroupModel>(), Serializable
