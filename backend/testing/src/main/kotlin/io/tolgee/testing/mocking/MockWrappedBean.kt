package io.tolgee.testing.mocking

/**
 * Using [MockBean] is problematic, because it "dirties" (not really, but the result is the same) the context.
 * Dirty context means slower tests, because the context has to be rebuilt for each test.
 *
 *
 * So instead of defining mocks in individual tests, we'll replace services with delegating mocks globally,
 * and then we can easily configure expected behaviour and reset them between tests.
 *
 *
 * See [MockWrappedBeanResetTestExecutionListener]
 */
@Target(
  AnnotationTarget.FIELD,
  AnnotationTarget.FUNCTION,
  AnnotationTarget.PROPERTY_GETTER,
  AnnotationTarget.PROPERTY_SETTER,
  AnnotationTarget.VALUE_PARAMETER,
  AnnotationTarget.CLASS,
  AnnotationTarget.ANNOTATION_CLASS
)
@Retention(
  AnnotationRetention.RUNTIME
)
@MustBeDocumented
annotation class MockWrappedBean(
  val `for`: String = "",
)
