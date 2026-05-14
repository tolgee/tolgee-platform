package io.tolgee.dtos.apps

import com.fasterxml.jackson.annotation.JsonProperty

data class AppManifest(
  val id: String,
  val name: String,
  val version: String,
  val baseUrl: String,
  val modules: AppManifestModules,
  val scopes: List<String>? = null,
)

data class AppManifestModules(
  @JsonProperty("project-dashboard-page")
  val projectDashboardPage: List<ProjectDashboardPageModule>? = null,
)

data class ProjectDashboardPageModule(
  val key: String,
  val title: String,
  val icon: String,
  val entry: String,
)
