package io.tolgee.service.apps

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.tolgee.activity.data.ActivityType
import io.tolgee.configuration.tolgee.AppsProperties
import io.tolgee.constants.Message
import io.tolgee.dtos.apps.AppActionType
import io.tolgee.dtos.apps.AppManifest
import io.tolgee.exceptions.BadRequestException
import io.tolgee.model.enums.Scope
import io.tolgee.util.UrlSecurity
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestTemplate
import java.net.URI

@Component
class AppManifestFetcher(
  private val restTemplate: RestTemplate,
  private val objectMapper: ObjectMapper,
  private val urlSecurity: UrlSecurity,
  private val appsProperties: AppsProperties,
) {
  data class FetchResult(
    val manifest: AppManifest,
    val rawJson: String,
    val scopes: Set<Scope>,
    val webhookEvents: Set<String>,
    val resolvedWebhookUrl: String?,
  )

  fun fetch(url: String): FetchResult {
    urlSecurity.validateUrl(url, allowLocalAddresses = appsProperties.allowLocalAddresses)

    val rawJson =
      try {
        restTemplate.getForObject(url, String::class.java)
          ?: throw BadRequestException(Message.APP_MANIFEST_FETCH_FAILED)
      } catch (e: RestClientException) {
        throw BadRequestException(Message.APP_MANIFEST_FETCH_FAILED, listOf(e.message ?: ""))
      }

    val manifest =
      try {
        objectMapper.readValue<AppManifest>(rawJson)
      } catch (e: Exception) {
        throw BadRequestException(Message.APP_MANIFEST_INVALID, listOf(e.message ?: ""))
      }

    val scopes =
      try {
        Scope.parse(manifest.scopes)
      } catch (e: BadRequestException) {
        throw BadRequestException(
          Message.APP_MANIFEST_INVALID,
          listOf("unknown scope: ${e.params?.firstOrNull() ?: ""}"),
        )
      }

    val webhookEvents = parseWebhookEvents(manifest)
    val resolvedWebhookUrl = resolveWebhookUrl(manifest)
    validateActions(manifest)

    return FetchResult(
      manifest = manifest,
      rawJson = rawJson,
      scopes = scopes,
      webhookEvents = webhookEvents,
      resolvedWebhookUrl = resolvedWebhookUrl,
    )
  }

  private fun parseWebhookEvents(manifest: AppManifest): Set<String> {
    val declared = manifest.webhooks?.events ?: return emptySet()
    declared.forEach { event ->
      if (event !in SUPPORTED_APP_EVENTS) {
        throw BadRequestException(
          Message.APP_MANIFEST_INVALID,
          listOf("unknown webhook event: $event"),
        )
      }
    }
    return declared.toSet()
  }

  /**
   * Returns true if the given manifest event name maps to a known
   * [ActivityType]. Plugins may subscribe to any activity type by its
   * enum name (e.g. `SET_TRANSLATIONS`, `CREATE_KEY`, `BATCH_KEY_RESTORE`).
   */
  private val SUPPORTED_APP_EVENTS: Set<String>
    get() = ActivityType.entries.map { it.name }.toSet()

  private fun validateActions(manifest: AppManifest) {
    val tabKeys =
      manifest.modules.keyEditTab
        ?.map { it.key }
        ?.toSet()
        .orEmpty()
    val panelKeys =
      manifest.modules.translationToolsPanel
        ?.map { it.key }
        ?.toSet()
        .orEmpty()
    val modalKeys =
      manifest.modules.modal
        ?.map { it.key }
        ?.toSet()
        .orEmpty()

    manifest.modules.keyAction?.forEach { action ->
      when (action.type) {
        AppActionType.LINK ->
          requireUrlTemplateForLink(
            location = "key-action",
            actionKey = action.key,
            urlTemplate = action.urlTemplate,
            dynamic = action.dynamic,
          )
        AppActionType.TAB ->
          requireRef(
            location = "key-action",
            actionKey = action.key,
            refKind = "key-edit-tab",
            refField = "tabKey",
            ref = action.tabKey,
            known = tabKeys,
          )
        AppActionType.MODAL ->
          requireRef(
            location = "key-action",
            actionKey = action.key,
            refKind = "modal",
            refField = "modalKey",
            ref = action.modalKey,
            known = modalKeys,
          )
        AppActionType.PANEL ->
          throw BadRequestException(
            Message.APP_MANIFEST_INVALID,
            listOf("key-action '${action.key}' cannot use type panel"),
          )
      }
    }

    manifest.modules.translationAction?.forEach { action ->
      when (action.type) {
        AppActionType.LINK ->
          requireUrlTemplateForLink(
            location = "translation-action",
            actionKey = action.key,
            urlTemplate = action.urlTemplate,
            dynamic = action.dynamic,
          )
        AppActionType.PANEL ->
          requireRef(
            location = "translation-action",
            actionKey = action.key,
            refKind = "translation-tools-panel",
            refField = "panelKey",
            ref = action.panelKey,
            known = panelKeys,
          )
        AppActionType.MODAL ->
          requireRef(
            location = "translation-action",
            actionKey = action.key,
            refKind = "modal",
            refField = "modalKey",
            ref = action.modalKey,
            known = modalKeys,
          )
        AppActionType.TAB ->
          throw BadRequestException(
            Message.APP_MANIFEST_INVALID,
            listOf("translation-action '${action.key}' cannot use type tab"),
          )
      }
    }

    manifest.modules.bulkAction?.forEach {
      triggerAction("bulk-action", it.key, it.type, it.urlTemplate, it.modalKey, modalKeys)
    }
    manifest.modules.translationsToolbarAction?.forEach {
      triggerAction("translations-toolbar-action", it.key, it.type, it.urlTemplate, it.modalKey, modalKeys)
    }
    manifest.modules.projectMenuAction?.forEach {
      triggerAction("project-menu-action", it.key, it.type, it.urlTemplate, it.modalKey, modalKeys)
    }
    manifest.modules.shortcut?.forEach { shortcut ->
      if (shortcut.combination.isBlank()) {
        throw BadRequestException(
          Message.APP_MANIFEST_INVALID,
          listOf("shortcut '${shortcut.key}' requires a non-blank combination"),
        )
      }
      triggerAction("shortcut", shortcut.key, shortcut.type, shortcut.urlTemplate, shortcut.modalKey, modalKeys)
    }
  }

  private fun requireUrlTemplateForLink(
    location: String,
    actionKey: String,
    urlTemplate: String?,
    dynamic: Boolean,
  ) {
    if (!dynamic && urlTemplate.isNullOrBlank()) {
      throw BadRequestException(
        Message.APP_MANIFEST_INVALID,
        listOf("$location '$actionKey' of type link requires urlTemplate when not dynamic"),
      )
    }
  }

  private fun requireRef(
    location: String,
    actionKey: String,
    refKind: String,
    refField: String,
    ref: String?,
    known: Set<String>,
  ) {
    if (ref.isNullOrBlank()) {
      throw BadRequestException(
        Message.APP_MANIFEST_INVALID,
        listOf("$location '$actionKey' of type $refKind requires $refField"),
      )
    }
    if (ref !in known) {
      throw BadRequestException(
        Message.APP_MANIFEST_INVALID,
        listOf("$location '$actionKey' references unknown $refKind '$ref'"),
      )
    }
  }

  /**
   * Validates a trigger action (bulk-action, translations-toolbar-action,
   * project-menu-action, shortcut). Trigger actions support only `link` and
   * `modal` types.
   */
  private fun triggerAction(
    location: String,
    actionKey: String,
    type: AppActionType,
    urlTemplate: String?,
    modalKey: String?,
    modalKeys: Set<String>,
  ) {
    when (type) {
      AppActionType.LINK -> requireUrlTemplateForLink(location, actionKey, urlTemplate, dynamic = false)
      AppActionType.MODAL -> requireRef(location, actionKey, "modal", "modalKey", modalKey, modalKeys)
      AppActionType.PANEL,
      AppActionType.TAB,
      ->
        throw BadRequestException(
          Message.APP_MANIFEST_INVALID,
          listOf("$location '$actionKey' cannot use type ${type.name.lowercase()}"),
        )
    }
  }

  private fun resolveWebhookUrl(manifest: AppManifest): String? {
    val webhookUrl = manifest.webhooks?.url ?: return null
    return try {
      URI(manifest.baseUrl).resolve(webhookUrl).toString()
    } catch (e: Exception) {
      throw BadRequestException(
        Message.APP_MANIFEST_INVALID,
        listOf("invalid webhook url: ${e.message ?: webhookUrl}"),
      )
    }
  }
}
