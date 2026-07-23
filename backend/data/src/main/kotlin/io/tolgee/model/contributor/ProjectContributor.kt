package io.tolgee.model.contributor

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.IdClass
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.util.Date

/**
 * Inserted only by the `track_project_contributor` trigger; the app reads and bulk-deletes on project
 * hard-delete, never inserts via JPA.
 */
@Entity
@IdClass(ProjectContributorId::class)
@Table(
  name = "project_contributor",
  indexes = [
    Index(name = "project_contributor_user_id", columnList = "user_id"),
  ],
)
class ProjectContributor(
  @Id
  @Column(name = "project_id")
  val projectId: Long,
  @Id
  @Column(name = "user_id")
  val userId: Long,
  @Column(name = "first_contribution_at", nullable = false)
  var firstContributionAt: Date,
  @Column(name = "last_contribution_at", nullable = false)
  var lastContributionAt: Date,
)
