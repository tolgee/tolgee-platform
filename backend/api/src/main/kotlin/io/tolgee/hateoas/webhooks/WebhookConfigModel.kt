package io.tolgee.hateoas.webhooks

import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation
import java.io.Serializable

@Suppress("unused")
@Relation(collectionRelation = "webhookConfigs", itemRelation = "webhookConfig")
class WebhookConfigModel(
  val id: Long,
  val url: String,
  val webhookSecret: String
) : RepresentationModel<WebhookConfigModel>(), Serializable
