package io.tolgee.service.projectExportImport

import com.fasterxml.jackson.databind.ObjectMapper
import io.tolgee.model.StandardAuditModel
import io.tolgee.service.projectExportImport.model.SerializedEntity
import jakarta.persistence.metamodel.EntityType
import jakarta.persistence.metamodel.PluralAttribute
import org.springframework.stereotype.Component
import java.lang.reflect.Field
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.jvm.isAccessible
import kotlin.reflect.jvm.javaField

/**
 * Writes a [SerializedEntity]'s values back onto a freshly built entity instance — the inverse of
 * [EntityMetamodelReader], driven by the same JPA metamodel so a newly added `@Column` round-trips with
 * no code change. It only mutates fields; deciding *which* row a handle resolves to (and the two-phase
 * ordering) is the deserializer's job.
 *
 * Construction goes through the JPA-mandated no-arg constructor (the `kotlin-jpa` plugin synthesises one
 * for every `@Entity`). That constructor does **not** run Kotlin field initializers, so collection fields
 * come back `null`; [newInstance] re-initializes them to empty collections to match a normally
 * constructed entity and avoid NPEs when Hibernate flushes inverse/owning collections.
 *
 * Values are written through the Kotlin **setter**, not the backing field directly: Tolgee's entities are
 * bytecode-enhanced with dirty tracking, so a raw field write on an already-managed entity (a Phase-B FK,
 * the project scalar mirror) would not be seen by the flush. The setter routes through the enhancement.
 */
@Component
class EntityMetamodelWriter(
  private val objectMapper: ObjectMapper,
) {
  fun newInstance(entityType: EntityType<*>): Any {
    val instance =
      entityType.javaType
        .getDeclaredConstructor()
        .also { it.isAccessible = true }
        .newInstance()
    initNullCollections(instance, entityType)
    if (instance is StandardAuditModel) instance.disableActivityLogging = true
    return instance
  }

  /**
   * Copies the persisted non-id, non-association columns from [record] onto [entity]. Each raw JSON value
   * is converted to the field's declared generic type via Jackson, so enums, dates, embeddables and JSONB
   * maps (`KeyMeta.custom`, `ProjectQaConfig.settings`) are reconstructed with their real key/value types
   * rather than left as raw strings/maps.
   */
  fun setBasicAttrs(
    entity: Any,
    record: SerializedEntity,
  ) {
    record.attrs.forEach { (name, raw) ->
      val javaType = objectMapper.typeFactory.constructType(fieldOf(entity.javaClass, name).genericType)
      writeValue(entity, name, objectMapper.convertValue(raw, javaType))
    }
  }

  fun setSingularAssociation(
    entity: Any,
    attributeName: String,
    target: Any?,
  ) {
    writeValue(entity, attributeName, target)
  }

  /**
   * Replaces the to-many owning collection [attributeName] with [targets], preserving the field's declared
   * collection kind (Set vs List) so membership/ordering semantics match the source.
   */
  fun setToManyAssociation(
    entity: Any,
    attributeName: String,
    targets: List<Any>,
  ) {
    val isSet = Set::class.java.isAssignableFrom(fieldOf(entity.javaClass, attributeName).type)
    writeValue(entity, attributeName, sameKindCollection(isSet, targets))
  }

  private fun sameKindCollection(
    isSet: Boolean,
    targets: List<Any>,
  ): MutableCollection<Any> {
    if (isSet) return LinkedHashSet(targets)
    return ArrayList(targets)
  }

  private fun initNullCollections(
    entity: Any,
    entityType: EntityType<*>,
  ) {
    entityType.pluralAttributes.forEach { attr ->
      val field = fieldOf(entity.javaClass, attr.name)
      field.isAccessible = true
      if (field.get(entity) != null) return@forEach
      writeValue(entity, attr.name, emptyCollectionFor(attr))
    }
  }

  private fun emptyCollectionFor(attr: PluralAttribute<*, *, *>): Any =
    when (attr.collectionType) {
      PluralAttribute.CollectionType.SET -> LinkedHashSet<Any>()
      PluralAttribute.CollectionType.LIST -> ArrayList<Any>()
      PluralAttribute.CollectionType.MAP -> LinkedHashMap<Any, Any>()
      PluralAttribute.CollectionType.COLLECTION -> ArrayList<Any>()
    }

  private fun writeValue(
    entity: Any,
    propertyName: String,
    value: Any?,
  ) {
    val property =
      requireNotNull(EntityReflection.propertyOf(entity::class, propertyName)) {
        "${entity.javaClass} has no property '$propertyName'"
      }
    require(property is KMutableProperty1<*, *>) { "${entity.javaClass}.$propertyName is not mutable" }
    property.isAccessible = true

    @Suppress("UNCHECKED_CAST")
    (property as KMutableProperty1<Any, Any?>).setter.call(entity, value)
  }

  private fun fieldOf(
    entityClass: Class<*>,
    name: String,
  ): Field {
    val field =
      EntityReflection.propertyOf(entityClass.kotlin, name)?.javaField
        ?: generateSequence<Class<*>>(entityClass) { it.superclass }
          .mapNotNull { runCatching { it.getDeclaredField(name) }.getOrNull() }
          .firstOrNull()
    return requireNotNull(field) { "$entityClass has no backing field for persistent attribute '$name'" }
  }
}
