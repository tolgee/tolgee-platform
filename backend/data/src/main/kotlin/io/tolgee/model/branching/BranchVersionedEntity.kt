package io.tolgee.model.branching

import io.tolgee.model.key.Key

/**
 * Interface for entities that are versioned across branches.
 *
 * Entities implementing this interface are part of the branching model and can exist in different versions
 * across different branches. This includes entities that are directly or indirectly related to [io.tolgee.model.key.Key] entities
 * that support branching.
 */
interface BranchVersionedEntity<T, U> {
  /**
   * Resolves the Key of the branch associated with the entity in the branching model.
   *
   * @return The Key entity
   */
  fun resolveKey(): Key?

  /**
   * Determines whether the entity has been modified based on the provided state.
   *
   * This method evaluates the given state map to decide if changes have occurred
   * compared to the internal state of the entity.
   *
   * @param oldState A map representing the previous state of the entity.
   * @return True if the entity has been modified; false otherwise.
   */
  fun isModified(oldState: Map<String, Any>): Boolean = true

  /**
   * Determines if the provided entity is different compared to its snapshot version
   *
   * @param snapshot The original entity snapshot to compare.
   * @return True if the two objects are different, false otherwise.
   */
  fun hasChanged(snapshot: U): Boolean

  /**
   * Merges the properties from the specified source entity into the current entity.
   *
   * @param source The object whose values will be merged into the current object.
   */
  fun merge(source: T)
}
