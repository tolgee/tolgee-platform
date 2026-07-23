package io.tolgee.model.contributor

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.IdClass
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.util.Date

/**
 * Rows are inserted and updated exclusively by the `track_project_contributor` database trigger on
 * `activity_revision`; the application only reads this table — never persist it through JPA.
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
