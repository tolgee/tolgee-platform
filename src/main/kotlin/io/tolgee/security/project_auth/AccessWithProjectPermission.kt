package io.tolgee.security.project_auth

import io.tolgee.model.Permission

@Target(AnnotationTarget.FUNCTION)
annotation class AccessWithProjectPermission(
  val permission: Permission.ProjectPermissionType
)
