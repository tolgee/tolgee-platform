package io.tolgee.activity.groups.viewProviders.createProject

import io.tolgee.api.ILanguageModel

class LanguageModel(
  override val id: Long,
  override val name: String,
  override val originalName: String,
  override val tag: String,
  override val flagEmoji: String
) : ILanguageModel
