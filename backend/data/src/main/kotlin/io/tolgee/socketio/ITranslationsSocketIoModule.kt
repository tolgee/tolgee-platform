package io.tolgee.socketio

import io.tolgee.model.key.Key
import io.tolgee.model.translation.Translation

interface ITranslationsSocketIoModule {
  fun onKeyCreated(key: Key)
  fun onKeyModified(key: Key, oldName: String)
  fun onKeyDeleted(key: Key)
  fun onKeyChange(key: Key, event: io.tolgee.socketio.TranslationEvent)
  fun onTranslationsCreated(translations: Collection<Translation>)
  fun onTranslationsModified(translations: Collection<Translation>)
  fun onTranslationsDeleted(translations: Collection<Translation>)
  fun onTranslationsChange(translations: Collection<Translation>, event: io.tolgee.socketio.TranslationEvent)
}
