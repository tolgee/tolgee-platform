package io.tolgee.component.cache

import io.tolgee.component.ProjectTranslationLastModifiedManager
import io.tolgee.constants.Caches
import org.springframework.stereotype.Component
import kotlin.reflect.KClass

@Component
class ApiDirectAccessCacheTypeProvider : DirectAccessCacheTypeProvider {
  override fun getDirectAccessCacheTypes(): Map<String, KClass<*>> =
    mapOf(
      Caches.PROJECT_TRANSLATIONS_MODIFIED to ProjectTranslationLastModifiedManager.LastModifiedInfo::class,
    )
}
