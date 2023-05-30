package io.tolgee.api.v2.controllers.configurationProps

data class Property(
  override val name: String,
  override val nameWithDashes: String?,
  override val displayName: String?,
  override val description: String?,
  val defaultValue: String? = null,
  val defaultExplanation: String? = null,
  val removedIn: String? = null,
) : DocItem

interface DocItem {
  val name: String
  val nameWithDashes: String?
  val displayName: String?
  val description: String?
}

data class Group(
  override val name: String,
  override val nameWithDashes: String?,
  override val displayName: String?,
  override val description: String?,
  val children: List<DocItem>,
  val prefix: String?
) : DocItem
