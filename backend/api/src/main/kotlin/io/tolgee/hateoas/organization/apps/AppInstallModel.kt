package io.tolgee.hateoas.organization.apps

import io.tolgee.dtos.apps.AppManifestModules
import io.tolgee.hateoas.apps.AppModel
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
  /**
   * Self-registration only: whether this call created the install, as opposed to repointing an
   * existing one at a new manifest URL. Absent elsewhere.
   */
  val created: Boolean? = null,
  /**
   * The registered app this is an installation of. Two organizations installing the same manifest
   * see the same app here, each with their own install.
   */
  val app: AppModel? = null,
) : RepresentationModel<AppInstallModel>()
