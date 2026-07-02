package io.tolgee.service.projectExportImport

import io.tolgee.model.EntityWithId
import io.tolgee.model.UserAccount
import io.tolgee.service.projectExportImport.model.SerializedEntity
import io.tolgee.service.projectExportImport.model.UserRef
import jakarta.persistence.metamodel.Attribute
import jakarta.persistence.metamodel.EntityType
import jakarta.persistence.metamodel.PluralAttribute
import jakarta.persistence.metamodel.SingularAttribute
import org.hibernate.Hibernate

/**
 * Turns one loaded entity row into a [SerializedEntity] using the JPA metamodel for structure and
 * [EntityReflection] for value reads.
 */
object EntityMetamodelReader {
  fun read(
    entity: Any,
    entityType: EntityType<*>,
  ): SerializedEntity {
    failOnElementCollection(entityType)
    val idAttributeNames =
      entityType.singularAttributes
        .filter { it.isId }
        .map { it.name }
        .toSet()
    return SerializedEntity(
      handle = handleOf(entity, entityType),
      attrs = readAttrs(entity, entityType, idAttributeNames),
      assocs = readAssocs(entity, entityType, idAttributeNames),
    )
  }

  private fun failOnElementCollection(entityType: EntityType<*>) {
    val elementCollections =
      entityType.pluralAttributes.filter {
        it.persistentAttributeType == Attribute.PersistentAttributeType.ELEMENT_COLLECTION
      }
    check(elementCollections.isEmpty()) {
      "OWNED entity ${entityType.javaType.simpleName} has @ElementCollection attribute(s) " +
        "${elementCollections.map { it.name }} that the export serializer does not handle. Extend the " +
        "serializer (and the parity-diff oracle) to cover them, or reclassify the entity."
    }
  }

  /**
   * The export handle of [entity]: a scalar for a simple id, or a map of component name to component
   * handle for a composite `@IdClass` id (e.g. `KeyScreenshotReference` → `{key, screenshot}`), where
   * an association component resolves to the referenced row's source id.
   */
  fun handleOf(
    entity: Any,
    entityType: EntityType<*>,
  ): Any {
    val idAttributes = entityType.singularAttributes.filter { it.isId }
    val single = idAttributes.singleOrNull()
    if (single != null && !single.isAssociation) {
      return requireNotNull(EntityReflection.readProperty(entity, single.name)) {
        "${entityType.javaType.simpleName} has a null id"
      }
    }
    return idAttributes.associate { attr ->
      attr.name to idComponentValue(attr.isAssociation, EntityReflection.readProperty(entity, attr.name))
    }
  }

  private fun idComponentValue(
    isAssociation: Boolean,
    value: Any?,
  ): Any? {
    if (isAssociation) return ownedHandle(value)
    return value
  }

  private fun readAttrs(
    entity: Any,
    entityType: EntityType<*>,
    idAttributeNames: Set<String>,
  ): Map<String, Any?> {
    val entityClass = entityType.javaType
    return entityType.singularAttributes
      .filter { it.persistentAttributeType in BASIC_KINDS }
      .filter { it.name !in idAttributeNames }
      .filterNot { EntityReflection.isDoNotExport(entityClass, it.name) }
      .associate { it.name to EntityReflection.readProperty(entity, it.name) }
  }

  private fun readAssocs(
    entity: Any,
    entityType: EntityType<*>,
    idAttributeNames: Set<String>,
  ): Map<String, Any?> {
    val entityClass = entityType.javaType
    val result = LinkedHashMap<String, Any?>()
    entityType.attributes
      .filter { it.isAssociation }
      .filter { it.name !in idAttributeNames }
      .filter { EntityReflection.isOwningAssociation(entityClass, it.name) }
      .forEach { attr ->
        val policy = targetPolicy(attr)
        if (policy?.isNotGraphCarried == true) return@forEach
        val value = EntityReflection.readProperty(entity, attr.name)
        if (value == null) {
          result[attr.name] = null
          return@forEach
        }
        result[attr.name] = serializeOwningAssociation(entityClass, attr, policy, value)
      }
    return result
  }

  /**
   * Serializes one owning association whose live value is [value]. A to-one whose target is dropped as
   * soft-deleted (so [assocValue] returns null) is only valid when the FK is nullable — otherwise the
   * row should have been excluded by its collector's soft-delete hop-filter, so we fail loudly rather
   * than emit an unsatisfiable null for a NOT-NULL column.
   */
  private fun serializeOwningAssociation(
    entityClass: Class<*>,
    attr: Attribute<*, *>,
    policy: ExportImportPolicy?,
    value: Any,
  ): Any? {
    val serialized = assocValue(attr, policy, value)
    if (serialized == null && attr !is PluralAttribute<*, *, *>) {
      require(EntityAssociations.isDroppableSingularAssociation(entityClass, attr.name)) {
        "${entityClass.simpleName}.${attr.name} resolves to a soft-deleted " +
          "${associationTargetClassName(attr)} but the FK is non-nullable; its collector " +
          "in ProjectScopedCollectorQueries must exclude rows under soft-deleted parents."
      }
    }
    return serialized
  }

  private fun assocValue(
    attr: Attribute<*, *>,
    policy: ExportImportPolicy?,
    value: Any,
  ): Any? {
    if (attr is PluralAttribute<*, *, *>) return toManyValue(policy, value)
    return referenceValue(policy, value)
  }

  private fun toManyValue(
    policy: ExportImportPolicy?,
    collection: Any,
  ): List<Any?> =
    (collection as Iterable<*>).mapNotNull { element ->
      element ?: return@mapNotNull null
      referenceValue(policy, element)
    }

  /**
   * A single association reference. A soft-deleted OWNED target is dropped (returns null): its
   * collector excluded it from the export, so a handle to it would dangle on import.
   */
  private fun referenceValue(
    policy: ExportImportPolicy?,
    target: Any,
  ): Any? {
    if (policy == ExportImportPolicy.USER_REF) {
      return UserRef(requireNotNull(EntityReflection.readProperty(target, UserAccount::username.name)) as String)
    }
    val unproxied = Hibernate.unproxy(target)
    // Keyed on the deletedAt column, not the SoftDeletable interface: Branch carries deletedAt without
    // implementing it, and Key.branch / Task.branch could otherwise emit a handle to a wiped branch.
    if (EntityReflection.readProperty(unproxied, "deletedAt") != null) return null
    return ownedHandle(unproxied)
  }

  private fun ownedHandle(target: Any?): Any =
    requireNotNull((Hibernate.unproxy(target) as? EntityWithId)?.id) {
      "association target ${target?.let { Hibernate.getClass(it).simpleName }} has no resolvable id"
    }

  private fun targetPolicy(attr: Attribute<*, *>): ExportImportPolicy? =
    ProjectExportImportPolicyRegistry.policyOf(associationTargetClassName(attr))

  fun associationTargetClassName(attr: Attribute<*, *>): String =
    when (attr) {
      is PluralAttribute<*, *, *> -> attr.elementType.javaType.name
      is SingularAttribute<*, *> -> attr.type.javaType.name
      else -> attr.javaType.name
    }

  private val BASIC_KINDS =
    setOf(
      Attribute.PersistentAttributeType.BASIC,
      Attribute.PersistentAttributeType.EMBEDDED,
    )
}
