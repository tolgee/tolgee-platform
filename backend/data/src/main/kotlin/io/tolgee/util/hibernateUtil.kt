package io.tolgee.util

import io.tolgee.model.EntityWithId
import org.hibernate.proxy.HibernateProxy

/**
 * Extracts entity ID from a potentially lazy-loaded Hibernate proxy without triggering initialization.
 * This is useful when you need to get an entity's ID during Hibernate event interception
 * where lazy loading would cause session state errors.
 */
fun extractEntityId(entity: Any?): Long? {
  if (entity == null) return null

  // Get ID from HibernateProxy without triggering initialization
  if (entity is HibernateProxy) {
    return entity.hibernateLazyInitializer.identifier as? Long
  }

  // Already initialized entity
  if (entity is EntityWithId) {
    return entity.id
  }

  return null
}
