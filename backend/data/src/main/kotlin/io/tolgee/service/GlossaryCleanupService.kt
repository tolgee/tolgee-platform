package io.tolgee.service

import io.tolgee.model.Project

interface GlossaryCleanupService {
  fun unassignFromAllProjects(project: Project)
}
