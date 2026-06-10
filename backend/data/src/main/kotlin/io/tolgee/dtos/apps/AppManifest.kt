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
  @JsonProperty("translation-tools-panel-empty")
  val translationToolsPanelEmpty: List<TranslationToolsPanelModule>? = null,
  @JsonProperty("key-edit-tab")
  val keyEditTab: List<KeyEditTabModule>? = null,
  @JsonProperty("key-action")
  val keyAction: List<KeyActionModule>? = null,
  @JsonProperty("translation-action")
  val translationAction: List<TranslationActionModule>? = null,
  @JsonProperty("modal")
  val modal: List<ModalModule>? = null,
  @JsonProperty("bulk-action")
  val bulkAction: List<BulkActionModule>? = null,
  @JsonProperty("translations-toolbar-action")
  val translationsToolbarAction: List<TranslationsToolbarActionModule>? = null,
  @JsonProperty("project-menu-action")
  val projectMenuAction: List<ProjectMenuActionModule>? = null,
  @JsonProperty("shortcut")
  val shortcut: List<ShortcutModule>? = null,
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
  val modalKey: String? = null,
  val visibility: AppActionVisibility? = null,
)

data class TranslationActionModule(
  val key: String,
  val type: AppActionType,
  val icon: String,
  val tooltip: String,
  val dynamic: Boolean = false,
  val urlTemplate: String? = null,
  val panelKey: String? = null,
  val modalKey: String? = null,
  val visibility: AppActionVisibility? = null,
)

data class ModalModule(
  val key: String,
  val title: String,
  val entry: String,
  val icon: String? = null,
  val width: Int? = null,
  val height: Int? = null,
)

data class BulkActionModule(
  val key: String,
  val title: String,
  val type: AppActionType,
  val icon: String? = null,
  val urlTemplate: String? = null,
  val modalKey: String? = null,
)

data class TranslationsToolbarActionModule(
  val key: String,
  val title: String,
  val type: AppActionType,
  val icon: String? = null,
  val urlTemplate: String? = null,
  val modalKey: String? = null,
)

data class ProjectMenuActionModule(
  val key: String,
  val title: String,
  val type: AppActionType,
  val icon: String? = null,
  val urlTemplate: String? = null,
  val modalKey: String? = null,
)

data class ShortcutModule(
  val key: String,
  val combination: String,
  val type: AppActionType,
  val title: String? = null,
  val urlTemplate: String? = null,
  val modalKey: String? = null,
)

enum class AppActionType {
  @JsonProperty("link")
  LINK,

  @JsonProperty("panel")
  PANEL,

  @JsonProperty("tab")
  TAB,

  @JsonProperty("modal")
  MODAL,
}

enum class AppActionVisibility {
  @JsonProperty("always")
  ALWAYS,

  @JsonProperty("on-hover")
  ON_HOVER,
}
