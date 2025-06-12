package io.tolgee.activity.annotation

@Target(AnnotationTarget.CLASS)
annotation class ActivityEntityDescribingPaths(
  val paths: Array<String> = [],
)
