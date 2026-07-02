package io.tolgee.ee.projectExportImport

import io.tolgee.AbstractSpringTest
import io.tolgee.model.branching.snapshot.KeyMetaSnapshot
import io.tolgee.model.branching.snapshot.KeySnapshot
import io.tolgee.model.branching.snapshot.TranslationSnapshot
import io.tolgee.service.projectExportImport.EntityAssociations
import io.tolgee.service.projectExportImport.EntityMetamodelReader
import io.tolgee.service.projectExportImport.ExportImportPolicy
import io.tolgee.service.projectExportImport.ProjectContentClearer
import io.tolgee.service.projectExportImport.ProjectExportImportPolicyRegistry
import io.tolgee.service.projectExportImport.ProjectScopedCollectorQueries
import io.tolgee.service.projectExportImport.sidechannel.SideChannelHandlerRegistry
import jakarta.persistence.metamodel.Attribute
import jakarta.persistence.metamodel.SingularAttribute
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

/**
 * Build-gate guards for project export/import. Adding a new `@Entity` without classifying it in
 * [ProjectExportImportPolicyRegistry] — or adding an association from an OWNED entity to an
 * unclassified type — fails the build here.
 *
 * Lives in `:ee-test` so the JPA metamodel sees every module's entities (`:data`/`:server-app`/
 * `:ee-app`/`:api`); the `entity scan is non-trivial` test guards against a too-small metamodel.
 */
@SpringBootTest
class ProjectExportImportPolicyGuardTest : AbstractSpringTest() {
  @Autowired
  private lateinit var sideChannelHandlerRegistry: SideChannelHandlerRegistry

  private val managedEntityClassNames: Set<String>
    get() =
      entityManager.metamodel.entities
        .map { it.javaType.name }
        .toSet()

  @Test
  fun `every managed entity is classified`() {
    val unclassified = ProjectExportImportPolicyRegistry.unclassified(managedEntityClassNames)
    assertThat(unclassified)
      .withFailMessage(
        "These @Entity classes are not classified for project export/import. Add each to " +
          "ProjectExportImportPolicyRegistry (OWNED / USER_REF / PROJECT_ROOT / SIDE_CHANNEL / IGNORED):\n" +
          unclassified.sorted().joinToString("\n") { "  - $it" },
      ).isEmpty()
  }

  @Test
  fun `registry has no stale entries`() {
    val stale = ProjectExportImportPolicyRegistry.staleEntries(managedEntityClassNames)
    assertThat(stale)
      .withFailMessage(
        "These classified names no longer correspond to a managed @Entity (renamed/removed?). " +
          "Remove them from ProjectExportImportPolicyRegistry:\n" +
          stale.sorted().joinToString("\n") { "  - $it" },
      ).isEmpty()
  }

  @Test
  fun `entity scan is non-trivial (a broken or empty metamodel cannot masquerade as success)`() {
    assertThat(managedEntityClassNames.size).isGreaterThan(50)
    assertThat(ProjectExportImportPolicyRegistry.ownedClassNames.size).isGreaterThan(10)
  }

  @Test
  fun `every association of an OWNED entity targets a classified type`() {
    val violations = mutableListOf<String>()
    var nonDroppableEvaluated = 0
    ownedEntityTypes()
      .flatMap { entityType -> entityType.attributes.filter { it.isAssociation }.map { entityType to it } }
      .forEach { (entityType, attr) ->
        val droppable = isDroppable(entityType.javaType, attr)
        if (!droppable) nonDroppableEvaluated++
        val violation =
          ProjectExportImportPolicyRegistry.associationViolation(
            EntityMetamodelReader.associationTargetClassName(attr),
            droppable,
          )
        if (violation != null) violations += "${entityType.javaType.simpleName}.${attr.name} -> $violation"
      }
    assertThat(violations)
      .withFailMessage(
        "OWNED entities have associations the export/import cannot handle. Classify the target type " +
          "(or, for a reference to an IGNORED type, make the FK nullable so it can be dropped on import):\n" +
          violations.sorted().joinToString("\n") { "  - $it" },
      ).isEmpty()
    assertThat(nonDroppableEvaluated)
      .withFailMessage("No OWNED non-nullable singular FK was detected — droppability resolved nothing")
      .isGreaterThan(0)
  }

  @Test
  fun `every OWNED type has a project-scoped collector query`() {
    val ownedClassNames = ProjectExportImportPolicyRegistry.ownedClassNames
    val missing = ownedClassNames - ProjectScopedCollectorQueries.queriesByClassName.keys
    assertThat(missing)
      .withFailMessage(
        "These OWNED entities have no project-scoped collector query. Row discovery is per-type (not a " +
          "graph walk), so each OWNED type must register one in ProjectScopedCollectorQueries:\n" +
          missing.sorted().joinToString("\n") { "  - $it" },
      ).isEmpty()
  }

  @Test
  fun `every OWNED type has a clear strategy`() {
    val ownedClassNames = ProjectExportImportPolicyRegistry.ownedClassNames
    val missing = ownedClassNames - ProjectContentClearer.clearedOwnedClassNames
    assertThat(missing)
      .withFailMessage(
        "These OWNED entities are not cleared by ProjectContentClearer. Clear-in-place must wipe every " +
          "OWNED type before a mirror import (a missed type silently degrades the mirror into a merge); " +
          "wire its project-scoped deletion and add it to ProjectContentClearer.CLEARED_OWNED_TYPES:\n" +
          missing.sorted().joinToString("\n") { "  - $it" },
      ).isEmpty()
  }

