package io.tolgee.security.repository_auth

import io.tolgee.model.Permission

@Target(AnnotationTarget.FUNCTION)
annotation class AccessWithRepositoryPermission(
        val permission: Permission.ProjectPermissionType
)
