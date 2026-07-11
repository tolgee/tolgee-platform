package io.tolgee.ee.data.glossary

interface GlossaryWithStats {
  val id: Long
  val name: String
  val baseLanguageTag: String
  val firstAssignedProjectName: String?
  val assignedProjectsCount: Long
}
