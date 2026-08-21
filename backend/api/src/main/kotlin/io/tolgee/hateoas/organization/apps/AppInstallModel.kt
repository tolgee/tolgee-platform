package io.tolgee.hateoas.organization.apps

import io.swagger.v3.oas.annotations.media.Schema
import io.tolgee.dtos.apps.AppManifestModulesDto
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
  @Schema(
    description =
      "The app's logo: an emoji, a native Tolgee icon name, or an absolute image URL served by " +
        "the app's own host. Null when the manifest declares none.",
  )
  val icon: String? = null,
  @Schema(description = "How many of this organization's projects the app is currently enabled for")
  val enabledProjectCount: Long = 0,
  val modules: AppManifestModulesDto,
  val scopes: List<String>,
  @Schema(
    description =
      "Scopes the app's manifest currently requests beyond what this organization granted. " +
        "Non-empty means the app is asking for more access — the organization approves (or sees " +
        "exactly what changed) through the refresh action.",
  )
  val pendingScopes: List<String> = emptyList(),
  /**
   * The registered app this is an installation of. Two organizations installing the same manifest
   * see the same app here, each with their own install.
   */
  val app: AppModel? = null,
) : RepresentationModel<AppInstallModel>()
