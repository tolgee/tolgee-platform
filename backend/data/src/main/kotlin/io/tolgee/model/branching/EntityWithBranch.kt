package io.tolgee.model.branching

import io.tolgee.model.Project

interface EntityWithBranch {
  /**
   * Resolves the branch associated with the entity.
   */
  fun resolveBranch(): Branch?

  /**
   * Resolves the project associated with the entity.
   */
  fun resolveProject(): Project?
}
