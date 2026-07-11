package io.tolgee.events

import org.springframework.core.ResolvableType

/**
 * Builds the [ResolvableType] reported to Spring's event multicaster so that
 * `@EventListener` parameters typed as e.g. `EntityPreCommitEvent<Key>` are only
 * dispatched for matching entity classes. When the entity is null the type
 * parameter cannot be resolved at runtime, so a wildcard type is returned and
 * the event is delivered only to listeners declared on the raw supertype.
 */
internal fun resolvableTypeFor(
  eventClass: Class<*>,
  entity: Any?,
): ResolvableType {
  val entityClass = entity?.javaClass ?: return ResolvableType.forClass(eventClass)
  return ResolvableType.forClassWithGenerics(eventClass, entityClass)
}
