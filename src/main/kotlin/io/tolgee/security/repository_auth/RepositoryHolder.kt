package io.tolgee.security.repository_auth

import io.tolgee.model.Project
import org.springframework.stereotype.Component
import org.springframework.web.context.annotation.RequestScope

@Component
@RequestScope
open class RepositoryHolder {
    open lateinit var project: Project
    val isRepositoryInitialized
        get() = this::project.isInitialized
}
