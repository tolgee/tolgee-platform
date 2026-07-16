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
  private val billingPrefix = "io.tolgee.billing."
  private val billingEntity = "${billingPrefix}data.model.Invoice"

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
  fun `classifies every billing entity as IGNORED without listing it`() {
    assertThat(ProjectExportImportPolicyRegistry.policyOf(billingEntity))
      .isEqualTo(ExportImportPolicy.IGNORED)
    assertThat(ProjectExportImportPolicyRegistry.policyOf("${billingPrefix}data.model.NotYetWrittenEntity"))
      .isEqualTo(ExportImportPolicy.IGNORED)
    assertThat(ProjectExportImportPolicyRegistry.unclassified(setOf(billingEntity, fake)))
      .containsExactly(fake)
  }

  @Test
  fun `the billing prefix does not classify a same-prefixed platform entity`() {
    assertThat(ProjectExportImportPolicyRegistry.policyOf("io.tolgee.billingsomething.Foo")).isNull()
  }

  @Test
  fun `staleEntries flags classified names absent from the managed set`() {
    assertThat(ProjectExportImportPolicyRegistry.staleEntries(emptySet()))
      .isNotEmpty
      .contains(Key::class.qualifiedName!!)
  }

  @Test
  fun `mayBeDeletedByImport is pinned (only the billing repo reads it, so platform CI cannot see a wrong value)`() {
    assertThat(ExportImportPolicy.entries.filterNot { it.mayBeDeletedByImport })
      .containsExactlyInAnyOrder(ExportImportPolicy.USER_REF, ExportImportPolicy.PROJECT_ROOT)
  }

  @Test
  fun `isNotGraphCarried is pinned (OWNED is its only divergence from mayBeDeletedByImport)`() {
    assertThat(ExportImportPolicy.entries.filter { it.isNotGraphCarried })
      .containsExactlyInAnyOrder(ExportImportPolicy.IGNORED, ExportImportPolicy.SIDE_CHANNEL)
  }

  @Test
  fun `no billing entity is listed in the registry (a listed name would be stale on every platform-only build)`() {
    val listedBillingNames =
      ProjectExportImportPolicyRegistry.listedClassNames.filter { it.startsWith(billingPrefix) }
    assertThat(listedBillingNames)
      .withFailMessage(
        "Billing entities must not be listed here. Remove: %s",
        listedBillingNames,
      ).isEmpty()
  }

  @Test
  fun `associationViolation rejects an unclassified target regardless of droppability`() {
    assertThat(ProjectExportImportPolicyRegistry.associationViolation(fake, droppable = true)).isNotNull()
    assertThat(ProjectExportImportPolicyRegistry.associationViolation(fake, droppable = false)).isNotNull()
  }

  @Test
  fun `associationViolation treats a billing target as IGNORED rather than unclassified`() {
    assertThat(ProjectExportImportPolicyRegistry.associationViolation(billingEntity, droppable = false))
      .isNotNull()
    assertThat(ProjectExportImportPolicyRegistry.associationViolation(billingEntity, droppable = true))
      .isNull()
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
  fun `associationViolation rejects a non-droppable reference to a SIDE_CHANNEL type`() {
    val sideChannelTarget = "io.tolgee.model.keyBigMeta.KeysDistance"
    assertThat(ProjectExportImportPolicyRegistry.policyOf(sideChannelTarget))
      .isEqualTo(ExportImportPolicy.SIDE_CHANNEL)
    assertThat(ProjectExportImportPolicyRegistry.associationViolation(sideChannelTarget, droppable = false))
      .isNotNull()
    assertThat(ProjectExportImportPolicyRegistry.associationViolation(sideChannelTarget, droppable = true))
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
