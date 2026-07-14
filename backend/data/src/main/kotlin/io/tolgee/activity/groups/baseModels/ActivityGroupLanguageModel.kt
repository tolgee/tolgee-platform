package io.tolgee.activity.groups.baseModels

import io.tolgee.api.ILanguageModel

class ActivityGroupLanguageModel(
  override val id: Long,
  override val name: String,
  override val originalName: String,
  override val tag: String,
  override val flagEmoji: String,
) : ILanguageModel
