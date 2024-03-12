package io.tolgee.activity.annotation

/**
 * When property is annotated with this annotation, change of this property won't trigger activity storing
 */
@Target(allowedTargets = [AnnotationTarget.PROPERTY])
annotation class ActivityIgnoredProp
