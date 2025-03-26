package io.tolgee.hateoas.prompt

import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation

@Relation(collectionRelation = "prompt", itemRelation = "prompt")
open class PromptModel(
  val id: Long,
  val name: String,
  val template: String,
  val projectId: Long,
  val providerName: String,
) : RepresentationModel<PromptModel>()
