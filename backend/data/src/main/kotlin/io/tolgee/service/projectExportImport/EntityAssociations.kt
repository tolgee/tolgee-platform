package io.tolgee.service.projectExportImport

import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToOne

object EntityAssociations {
  /**
   * Whether the singular association named [propertyName] on [entityClass] can be dropped on import
   * (set null) without violating a NOT-NULL constraint. Droppable only when EVERY available signal
   * agrees the column is nullable: the Kotlin property type is nullable (`Foo?`) AND no mapping
   * annotation marks it NOT-NULL (`@ManyToOne`/`@OneToOne(optional = false)` or
   * `@JoinColumn(nullable = false)`). The signals are combined because neither alone is reliable
   * here — `Key.project` is `@ManyToOne(optional = false)` on a non-null type, while
   * `KeyComment.author` is `@ManyToOne(optional = false)` on a *nullable* type whose column is still
   * NOT-NULL. Annotations are read off both the backing field and the getter, since either may carry
   * a use-site-targeted (`@get:`/`@field:`) mapping annotation.
   *
   * Residual gap: a nullable-typed column made NOT-NULL only in the Liquibase schema with no mapping
   * annotation would be mis-reported droppable; that type/schema mismatch is unusual and would
   * surface as an insert failure at import time. An unresolvable property is treated as non-droppable.
   */
  fun isDroppableSingularAssociation(
    entityClass: Class<*>,
    propertyName: String,
  ): Boolean {
    val property = EntityReflection.propertyOf(entityClass.kotlin, propertyName) ?: return false
    if (!property.returnType.isMarkedNullable) return false
    val members = EntityReflection.annotationMembers(property)
    if (members.isEmpty()) return false
    if (members.any { it.getAnnotation(ManyToOne::class.java)?.optional == false }) return false
    if (members.any { it.getAnnotation(OneToOne::class.java)?.optional == false }) return false
    if (members.any { it.getAnnotation(JoinColumn::class.java)?.nullable == false }) return false
    return true
  }
}
