package io.tolgee.component.cache

import io.tolgee.component.ProjectTranslationLastModifiedManager
import io.tolgee.constants.Caches
import org.springframework.stereotype.Component
import kotlin.reflect.KType
import kotlin.reflect.typeOf

@Component
class ApiDirectAccessCacheTypeProvider : DirectAccessCacheTypeProvider {
  override fun getDirectAccessCacheTypes(): Map<String, KType> =
    mapOf(
      Caches.PROJECT_TRANSLATIONS_MODIFIED to typeOf<ProjectTranslationLastModifiedManager.LastModifiedInfo>(),
    )
}
