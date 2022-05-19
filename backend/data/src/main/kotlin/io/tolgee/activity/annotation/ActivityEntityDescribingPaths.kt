package io.tolgee.activity.annotation

@Target(allowedTargets = [AnnotationTarget.CLASS])
annotation class ActivityEntityDescribingPaths(
  val paths: Array<String> = []
)
