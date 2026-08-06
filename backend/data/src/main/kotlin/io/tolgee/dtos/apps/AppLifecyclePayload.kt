package io.tolgee.dtos.apps

import com.fasterxml.jackson.annotation.JsonInclude

/**
 * The body of a lifecycle delivery. It is what the signature covers, so its serialized form is
 * computed once and both signed and sent — never rebuilt.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class AppLifecyclePayload(
  val eventType: String,
  val deliveryId: Long,
  /** The manifest id of the app the event is about. */
  val appId: String,
  /**
   * Which Tolgee this came from. An app installed on several servers keeps one credential set per
   * server, and this is what tells them apart.
   */
  val tolgeeInstanceUrl: String?,
  val app: AppLifecycleAppCredentials? = null,
  val install: AppLifecycleInstall? = null,
  val organization: AppLifecycleOrganization? = null,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class AppLifecycleAppCredentials(
  val clientId: String,
  /** Present only when this delivery is the one disclosing it. */
  val clientSecret: String? = null,
  val webhookSecret: String? = null,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class AppLifecycleInstall(
  val id: Long,
  val clientId: String?,
  /** Present only when this delivery is the one disclosing it. */
  val clientSecret: String? = null,
  val scopes: List<String> = listOf(),
)

data class AppLifecycleOrganization(
  val id: Long,
  val name: String,
  val slug: String,
)
