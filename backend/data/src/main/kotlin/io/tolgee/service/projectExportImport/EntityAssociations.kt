package io.tolgee.service.projectExportImport

import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToOne

object EntityAssociations {
  /**
   * Whether the singular association [propertyName] on [entityClass] can be dropped on import (set null)
   * without violating a NOT-NULL constraint. Droppable only when every signal agrees: the Kotlin type is
   * nullable AND no mapping annotation marks it NOT-NULL (`@ManyToOne`/`@OneToOne(optional = false)`,
   * `@JoinColumn(nullable = false)`). Both are needed because neither alone is reliable — `KeyComment.author`
   * is a nullable type whose column is still NOT-NULL. Annotations are read off both field and getter for
   * `@get:`/`@field:` targets. An unresolvable property is treated as non-droppable.
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
