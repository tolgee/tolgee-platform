package io.tolgee.component.email.customTemplate.placeholder

@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class EmailPlaceholder(
  val position: Int,
  val placeholder: String,
  val description: String,
  val exampleValue: String,
)
