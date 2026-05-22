package io.tolgee.dtos.apps

import com.fasterxml.jackson.annotation.JsonProperty

data class AppManifest(
  val id: String,
  val name: String,
  val version: String,
  val baseUrl: String,
  val modules: AppManifestModules,
  val scopes: List<String>? = null,
  val webhooks: AppManifestWebhooks? = null,
  val decoratorsUrl: String? = null,
)

data class AppManifestWebhooks(
  val events: List<String>,
  val url: String,
)

data class AppManifestModules(
  @JsonProperty("project-dashboard-page")
  val projectDashboardPage: List<ProjectDashboardPageModule>? = null,
  @JsonProperty("translation-tools-panel")
  val translationToolsPanel: List<TranslationToolsPanelModule>? = null,
  @JsonProperty("key-edit-tab")
  val keyEditTab: List<KeyEditTabModule>? = null,
  @JsonProperty("key-action")
  val keyAction: List<KeyActionModule>? = null,
  @JsonProperty("translation-action")
  val translationAction: List<TranslationActionModule>? = null,
)

data class ProjectDashboardPageModule(
  val key: String,
  val title: String,
  val icon: String,
  val entry: String,
)

data class TranslationToolsPanelModule(
  val key: String,
  val title: String,
  val icon: String,
  val entry: String,
)

data class KeyEditTabModule(
  val key: String,
  val title: String,
  val icon: String,
  val entry: String,
)

data class KeyActionModule(
  val key: String,
  val type: AppActionType,
  val icon: String,
  val tooltip: String,
  val dynamic: Boolean = false,
  val urlTemplate: String? = null,
  val tabKey: String? = null,
)

data class TranslationActionModule(
  val key: String,
  val type: AppActionType,
  val icon: String,
  val tooltip: String,
  val dynamic: Boolean = false,
  val urlTemplate: String? = null,
  val panelKey: String? = null,
)

enum class AppActionType {
  @JsonProperty("link")
  LINK,

  @JsonProperty("panel")
  PANEL,

  @JsonProperty("tab")
  TAB,
}
