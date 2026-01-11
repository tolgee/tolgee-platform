package io.tolgee.development.testDataBuilder.builders

import io.tolgee.model.key.Namespace

class NamespaceBuilder(
  val projectBuilder: ProjectBuilder,
) : BaseEntityDataBuilder<Namespace, NamespaceBuilder>() {
  override var self: Namespace = Namespace("homepage", projectBuilder.self)
}
