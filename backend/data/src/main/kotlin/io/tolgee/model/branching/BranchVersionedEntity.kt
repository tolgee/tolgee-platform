package io.tolgee.model.branching

import io.tolgee.model.Project
import io.tolgee.model.key.Key

interface BranchVersionedEntity : EntityWithBranch {
  /**
   * Resolves the Key of the branch associated with the entity in the branching model.
   *
   * @return The Key entity
   */
  fun resolveKey(): Key?

  override fun resolveBranch(): Branch? {
    return resolveKey()?.branch?.let { return it }
  }

  override fun resolveProject(): Project? {
    return resolveKey()?.project
  }
}
