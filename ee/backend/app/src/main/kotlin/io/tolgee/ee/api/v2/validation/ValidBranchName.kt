package io.tolgee.ee.api.v2.validation

import jakarta.validation.Constraint
import jakarta.validation.Payload
import org.hibernate.validator.constraints.Length
import kotlin.reflect.KClass

@MustBeDocumented
@Length(min = 2, max = 100, message = "max_length")
@Constraint(validatedBy = [BranchNameValidator::class])
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
