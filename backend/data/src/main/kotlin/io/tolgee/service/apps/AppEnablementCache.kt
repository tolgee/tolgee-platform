package io.tolgee.service.apps

import io.tolgee.constants.Caches
import io.tolgee.repository.apps.AppEnabledForProjectRepository
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AppEnablementCache(
  private val appEnabledForProjectRepository: AppEnabledForProjectRepository,
) {
  @Cacheable(cacheNames = [Caches.APP_ENABLEMENTS], key = "#appInstallId")
  @Transactional(readOnly = true)
  fun getEnabledProjectIds(appInstallId: Long): Set<Long> =
    appEnabledForProjectRepository.findEnabledProjectIdsByInstallId(appInstallId).toSet()

  @CacheEvict(cacheNames = [Caches.APP_ENABLEMENTS], key = "#appInstallId")
  fun evict(appInstallId: Long) {}

  @CacheEvict(cacheNames = [Caches.APP_ENABLEMENTS], allEntries = true)
  fun evictAll() {}
}
