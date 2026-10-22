package io.tolgee.model.contributor

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.IdClass
import jakarta.persistence.Index
import jakarta.persistence.Table
import org.hibernate.annotations.Immutable
import java.time.LocalDate

/** Rows arrive from the `track_community_contribution_day` trigger and `communityContributionDayBackfill.sql`, never from JPA. */
@Entity
@Immutable
@IdClass(CommunityContributionDayId::class)
@Table(
  name = "community_contribution_day",
  indexes = [
    Index(name = "community_contribution_day_user_id", columnList = "user_id"),
  ],
)
class CommunityContributionDay(
  @Id
  @Column(name = "project_id")
  val projectId: Long,
  @Id
  @Column(name = "user_id")
  val userId: Long,
  @Id
  @Column(name = "language_id")
  val languageId: Long,
  @Id
  val day: LocalDate,
  @Column(nullable = false)
  val suggested: Int,
  @Column(nullable = false)
  val accepted: Int,
  @Column(nullable = false)
  val declined: Int,
)
