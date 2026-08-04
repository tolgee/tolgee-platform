package io.tolgee.hateoas.apps

import com.fasterxml.jackson.annotation.JsonProperty
import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation

/**
 * OAuth 2.0 access-token response (RFC 6749 §5.1). No refresh token is issued for the
 * client-credentials grant (§4.4.3) — the app re-authenticates with its client credentials.
 */
@Relation(itemRelation = "appAccessToken")
open class AppAccessTokenModel(
  @get:JsonProperty("access_token")
  val accessToken: String,
  @get:JsonProperty("token_type")
  val tokenType: String,
  @get:JsonProperty("expires_in")
  val expiresIn: Long,
) : RepresentationModel<AppAccessTokenModel>()
