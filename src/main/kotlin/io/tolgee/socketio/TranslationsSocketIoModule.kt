package io.tolgee.socketio

import com.corundumstudio.socketio.SocketIONamespace
import com.corundumstudio.socketio.SocketIOServer
import io.tolgee.api.v2.hateoas.key.KeyModelAssembler
import io.tolgee.api.v2.hateoas.key.KeyModifiedModel
import io.tolgee.api.v2.hateoas.translations.TranslationSocketModel
import io.tolgee.model.Project
import io.tolgee.model.key.Key
import io.tolgee.model.translation.Translation
import org.springframework.stereotype.Component

@Component
class TranslationsSocketIoModule(
  server: SocketIOServer,
  private val keyModelAssembler: KeyModelAssembler,
  translationsConnectionListener: TranslationsConnectionListener
) {
  private var namespace: SocketIONamespace = server.addNamespace("/translations")

  init {
    translationsConnectionListener.listen()
  }

  fun onKeyCreated(key: Key) {
    onKeyChange(key, TranslationEvent.KEY_CREATED)
  }

  fun onKeyModified(key: Key, oldName: String) {
    key.project?.getRoomName()?.let { roomName ->
      namespace.getRoomOperations(roomName).sendEvent(
        TranslationEvent.KEY_MODIFIED.eventName,
        KeyModifiedModel(key.id, key.name, oldName)
      )
    }
  }

  fun onKeyDeleted(key: Key) {
    onKeyChange(key, TranslationEvent.KEY_DELETED)
  }

  fun onKeyChange(key: Key, event: TranslationEvent) {
    key.project?.getRoomName()?.let { roomName ->
      namespace.getRoomOperations(roomName).sendEvent(
        event.eventName,
        keyModelAssembler.toModel(key)
      )
    }
  }

  fun onTranslationsCreated(translations: Collection<Translation>) {
    onTranslationsChange(translations, TranslationEvent.TRANSLATION_CREATED)
  }

  fun onTranslationsModified(translations: Collection<Translation>) {
    onTranslationsChange(translations, TranslationEvent.TRANSLATION_MODIFIED)
  }

  fun onTranslationsDeleted(translations: Collection<Translation>) {
    onTranslationsChange(translations, TranslationEvent.TRANSLATION_DELETED)
  }

  fun onTranslationsChange(translations: Collection<Translation>, event: TranslationEvent) {
    translations.map { translation ->
      translation.key?.project?.getRoomName()?.let { roomName ->
        namespace.getRoomOperations(roomName).sendEvent(
          event.eventName,
          TranslationSocketModel(
            id = translation.id,
            text = translation.text,
            state = translation.state,
            languageTag = translation.language?.tag ?: "",
            key = keyModelAssembler.toModel(translation.key!!)
          )
        )
      }
    }
  }

  private fun Project.getRoomName() = "translations-${this.id}"
}
