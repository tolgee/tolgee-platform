package io.tolgee.service.projectExportImport

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import io.tolgee.model.Project
import io.tolgee.model.Screenshot
import io.tolgee.model.UserAccount
import io.tolgee.model.branching.Branch
import io.tolgee.model.key.Key
import io.tolgee.model.task.Task
import io.tolgee.service.projectExportImport.model.SerializedEntity
import jakarta.persistence.EntityManager
import jakarta.persistence.metamodel.Attribute
import jakarta.persistence.metamodel.EntityType
import jakarta.persistence.metamodel.PluralAttribute
import jakarta.persistence.metamodel.SingularAttribute
import org.springframework.stereotype.Component

/**
 * Inserts a project's exported OWNED graph onto a freshly cleared target project, mirroring
 * [EntityMetamodelReader] in reverse. Two phases in one transaction (the caller's), with auditing/activity
 * suppressed:
 *
 * - **Phase A** walks the OWNED types in [OwnedTypeTopologicalOrder.insertOrder] and, for each row, builds
 *   a fresh entity, copies its columns, wires every singular FK whose target is another type (its parent
 *   row is already persisted, including the `Key.branch`/default-branch normalization), and persists it —
 *   so every row has a real PK before anything references it.
 * - **Phase B** wires the only thing phase A can't: self-referential FKs (`Branch.originBranch`, which may
 *   point at a later branch) and the to-many owning links (labels, tags, assignees); it also resets
 *   `Branch.pending` per the mirror's merge-state discard.
 *
 * Every lookup is keyed by the **source PK handle** (a scalar/string), never by an entity instance: an
 * unflushed entity has `id == 0` and a colliding hash, so unsaved entities must never enter a hash map.
 */
