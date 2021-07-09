package io.tolgee.socketio

import com.corundumstudio.socketio.SocketIONamespace
import com.corundumstudio.socketio.SocketIOServer
import io.tolgee.api.v2.hateoas.key.KeyModelAssembler
import io.tolgee.api.v2.hateoas.key.KeyModifiedModel
import io.tolgee.api.v2.hateoas.translations.TranslationWithKeyModel
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

  fun onKeyModified(key: Key, oldName: String) {
    key.project?.getRoomName()?.let { roomName ->
      namespace.getRoomOperations(roomName).sendEvent(
        "key_modified",
        KeyModifiedModel(key.id, key.name, oldName)
      )
    }
  }

  fun onKeyCreated(key: Key) {
    onKeyChange(key, "key_created")
  }

  fun onKeyDeleted(key: Key) {
    onKeyChange(key, "key_deleted")
  }

  fun onKeyChange(key: Key, eventName: String) {
    key.project?.getRoomName()?.let { roomName ->
      namespace.getRoomOperations(roomName).sendEvent(
        eventName,
        keyModelAssembler.toModel(key)
      )
    }
  }

  fun onTranslationsModified(translations: Collection<Translation>) {
    onTranslationsChange(translations, "translation_modified")
  }

  fun onTranslationsCreated(translations: Collection<Translation>) {
    onTranslationsChange(translations, "translation_created")
  }

  fun onTranslationsDeleted(translations: Collection<Translation>) {
    onTranslationsChange(translations, "translation_deleted")
  }

  fun onTranslationsChange(translations: Collection<Translation>, eventName: String) {
    translations.map { translation ->
      translation.key?.project?.getRoomName()?.let { roomName ->
        namespace.getRoomOperations(roomName).sendEvent(
          eventName,
          TranslationWithKeyModel(
            id = translation.id,
            text = translation.text,
            state = translation.state,
            key = keyModelAssembler.toModel(translation.key!!)
          )
        )
      }
    }
  }

  private fun Project.getRoomName() = "translations-${this.id}"
}
