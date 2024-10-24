package io.tolgee.hateoas.translationAgency

import io.tolgee.dtos.Avatar
import io.tolgee.hateoas.task.TaskModel
import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation

@Relation(collectionRelation = "translationAgencies", itemRelation = "translationAgency")
class TranslationAgencyModel(
  var id: Long = 0L,
  var name: String = "",
  var description: String? = "",
  var services: List<String> = listOf(),
  var url: String? = "",
  val avatar: Avatar?,
) : RepresentationModel<TaskModel>()
