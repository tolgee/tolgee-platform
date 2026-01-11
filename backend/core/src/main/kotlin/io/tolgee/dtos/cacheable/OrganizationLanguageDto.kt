package io.tolgee.dtos.cacheable

interface OrganizationLanguageDto {
  val name: String
  val tag: String
  val originalName: String?
  val flagEmoji: String?
  val base: Boolean
}
