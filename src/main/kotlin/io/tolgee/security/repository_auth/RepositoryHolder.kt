package io.tolgee.security.repository_auth

import io.tolgee.model.Repository
import org.springframework.stereotype.Component
import org.springframework.web.context.annotation.RequestScope

@Component
@RequestScope
open class RepositoryHolder {
    open lateinit var repository: Repository
    val isRepositoryInitialized
        get() = this::repository.isInitialized
}
