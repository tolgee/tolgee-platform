package io.tolgee.model.branching

/**
 * Interface for entities that are versioned across branches.
 *
 * Entities implementing this interface are part of the branching model and can exist in different versions
 * across different branches. This includes entities that are directly or indirectly related to [io.tolgee.model.key.Key] entities
 * that support branching.
 */
interface BranchVersionedEntity {

  /**
   * Resolves the unique identifier of the branch associated with the entity in the branching model.
   *
   * @return The ID of the branch
   */
  fun resolveBranchId(): Long?

  /**
   * Determines if two given objects are different in terms of branch versioning.
   *
   * @param oldState The original object data to compare.
   * @return True if the two objects are different, false otherwise.
   */
  fun isDifferent(oldState: Map<String, Any>): Boolean = true
}
