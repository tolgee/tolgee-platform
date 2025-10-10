package io.tolgee.model.branching

/**
 * Interface for entities that are versioned across branches.
 *
 * Entities implementing this interface are part of the branching model and can exist in different versions
 * across different branches. This includes entities that are directly or indirectly related to [io.tolgee.model.key.Key] entities
 * that support branching.
 */
interface BranchVersionedEntity<T> {

  /**
   * Resolves the unique identifier of the branch associated with the entity in the branching model.
   *
   * @return The ID of the branch
   */
  fun resolveKeyId(): Long?

  /**
   * Determines if an entity has relevant changes in data in terms of branch versioning.
   *
   * @param oldState The original object data to compare.
   * @return True if the update actually modified data
   */
  fun isModified(oldState: Map<String, Any>): Boolean = true

  /**
   * Determines if the provided entity is different in terms of branch versioning.
   *
   * @param entity The original entity to compare.
   * @return True if the two objects are different, false otherwise.
   */
  fun differsInBranchVersion(entity: T): Boolean

  /**
   * Merges the properties from the specified source entity into the current entity.
   *
   * @param source The object whose values will be merged into the current object.
   */
  fun merge(source: T)
}
