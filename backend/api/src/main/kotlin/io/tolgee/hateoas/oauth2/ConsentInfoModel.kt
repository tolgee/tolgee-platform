package io.tolgee.hateoas.oauth2

data class ConsentInfoModel(
  val appName: String,
  val scopes: List<String>,
  val requiredScopes: List<String>,
  val project: OAuth2ProjectModel?,
  // The client's authorize-time project hint, regardless of access; set with a null [project] = a project the user can't edit here.
  val requestedProjectId: Long?,
  val verified: Boolean,
  // The code goes to a port on the user's machine, which no client_id can prove the ownership of.
  val redirectsToLocalApp: Boolean,
  val clientOrigin: String?,
)