  @Test
  fun `clear strategies target only OWNED types`() {
    val extra = ProjectContentClearer.clearedOwnedClassNames - ProjectExportImportPolicyRegistry.ownedClassNames
    assertThat(extra)
      .withFailMessage(
        "ProjectContentClearer.CLEARED_OWNED_TYPES lists non-OWNED (or removed) types. Remove them:\n" +
          extra.sorted().joinToString("\n") { "  - $it" },
      ).isEmpty()
  }

  @Test
  fun `collector queries target only OWNED types`() {
    val ownedClassNames = ProjectExportImportPolicyRegistry.ownedClassNames
    val extra = ProjectScopedCollectorQueries.queriesByClassName.keys - ownedClassNames
    assertThat(extra)
      .withFailMessage(
        "These collector queries are registered for non-OWNED (or removed) types. Remove them from " +
          "ProjectScopedCollectorQueries:\n" + extra.sorted().joinToString("\n") { "  - $it" },
      ).isEmpty()
  }

  @Test
  fun `every SIDE_CHANNEL type has a handler (and every handler a SIDE_CHANNEL type)`() {
    val sideChannel = ProjectExportImportPolicyRegistry.sideChannelClassNames
    val handled = sideChannelHandlerRegistry.handledEntityClassNames

    assertThat(sideChannel - handled)
      .withFailMessage(
        "These SIDE_CHANNEL types have no SideChannelHandler, so they would export/restore nothing. " +
          "Add a handler (writer + reader) for each:\n" +
          (sideChannel - handled).sorted().joinToString("\n") { "  - $it" },
      ).isEmpty()
    assertThat(handled - sideChannel)
      .withFailMessage(
        "These SideChannelHandlers target a type not classified SIDE_CHANNEL. Classify it SIDE_CHANNEL " +
          "or remove the handler:\n" +
          (handled - sideChannel).sorted().joinToString("\n") { "  - $it" },
      ).isEmpty()
    assertThat(sideChannel)
      .withFailMessage("Expected at least one SIDE_CHANNEL type (KeysDistance/BigMeta)")
      .isNotEmpty()
  }

  @Test
  fun `OWNED type simple names are distinct (the export zip keys entity files by simple name)`() {
    val duplicates =
      ownedEntityTypes()
        .groupBy { it.javaType.simpleName }
        .filterValues { it.size > 1 }
    assertThat(duplicates)
      .withFailMessage(
        "These OWNED types share a simple name. The export writes one entities/<SimpleName>.json per " +
          "type, so a collision would silently overwrite one type's rows. Disambiguate the zip layout " +
          "before adding such a type:\n" +
          duplicates.entries.joinToString("\n") { (name, types) ->
            "  - $name: ${types.joinToString { t -> t.javaType.name }}"
          },
      ).isEmpty()
  }

  @Test
  fun `snapshot columns are pinned (a new snapshot column may be a cross-entity id needing a remap)`() {
    // The snapshot entities carry foreign-keys-in-disguise — plain Long columns (originalKeyId/
    // branchKeyId) and jsonb-embedded screenshotIds — that the metamodel can't see as associations, so
    // the categorical association guards above don't cover them. They are remapped by hand in
    // EntityGraphDeserializer.remapSnapshotReferences. Pin the basic-column set of each snapshot type so
    // adding a column fails the build and forces a decision: is it another cross-entity id to remap?
    val pinned =
      mapOf(
        KeySnapshot::class.java to
          setOf(
            "name",
            "namespace",
            "isPlural",
            "pluralArgName",
            "maxCharLimit",
            "originalKeyId",
            "branchKeyId",
            "screenshotReferences",
            "createdAt",
            "updatedAt",
          ),
        TranslationSnapshot::class.java to setOf("language", "value", "state", "labels", "createdAt", "updatedAt"),
        KeyMetaSnapshot::class.java to setOf("description", "custom", "tags", "createdAt", "updatedAt"),
      )
    pinned.forEach { (clazz, expected) ->
      val actual =
        entityManager.metamodel
          .entity(clazz)
          .attributes
          .filterNot { it.isAssociation }
          .filterNot { it is SingularAttribute<*, *> && it.isId }
          .map { it.name }
          .toSet()
      assertThat(actual)
        .withFailMessage(
          "Basic columns of ${clazz.simpleName} changed (expected %s, was %s). If you added another " +
            "cross-entity id, wire it into EntityGraphDeserializer.remapSnapshotReferences, then update " +
            "this pinned set.",
          expected,
          actual,
        ).isEqualTo(expected)
    }
  }

  private fun ownedEntityTypes() =
    entityManager.metamodel.entities
      .filter { ProjectExportImportPolicyRegistry.policyOf(it.javaType.name) == ExportImportPolicy.OWNED }

  private fun isDroppable(
    entityClass: Class<*>,
    attr: Attribute<*, *>,
  ): Boolean {
    if (attr !is SingularAttribute<*, *>) return true
    return EntityAssociations.isDroppableSingularAssociation(entityClass, attr.name)
  }
}