@Component
class EntityGraphDeserializer(
  private val entityManager: EntityManager,
  private val writer: EntityMetamodelWriter,
  private val objectMapper: ObjectMapper,
) {
  fun deserialize(
    entityJsonByType: Map<String, ByteArray>,
    targetProject: Project,
    importingAdmin: UserAccount,
    userResolver: (String) -> UserAccount?,
    projectRecord: SerializedEntity?,
  ): Result {
    val ownedTypes = OwnedTypeTopologicalOrder.insertOrder(entityManager.metamodel.entities)
    val recordsByType = ownedTypes.associateWith { parseRecords(entityJsonByType[it.javaType.simpleName]) }
    val context = Context(targetProject, importingAdmin, userResolver)

    ownedTypes.forEach { type -> recordsByType.getValue(type).forEach { phaseAInsert(type, it, context) } }
    ownedTypes.forEach { type -> recordsByType.getValue(type).forEach { phaseBWire(type, it, context) } }
    projectRecord?.let { wireProjectRootContentPointers(it, context) }

    return Result(context.screenshotsBySourceId, context.maxTaskNumber)
  }

  private fun phaseAInsert(
    type: EntityType<*>,
    record: SerializedEntity,
    context: Context,
  ) {
    val entity = writer.newInstance(type)
    writer.setBasicAttrs(entity, record)
    setIdAssociations(entity, type, record, context)
    phaseASingularAssociations(type).forEach { attr ->
      writer.setSingularAssociation(entity, attr.name, resolvePhaseASingular(type, attr, record, context))
    }
    entityManager.persist(entity)
    context.register(type, record.handle, entity)
    captureForPostProcessing(entity, type, record, context)
  }

  private fun phaseBWire(
    type: EntityType<*>,
    record: SerializedEntity,
    context: Context,
  ) {
    val entity = context.entity(type.javaType.name, record.handle) ?: return
    selfReferenceAssociations(type).forEach { attr ->
      writer.setSingularAssociation(entity, attr.name, resolveReference(attr, record.assocs[attr.name], context))
    }
    toManyOwningAssociations(type).forEach { attr ->
      val resolved = (record.assocs[attr.name] as? List<*>).orEmpty().mapNotNull { resolveReference(attr, it, context) }
      writer.setToManyAssociation(entity, attr.name, resolved)
    }
    if (entity is Branch) entity.pending = false
  }

  /**
   * Re-points the kept project row at its imported content. `baseLanguage` and `defaultNamespace` are the
   * project's only associations to OWNED rows; they are wired from the source handles once languages and
   * namespaces exist. Every other project association (org owner, glossaries) stays the target's.
   */
  private fun wireProjectRootContentPointers(
    projectRecord: SerializedEntity,
    context: Context,
  ) {
    val projectType = entityManager.metamodel.entity(Project::class.java)
    PROJECT_CONTENT_POINTERS.forEach { name ->
      val attr = projectType.singularAttributes.firstOrNull { it.name == name } ?: return@forEach
      writer.setSingularAssociation(
        context.targetProject,
        name,
        resolveReference(attr, projectRecord.assocs[name], context),
      )
    }
  }

  private fun resolvePhaseASingular(
    type: EntityType<*>,
    attr: SingularAttribute<*, *>,
    record: SerializedEntity,
    context: Context,
  ): Any? {
    // Key.branch carries the source's branch handle, NULL meaning "legacy default". Both NULL and an
    // explicit source-default handle collapse to the imported default branch so the two encodings
    // can't diverge; any other handle remaps to its imported branch. Resolvable in phase A because every
    // branch is inserted before any key (a key depends on its branch).
    if (type.javaType == Key::class.java && attr.name == "branch") {
      return resolveKeyBranch(record.assocs[attr.name], context)
    }
    return resolveReference(attr, record.assocs[attr.name], context)
  }

  /** Composite-id associations (`KeyScreenshotReference.key`/`.screenshot`) live only in the handle map. */
  private fun setIdAssociations(
    entity: Any,
    type: EntityType<*>,
    record: SerializedEntity,
    context: Context,
  ) {
    val idAssociations = type.singularAttributes.filter { it.isId && it.isAssociation }
    if (idAssociations.isEmpty()) return
    val handle = record.handle as? Map<*, *> ?: return
    idAssociations.forEach { attr ->
      writer.setSingularAssociation(entity, attr.name, resolveReference(attr, handle[attr.name], context))
    }
  }

  private fun resolveKeyBranch(
    rawHandle: Any?,
    context: Context,
  ): Branch? {
    val default = context.importedDefaultBranch
    if (rawHandle == null) return default
    if (default != null && normalizeHandle(rawHandle) == context.sourceDefaultBranchHandle) return default
    return context.entity(Branch::class.java.name, rawHandle) as? Branch
  }

  private fun resolveReference(
    attr: Attribute<*, *>,
    rawValue: Any?,
    context: Context,
  ): Any? {
    rawValue ?: return null
    val targetClassName = EntityMetamodelReader.associationTargetClassName(attr)
    return when (ProjectExportImportPolicyRegistry.policyOf(targetClassName)) {
      ExportImportPolicy.USER_REF -> resolveUser(rawValue, context)
      ExportImportPolicy.PROJECT_ROOT -> context.targetProject
      ExportImportPolicy.OWNED -> context.entity(targetClassName, rawValue)
      else -> null
    }
  }

  private fun resolveUser(
    rawValue: Any,
    context: Context,
  ): UserAccount {
    val username = (rawValue as Map<*, *>)["username"] as String
    return context.userResolver(username) ?: context.importingAdmin
  }

  private fun captureForPostProcessing(
    entity: Any,
    type: EntityType<*>,
    record: SerializedEntity,
    context: Context,
  ) {
    if (entity is Screenshot) {
      context.screenshotsBySourceId[(record.handle as Number).toLong()] = entity
    }
    if (entity is Branch && record.attrs["isDefault"] == true) {
      context.importedDefaultBranch = entity
      context.sourceDefaultBranchHandle = normalizeHandle(record.handle)
    }
    if (entity is Task) {
      val number = (record.attrs["number"] as? Number)?.toLong() ?: 0L
      context.maxTaskNumber = maxOf(context.maxTaskNumber, number)
    }
  }

  private fun parseRecords(json: ByteArray?): List<SerializedEntity> {
    json ?: return emptyList()
    return objectMapper.readValue(json, object : TypeReference<List<SerializedEntity>>() {})
  }

  /**
   * Owning singular FKs set in phase A: everything except self-references. Their target type precedes this
   * type in the insert order (a row's parents are persisted before it), so each handle resolves to an
   * already-persisted entity. Self-references can't (a branch may point at a later branch) and wait for B.
   */
  private fun phaseASingularAssociations(type: EntityType<*>): List<SingularAttribute<*, *>> =
    owningSingularAssociations(type).filter {
      !it.isId && EntityMetamodelReader.associationTargetClassName(it) != type.javaType.name
    }

  private fun selfReferenceAssociations(type: EntityType<*>): List<SingularAttribute<*, *>> =
    owningSingularAssociations(type).filter {
      !it.isId && EntityMetamodelReader.associationTargetClassName(it) == type.javaType.name
    }

  private fun owningSingularAssociations(type: EntityType<*>): List<SingularAttribute<*, *>> =
    type.singularAttributes
      .filter { it.isAssociation }
      .filter { EntityReflection.isOwningAssociation(type.javaType, it.name) }
      .filter {
        ProjectExportImportPolicyRegistry.policyOf(EntityMetamodelReader.associationTargetClassName(it)) !=
          ExportImportPolicy.IGNORED
      }

  private fun toManyOwningAssociations(type: EntityType<*>): List<PluralAttribute<*, *, *>> =
    type.pluralAttributes
      .filter { it.isAssociation }
      .filter { EntityReflection.isOwningAssociation(type.javaType, it.name) }
      .filter {
        ProjectExportImportPolicyRegistry.policyOf(EntityMetamodelReader.associationTargetClassName(it)) !=
          ExportImportPolicy.IGNORED
      }

  private fun normalizeHandle(handle: Any): Any {
    if (handle is Number) return handle.toLong()
    return handle
  }

  private inner class Context(
    val targetProject: Project,
    val importingAdmin: UserAccount,
    val userResolver: (String) -> UserAccount?,
  ) {
    private val byTypeAndHandle = HashMap<String, MutableMap<Any, Any>>()
    val screenshotsBySourceId = LinkedHashMap<Long, Screenshot>()
    var importedDefaultBranch: Branch? = null
    var sourceDefaultBranchHandle: Any? = null
    var maxTaskNumber: Long = 0

    fun register(
      type: EntityType<*>,
      handle: Any,
      entity: Any,
    ) {
      byTypeAndHandle.getOrPut(type.javaType.name) { HashMap() }[normalizeHandle(handle)] = entity
    }

    fun entity(
      className: String,
      handle: Any?,
    ): Any? {
      handle ?: return null
      return byTypeAndHandle[className]?.get(normalizeHandle(handle))
    }
  }

  data class Result(
    val screenshotsBySourceId: Map<Long, Screenshot>,
    val maxImportedTaskNumber: Long,
  )

  companion object {
    private val PROJECT_CONTENT_POINTERS = listOf("baseLanguage", "defaultNamespace")
  }
}
