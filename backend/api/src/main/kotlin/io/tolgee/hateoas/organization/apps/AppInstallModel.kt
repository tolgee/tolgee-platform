package io.tolgee.hateoas.organization.apps

import io.tolgee.dtos.apps.AppManifestModules
import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation

@Relation(collectionRelation = "appInstalls", itemRelation = "appInstall")
open class AppInstallModel(
  val id: Long,
  val manifestUrl: String,
  val appId: String,
  val name: String,
  val version: String,
  val baseUrl: String,
  val modules: AppManifestModules,
  val scopes: List<String>,
  val clientId: String?,
  val clientSecretPrefix: String?,
  /**
   * The OAuth client secret in plaintext. Present only in the response to registration — it is never
   * stored and cannot be retrieved again.
   */
  val clientSecret: String? = null,
) : RepresentationModel<AppInstallModel>()
