package io.tolgee.hateoas.oauth2

/** A client the user has authorized; revoking it drops all of that client's grants (per-client, not per-project). */
data class ConnectedAppModel(
  val clientId: String,
  val clientName: String,
  val scopes: List<String>,
  val lastAuthorizedAt: Long?,
)
