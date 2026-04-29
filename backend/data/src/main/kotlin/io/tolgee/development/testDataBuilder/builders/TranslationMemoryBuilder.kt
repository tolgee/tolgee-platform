package io.tolgee.development.testDataBuilder.builders

import io.tolgee.development.testDataBuilder.FT
import io.tolgee.model.Project
import io.tolgee.model.translationMemory.TranslationMemory
import io.tolgee.model.translationMemory.TranslationMemoryEntry
import io.tolgee.model.translationMemory.TranslationMemoryProject
import io.tolgee.model.translationMemory.TranslationMemoryType

class TranslationMemoryBuilder(
  val organizationBuilder: OrganizationBuilder,
) : BaseEntityDataBuilder<TranslationMemory, TranslationMemoryBuilder>() {
  override var self: TranslationMemory =
    TranslationMemory().apply {
      organizationOwner = organizationBuilder.self
      type = TranslationMemoryType.PROJECT
    }

  class DATA {
    val entries = mutableListOf<TranslationMemoryEntryBuilder>()
    val projectAssignments = mutableListOf<TranslationMemoryProjectBuilder>()
  }

  var data = DATA()

  fun addEntry(ft: FT<TranslationMemoryEntry>) = addOperation(data.entries, ft)

  fun assignProject(
    project: Project,
    ft: FT<TranslationMemoryProject> = {},
  ): TranslationMemoryProjectBuilder {
    val builder = TranslationMemoryProjectBuilder(this)
    builder.self.project = project
    ft(builder.self)
    data.projectAssignments.add(builder)
    return builder
  }
}
