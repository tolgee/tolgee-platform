package io.tolgee.hateoas.prompt

import io.tolgee.model.enums.BasicPromptOption
import io.tolgee.model.enums.PromptType
import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation

@Relation(collectionRelation = "prompts", itemRelation = "prompt")
open class PromptModel(
  val id: Long,
  val name: String,
  val template: String?,
  val projectId: Long,
  val providerName: String,
  val basicPromptOptions: List<BasicPromptOption>?,
  val type: PromptType = PromptType.TRANSLATE,
  val reviewCriteria: String? = null,
) : RepresentationModel<PromptModel>()
