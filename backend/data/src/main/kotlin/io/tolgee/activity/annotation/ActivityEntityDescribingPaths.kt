package io.tolgee.activity.annotation

/** Each path is `relationName=a.b.c`, or just `a.b.c` when the relation is named after the path. */
@Target(AnnotationTarget.CLASS)
annotation class ActivityEntityDescribingPaths(
  val paths: Array<String> = [],
)
