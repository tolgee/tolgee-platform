package io.tolgee.service.notification

import io.tolgee.constants.Caches
import io.tolgee.dtos.request.notification.NotificationFilters
import io.tolgee.events.OnNotificationsChangedForUser
import io.tolgee.model.Notification
import io.tolgee.repository.NotificationRepository
import jakarta.transaction.Transactional
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.Cacheable
import org.springframework.cache.caffeine.CaffeineCache
import org.springframework.cache.transaction.TransactionAwareCacheDecorator
import org.springframework.context.ApplicationEventPublisher
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service

@Service
class NotificationService(
  private val notificationRepository: NotificationRepository,
  private val applicationEventPublisher: ApplicationEventPublisher,
  private val cacheManager: CacheManager,
) {
  @Cacheable(
    cacheNames = [Caches.NOTIFICATIONS],
    key = "{#userId, #pageable, #filters}",
  )
  fun getNotifications(
    userId: Long,
    pageable: Pageable,
    filters: NotificationFilters,
  ): Page<Notification> = notificationRepository.fetchNotificationsByUserId(userId, pageable, filters)

  fun getCountOfUnseenNotifications(userId: Long): Int =
    notificationRepository
      .fetchNotificationsByUserId(
        userId,
        PageRequest.of(0, 1),
        NotificationFilters(false),
      ).totalElements
      .toInt()

  @Transactional
  fun save(notification: Notification) {
    notificationRepository.save(notification)
    evictCache(notification.user.id)
    applicationEventPublisher.publishEvent(
      OnNotificationsChangedForUser(
        notification.user.id,
        notification,
      ),
    )
  }

  @Transactional
  fun markNotificationsAsSeen(
    notificationIds: List<Long>,
    userId: Long,
  ) {
    val modifiedCount = notificationRepository.markNotificationsAsSeen(notificationIds, userId)

    if (modifiedCount > 0) {
      evictCache(userId)
      applicationEventPublisher.publishEvent(
        OnNotificationsChangedForUser(
          userId,
        ),
      )
    }
  }

  private fun evictCache(userId: Long) {
    val cache = cacheManager.getCache(Caches.NOTIFICATIONS)
    if (cache == null || cache !is TransactionAwareCacheDecorator)
      return
    val targetCache = cache.targetCache
    if (targetCache !is CaffeineCache)
      return
    targetCache.nativeCache.asMap().keys.removeIf {
      (it as List<*>)[0] == userId
    }
  }
}
