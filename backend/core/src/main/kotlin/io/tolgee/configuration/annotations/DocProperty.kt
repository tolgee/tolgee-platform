package io.tolgee.configuration.annotations

@Target(AnnotationTarget.CLASS, AnnotationTarget.PROPERTY)
annotation class DocProperty(
  val name: String = "",
  val displayName: String = "",
  val description: String = "",
  val defaultValue: String = "",
  val defaultExplanation: String = "",
  val children: Array<DocProperty> = [],
  val prefix: String = "",
  val removedIn: String = "",
  val removalReason: String = "",
  val isList: Boolean = false,
  val hidden: Boolean = false,
)
