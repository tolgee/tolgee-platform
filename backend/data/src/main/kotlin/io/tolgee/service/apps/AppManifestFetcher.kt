package io.tolgee.service.apps

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.tolgee.constants.Message
import io.tolgee.dtos.apps.AppActionType
import io.tolgee.dtos.apps.AppManifest
import io.tolgee.exceptions.BadRequestException
import io.tolgee.model.enums.Scope
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestTemplate
import java.net.URI

@Component
class AppManifestFetcher(
  private val restTemplate: RestTemplate,
  private val objectMapper: ObjectMapper,
) {
  data class FetchResult(
    val manifest: AppManifest,
    val rawJson: String,
    val scopes: Set<Scope>,
    val webhookEvents: Set<String>,
    val resolvedWebhookUrl: String?,
  )

  fun fetch(url: String): FetchResult {
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

  private fun validateActions(manifest: AppManifest) {
    val tabKeys = manifest.modules.keyEditTab?.map { it.key }?.toSet().orEmpty()
    val panelKeys = manifest.modules.translationToolsPanel?.map { it.key }?.toSet().orEmpty()

    manifest.modules.keyAction?.forEach { action ->
      when (action.type) {
        AppActionType.LINK -> {
          if (!action.dynamic && action.urlTemplate.isNullOrBlank()) {
            throw BadRequestException(
              Message.APP_MANIFEST_INVALID,
              listOf("key-action '${action.key}' of type link requires urlTemplate when not dynamic"),
            )
          }
        }
        AppActionType.TAB -> {
          val ref = action.tabKey
          if (ref.isNullOrBlank()) {
            throw BadRequestException(
              Message.APP_MANIFEST_INVALID,
              listOf("key-action '${action.key}' of type tab requires tabKey"),
            )
          }
          if (ref !in tabKeys) {
            throw BadRequestException(
              Message.APP_MANIFEST_INVALID,
              listOf("key-action '${action.key}' references unknown key-edit-tab '$ref'"),
            )
          }
        }
        AppActionType.PANEL ->
          throw BadRequestException(
            Message.APP_MANIFEST_INVALID,
            listOf("key-action '${action.key}' cannot use type panel"),
          )
      }
    }

    manifest.modules.translationAction?.forEach { action ->
      when (action.type) {
        AppActionType.LINK -> {
          if (!action.dynamic && action.urlTemplate.isNullOrBlank()) {
            throw BadRequestException(
              Message.APP_MANIFEST_INVALID,
              listOf("translation-action '${action.key}' of type link requires urlTemplate when not dynamic"),
            )
          }
        }
        AppActionType.PANEL -> {
          val ref = action.panelKey
          if (ref.isNullOrBlank()) {
            throw BadRequestException(
              Message.APP_MANIFEST_INVALID,
              listOf("translation-action '${action.key}' of type panel requires panelKey"),
            )
          }
          if (ref !in panelKeys) {
            throw BadRequestException(
              Message.APP_MANIFEST_INVALID,
              listOf("translation-action '${action.key}' references unknown translation-tools-panel '$ref'"),
            )
          }
        }
        AppActionType.TAB ->
          throw BadRequestException(
            Message.APP_MANIFEST_INVALID,
            listOf("translation-action '${action.key}' cannot use type tab"),
          )
      }
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

  companion object {
    val SUPPORTED_APP_EVENTS: Set<String> = setOf("translation.set")
  }
}
