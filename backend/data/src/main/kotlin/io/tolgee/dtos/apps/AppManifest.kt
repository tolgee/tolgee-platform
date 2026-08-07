package io.tolgee.dtos.apps

import com.fasterxml.jackson.annotation.JsonAnySetter
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * The alpha of Tolgee Apps supports exactly one module type: `project-dashboard-page`. Any other
 * top-level property or module key a manifest declares is captured into [unknownProperties] /
 * [AppManifestModules.unknownModules] and rejected by the fetcher, so an app author gets an explicit
 * error rather than silently having an unsupported capability ignored.
 */
data class AppManifest(
  val id: String,
  val name: String,
  val version: String,
  val baseUrl: String,
  val modules: AppManifestModules,
  val scopes: List<String>? = null,
) {
  @get:JsonIgnore
  val unknownProperties: MutableSet<String> = mutableSetOf()

  @JsonAnySetter
  private fun setUnknownProperty(
    name: String,
    value: Any?,
  ) {
    unknownProperties.add(name)
  }
}

data class AppManifestModules(
  @JsonProperty("project-dashboard-page")
  val projectDashboardPage: List<ProjectDashboardPageModule>? = null,
) {
  @get:JsonIgnore
  val unknownModules: MutableSet<String> = mutableSetOf()

  @JsonAnySetter
  private fun setUnknownModule(
    name: String,
    value: Any?,
  ) {
    unknownModules.add(name)
  }
}

data class ProjectDashboardPageModule(
  val key: String,
  val title: String,
  val icon: String,
  val entry: String,
)
