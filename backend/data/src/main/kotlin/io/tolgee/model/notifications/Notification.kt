package io.tolgee.model.notifications

import io.tolgee.model.Project
import io.tolgee.model.StandardAuditModel
import io.tolgee.model.UserAccount
import io.tolgee.model.task.Task
import io.tolgee.model.translation.TranslationComment
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Index
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.annotations.ColumnDefault
import java.util.Date

@Entity
@Table(
  indexes = [
    Index(columnList = "user_id"),
  ],
)
class Notification : StandardAuditModel() {
  @ManyToOne(fetch = FetchType.LAZY)
  lateinit var user: UserAccount

  @Enumerated(EnumType.STRING)
  @ColumnDefault("TASK_ASSIGNED")
  var type: NotificationType = NotificationType.TASK_ASSIGNED

  @ManyToOne(fetch = FetchType.LAZY)
  var project: Project? = null

  @ManyToOne(fetch = FetchType.LAZY)
  var originatingUser: UserAccount? = null

  @ManyToOne(fetch = FetchType.LAZY)
  var linkedTask: Task? = null

  @ManyToOne(fetch = FetchType.LAZY)
  var linkedComment: TranslationComment? = null

  @ColumnDefault("false")
  var seen: Boolean = false

  var seenAt: Date? = null

  @ColumnDefault("false")
  var emailPending: Boolean = false

  var emailDueAt: Date? = null

  var emailWindowStartedAt: Date? = null

  /**
   * The user turned in-app off for this type but kept email on: the row only carries the pending email
   * and is never shown in the bell.
   */
  @ColumnDefault("false")
  var emailOnly: Boolean = false

  @ColumnDefault("0")
  var emailedCount: Int = 0
}
