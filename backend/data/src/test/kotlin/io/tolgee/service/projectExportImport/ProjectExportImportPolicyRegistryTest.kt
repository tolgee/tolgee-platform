package io.tolgee.service.projectExportImport

import io.tolgee.model.Project
import io.tolgee.model.UserAccount
import io.tolgee.model.key.Key
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

/**
 * Metamodel-driven completeness/association guards over the real entity set live in
 * `io.tolgee.ee.projectExportImport.ProjectExportImportPolicyGuardTest` (where all modules' entities load).
 */
class ProjectExportImportPolicyRegistryTest {
  private val fake = "io.tolgee.model.DefinitelyNotAnEntity"

  @Test
  fun `classifies the load-bearing entities as expected`() {
    assertThat(ProjectExportImportPolicyRegistry.policyOf(Key::class.qualifiedName!!))
      .isEqualTo(ExportImportPolicy.OWNED)
    assertThat(ProjectExportImportPolicyRegistry.policyOf(UserAccount::class.qualifiedName!!))
      .isEqualTo(ExportImportPolicy.USER_REF)
    assertThat(ProjectExportImportPolicyRegistry.policyOf(Project::class.qualifiedName!!))
      .isEqualTo(ExportImportPolicy.PROJECT_ROOT)
    assertThat(ProjectExportImportPolicyRegistry.policyOf("io.tolgee.model.ApiKey"))
      .isEqualTo(ExportImportPolicy.IGNORED)
  }

  @Test
  fun `unclassified flags an unknown entity (the entity-guard's core predicate can go red)`() {
    assertThat(ProjectExportImportPolicyRegistry.policyOf(fake)).isNull()
    assertThat(ProjectExportImportPolicyRegistry.unclassified(setOf(Key::class.qualifiedName!!, fake)))
      .containsExactly(fake)
  }

  @Test
  fun `staleEntries flags classified names absent from the managed set`() {
    assertThat(ProjectExportImportPolicyRegistry.staleEntries(emptySet()))
      .isNotEmpty
      .contains(Key::class.qualifiedName!!)
  }

  @Test
  fun `associationViolation rejects an unclassified target regardless of droppability`() {
    assertThat(ProjectExportImportPolicyRegistry.associationViolation(fake, droppable = true)).isNotNull()
    assertThat(ProjectExportImportPolicyRegistry.associationViolation(fake, droppable = false)).isNotNull()
  }

  @Test
  fun `associationViolation rejects a non-droppable reference to an IGNORED type`() {
    val ignoredTarget = "io.tolgee.model.translationAgency.TranslationAgency"
    assertThat(ProjectExportImportPolicyRegistry.associationViolation(ignoredTarget, droppable = false))
      .isNotNull()
    assertThat(ProjectExportImportPolicyRegistry.associationViolation(ignoredTarget, droppable = true))
      .isNull()
  }

  @Test
  fun `associationViolation allows references to OWNED, USER_REF and PROJECT_ROOT targets`() {
    assertThat(ProjectExportImportPolicyRegistry.associationViolation(Key::class.qualifiedName!!, false)).isNull()
    assertThat(
      ProjectExportImportPolicyRegistry.associationViolation(UserAccount::class.qualifiedName!!, false),
    ).isNull()
    assertThat(ProjectExportImportPolicyRegistry.associationViolation(Project::class.qualifiedName!!, false)).isNull()
  }

  @Test
  fun `classifyUnique rejects classifying the same entity twice`() {
    val map = mutableMapOf<String, ExportImportPolicy>()
    map.classifyUnique("io.tolgee.model.Example", ExportImportPolicy.OWNED)
    assertThatThrownBy { map.classifyUnique("io.tolgee.model.Example", ExportImportPolicy.IGNORED) }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessageContaining("classified more than once")
  }
}
