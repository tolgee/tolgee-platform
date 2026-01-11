package io.tolgee.development.testDataBuilder.builders

import io.tolgee.development.testDataBuilder.FT
import io.tolgee.model.Project
import io.tolgee.model.glossary.Glossary
import io.tolgee.model.glossary.GlossaryTerm

class GlossaryBuilder(
  val organizationBuilder: OrganizationBuilder,
) : BaseEntityDataBuilder<Glossary, GlossaryBuilder>() {
  override var self: Glossary =
    Glossary().apply {
      organizationOwner = organizationBuilder.self
      organizationBuilder.self.glossaries.add(this)
    }

  class DATA {
    val terms = mutableListOf<GlossaryTermBuilder>()
  }

  var data = DATA()

  fun addTerm(ft: FT<GlossaryTerm>) = addOperation(data.terms, ft)

  fun assignProject(project: Project) {
    self.assignedProjects.add(project)
    project.glossaries.add(self)
  }
}
