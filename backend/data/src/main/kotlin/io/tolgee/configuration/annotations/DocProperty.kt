package io.tolgee.configuration.annotations

@Target(AnnotationTarget.CLASS, AnnotationTarget.PROPERTY)
annotation class DocProperty(
  val name: String = "",
  val description: String = "",
  val defaultValue: String = "",
  val defaultExplanation: String = "",
  val children: Array<DocProperty> = [],
  val removedIn: String = "",
  val hidden: Boolean = false
)
