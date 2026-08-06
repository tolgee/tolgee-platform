package io.tolgee.hateoas.apps

import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation

@Relation(collectionRelation = "appDeliveries", itemRelation = "appDelivery")
open class AppDeliveryModel(
  val id: Long,
  @Schema(description = "The lifecycle event as the app sees it, e.g. `app.installed`")
  val eventType: String,
  val targetUrl: String,
  @Schema(description = "The organization the event concerns, or null for app-level events")
  val organizationId: Long?,
  val createdAt: Long,
  val attempts: Int,
  val lastAttemptAt: Long?,
  val lastError: String?,
  val deliveredAt: Long?,
  @Schema(
    description =
      "When retrying stopped without success. The operation that triggered the delivery still " +
        "happened — issue a new secret to have the credentials delivered again.",
  )
  val abandonedAt: Long?,
) : RepresentationModel<AppDeliveryModel>()
