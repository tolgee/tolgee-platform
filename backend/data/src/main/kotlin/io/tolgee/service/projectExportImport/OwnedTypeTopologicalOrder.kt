package io.tolgee.service.projectExportImport

import jakarta.persistence.metamodel.EntityType

/**
 * Orders the OWNED entity types for the two-phase insert: [insertOrder] lists every type *after* all OWNED
 * types it points at through a singular owning FK, so an insert that resolves a parent reference always
 * finds the parent already persisted.
 *
 * The edge set is **all** singular owning associations to OWNED targets (required *and* nullable), not just
 * the required ones, so the order is also a safe child-before-parent basis (a nullable FK like `Key.branch`
 * still imposes an ordering); inserting over the superset trivially respects the required subset too
 * (nullable FKs are wired in the deserializer's second phase, after every row exists). Self-references
 * (`Branch.originBranch`) are not edges — a self-referential table is filled in one statement, and a
 * self-edge would be a cycle. (The wipe side does not consume this order — [ProjectContentClearer] hand-
 * writes its delete sequence because the FK hazards it faces, join tables and non-OWNED branch-merge rows,
 * aren't expressible by singular-FK topology.)
 */
object OwnedTypeTopologicalOrder {
  fun insertOrder(entityManagerEntities: Collection<EntityType<*>>): List<EntityType<*>> {
    val owned =
      entityManagerEntities
        .filter { ProjectExportImportPolicyRegistry.policyOf(it.javaType.name) == ExportImportPolicy.OWNED }
    val byName = owned.associateBy { it.javaType.name }
    val visited = LinkedHashSet<String>()
    val ordered = mutableListOf<EntityType<*>>()
    val onStack = HashSet<String>()

    fun visit(type: EntityType<*>) {
      val name = type.javaType.name
      if (!visited.add(name)) return
      onStack.add(name)
      dependencies(type).forEach { dependencyName ->
        require(dependencyName !in onStack) {
          "OWNED entity FK graph has a cycle through $name -> $dependencyName; the two-phase insert " +
            "assumes singular owning FKs between distinct OWNED types form a DAG (self-references aside)."
        }
        byName[dependencyName]?.let { visit(it) }
      }
      onStack.remove(name)
      ordered.add(type)
    }

    // Sort the roots by simple name so the order is deterministic across JVMs/metamodel iteration order.
    owned.sortedBy { it.javaType.simpleName }.forEach { visit(it) }
    return ordered
  }

  /** OWNED types referenced by [type] through a singular owning FK, excluding self-references. */
  private fun dependencies(type: EntityType<*>): Set<String> {
    val selfName = type.javaType.name
    return type.singularAttributes
      .asSequence()
      .filter { it.isAssociation }
      .filter { EntityReflection.isOwningAssociation(type.javaType, it.name) }
      .map { EntityMetamodelReader.associationTargetClassName(it) }
      .filter { it != selfName }
      .filter { ProjectExportImportPolicyRegistry.policyOf(it) == ExportImportPolicy.OWNED }
      .toSet()
  }
}
