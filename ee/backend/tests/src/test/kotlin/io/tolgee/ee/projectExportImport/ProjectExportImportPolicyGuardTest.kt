package io.tolgee.ee.projectExportImport

import io.tolgee.AbstractSpringTest
import io.tolgee.service.projectExportImport.EntityAssociations
import io.tolgee.service.projectExportImport.ExportImportPolicy
import io.tolgee.service.projectExportImport.ProjectExportImportPolicyRegistry
import jakarta.persistence.metamodel.Attribute
import jakarta.persistence.metamodel.PluralAttribute
import jakarta.persistence.metamodel.SingularAttribute
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
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
          "ProjectExportImportPolicyRegistry (OWNED / USER_REF / PROJECT_ROOT / IGNORED):\n" +
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
            associationTargetClassName(attr),
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
    // Anti-vacuity: prove droppability was actually evaluated (a non-droppable singular FK found),
    // so the check isn't passing only because every association resolved to droppable.
    assertThat(nonDroppableEvaluated)
      .withFailMessage("No OWNED non-nullable singular FK was detected — droppability resolved nothing")
      .isGreaterThan(0)
  }

  private fun ownedEntityTypes() =
    entityManager.metamodel.entities
      .filter { ProjectExportImportPolicyRegistry.policyOf(it.javaType.name) == ExportImportPolicy.OWNED }

  private fun associationTargetClassName(attr: Attribute<*, *>): String =
    when (attr) {
      is PluralAttribute<*, *, *> -> attr.elementType.javaType.name
      is SingularAttribute<*, *> -> attr.type.javaType.name
      else -> attr.javaType.name
    }

  private fun isDroppable(
    entityClass: Class<*>,
    attr: Attribute<*, *>,
  ): Boolean {
    if (attr !is SingularAttribute<*, *>) return true
    return EntityAssociations.isDroppableSingularAssociation(entityClass, attr.name)
  }
}
