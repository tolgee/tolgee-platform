package io.tolgee.dtos.apps

import com.fasterxml.jackson.annotation.JsonAnySetter
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * The alpha of Tolgee Apps supports exactly one module type: `project-dashboard-page`. Any other
 * top-level property or module key a manifest declares is captured into [unknownProperties] /
 * [AppManifestModulesDto.unknownModules] and rejected by validation, so an app author gets an
 * explicit error rather than silently having an unsupported capability ignored.
 */
data class AppManifestDto(
  val id: String,
  val name: String,
  val version: String,
  val baseUrl: String,
  val modules: AppManifestModulesDto,
  val scopes: List<String>? = null,
  /**
   * The app's logo: an emoji, a native Tolgee icon name, or an image URL (absolute, or relative to
   * [baseUrl]). Tolgee stores only the string — an image is loaded by the browser straight from the
   * app's host, never through Tolgee's file storage.
   */
  val icon: String? = null,
  /**
   * Which revision of the app contract the app speaks. Optional and defaults to 1 — the only
   * version this alpha defines. Declared so a manifest may carry it without being rejected as an
   * unknown property, and so a future breaking revision can be told apart from this one.
   */
  val protocolVersion: Int? = null,
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

data class AppManifestModulesDto(
  @JsonProperty("project-dashboard-page")
  val projectDashboardPage: List<ProjectDashboardPageModuleDto>? = null,
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

data class ProjectDashboardPageModuleDto(
  val key: String,
  val title: String,
  val icon: String,
  val entry: String,
)
