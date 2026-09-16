package io.tolgee.hateoas.oauth2

data class ConsentInfoModel(
  val appName: String,
  val scopes: List<String>,
  val requiredScopes: List<String>,
  val project: OAuth2ProjectModel?,
  // The client's authorize-time project hint, regardless of access; set with a null [project] = a project the user can't edit here.
  val requestedProjectId: Long?,
  // A pre-registered client is verified; a client resolved from a CIMD document is not, and the consent screen warns.
  val verified: Boolean,
  // Origin of the client_id URL for an unverified (CIMD) client, shown as the identity the user can actually trust; null for a verified client.
  val clientOrigin: String?,
  // The client's self-asserted logo, kept only when same-origin; null for a verified client or when the document had none.
  val logoUri: String?,
)
