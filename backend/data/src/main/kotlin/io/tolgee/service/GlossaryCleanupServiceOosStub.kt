package io.tolgee.service

import io.tolgee.model.Project
import org.springframework.stereotype.Service

@Service
class GlossaryCleanupServiceOosStub : GlossaryCleanupService {
  override fun unassignFromAllProjects(project: Project) {
    // no-op
  }
}
