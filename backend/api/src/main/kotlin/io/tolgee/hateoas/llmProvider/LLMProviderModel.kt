package io.tolgee.hateoas.llmProvider

import io.tolgee.configuration.tolgee.machineTranslation.LLMProviderInterface
import io.tolgee.model.enums.LLMProviderType
import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation

@Relation(collectionRelation = "provider", itemRelation = "provider")
open class LLMProviderModel(
  var id: Long,
  override var name: String,
  override var type: LLMProviderType,
  override var priority: String?,
  override var apiKey: String?,
  override var apiUrl: String?,
  override var model: String?,
  override var deployment: String?,
  override var keepAlive: String?,
  override var format: String?,
) : RepresentationModel<LLMProviderModel>(), LLMProviderInterface
