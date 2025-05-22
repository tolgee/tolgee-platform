package io.tolgee.hateoas.ee.webhooks

import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation
import java.io.Serializable

@Suppress("unused")
@Relation(collectionRelation = "webhookConfigs", itemRelation = "webhookConfig")
class WebhookConfigModel(
  val id: Long,
  val url: String,
  val webhookSecret: String,
  @Schema(
    description =
      "Date of the first failed webhook request. " +
        "If the last webhook request is successful, this value is set to null.",
  )
  val firstFailed: Long?,
  @Schema(
    description = """Date of the last webhook request.""",
  )
  var lastExecuted: Long?,
) : RepresentationModel<WebhookConfigModel>(), Serializable
