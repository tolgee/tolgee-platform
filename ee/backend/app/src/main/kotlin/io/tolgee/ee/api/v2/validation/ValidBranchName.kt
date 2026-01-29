package io.tolgee.ee.api.v2.validation

import jakarta.validation.Constraint
import jakarta.validation.Payload
import jakarta.validation.constraints.Pattern
import org.hibernate.validator.constraints.Length
import kotlin.reflect.KClass

@MustBeDocumented
@Pattern(regexp = "^[a-z0-9]([a-z0-9-_/]*[a-z0-9])?$", message = "invalid_pattern")
@Length(min = 2, max = 100, message = "max_length")
@Constraint(validatedBy = [])
@Target(
  AnnotationTarget.FIELD,
  AnnotationTarget.VALUE_PARAMETER,
  AnnotationTarget.PROPERTY_GETTER,
)
@Retention(AnnotationRetention.RUNTIME)
annotation class ValidBranchName(
  val message: String = "branch name is not valid",
  val groups: Array<KClass<*>> = [],
  val payload: Array<KClass<out Payload>> = [],
)
