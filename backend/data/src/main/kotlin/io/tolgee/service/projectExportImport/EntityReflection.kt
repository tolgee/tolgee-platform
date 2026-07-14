package io.tolgee.service.projectExportImport

import io.tolgee.model.annotation.DoNotExport
import jakarta.persistence.ManyToMany
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.OneToOne
import java.lang.reflect.AnnotatedElement
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible
import kotlin.reflect.jvm.javaField
import kotlin.reflect.jvm.javaMethod

/** Reflective helpers for the export serializer, free of any JPA metamodel / Spring dependency. */
object EntityReflection {
  /**
   * Whether the association [propertyName] of [entityClass] is the OWNING side (`@ManyToOne`, or a
   * `mappedBy`-less `@OneToOne`/`@OneToMany`/`@ManyToMany`). Mapping annotations are read off both the
   * backing field and the getter, since either may carry a `@get:`/`@field:` use-site target.
   */
  fun isOwningAssociation(
    entityClass: Class<*>,
    propertyName: String,
  ): Boolean {
    val property = propertyOf(entityClass.kotlin, propertyName) ?: return false
    val members = annotationMembers(property)
    if (members.any { it.isAnnotationPresent(ManyToOne::class.java) }) return true
    members.forEach { member ->
      member.getAnnotation(OneToOne::class.java)?.let { return it.mappedBy.isEmpty() }
      member.getAnnotation(OneToMany::class.java)?.let { return it.mappedBy.isEmpty() }
      member.getAnnotation(ManyToMany::class.java)?.let { return it.mappedBy.isEmpty() }
    }
    return false
  }

  /** Whether the column [propertyName] of [entityClass] is annotated [DoNotExport] and skipped on export. */
  fun isDoNotExport(
    entityClass: Class<*>,
    propertyName: String,
  ): Boolean {
    val property = propertyOf(entityClass.kotlin, propertyName) ?: return false
    // An untargeted `@DoNotExport` binds to the Kotlin property (Kotlin reflection only); `@field:`/`@get:`
    // land on the Java field/getter. Check all three.
    if (property.annotations.any { it is DoNotExport }) return true
    return annotationMembers(property).any { it.isAnnotationPresent(DoNotExport::class.java) }
  }

  /**
   * Reads [propertyName] from [entity] through its Kotlin getter, initializing a lazy association.
   * Returns null when the property is absent.
   */
  fun readProperty(
    entity: Any,
    propertyName: String,
  ): Any? {
    val property = propertyOf(entity::class, propertyName) ?: return null

    @Suppress("UNCHECKED_CAST")
    val typed = property as KProperty1<Any, *>
    typed.isAccessible = true
    return typed.get(entity)
  }

  internal fun propertyOf(
    klass: KClass<*>,
    propertyName: String,
  ): KProperty1<out Any, *>? = klass.memberProperties.find { it.name == propertyName }

  /** The Java members a `@field:`/`@get:` annotation may live on: the backing field and the getter. */
  internal fun annotationMembers(property: KProperty1<out Any, *>): List<AnnotatedElement> =
    listOfNotNull(property.javaField, property.getter.javaMethod)
}
