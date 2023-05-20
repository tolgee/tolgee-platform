package io.tolgee.security.project_auth

import io.tolgee.model.enums.Scope

@Target(AnnotationTarget.FUNCTION)
annotation class AccessWithProjectPermission(
  val scope: Scope
)
