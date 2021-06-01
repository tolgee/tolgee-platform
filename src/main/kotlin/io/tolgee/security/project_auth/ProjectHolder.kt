package io.tolgee.security.project_auth

import io.tolgee.model.Project
import org.springframework.stereotype.Component
import org.springframework.web.context.annotation.RequestScope

@Component
@RequestScope
class ProjectHolder {
    lateinit var project: Project
    val isRepositoryInitialized
        get() = this::project.isInitialized
}
