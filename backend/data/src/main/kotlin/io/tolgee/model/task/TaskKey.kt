package io.tolgee.model.task

import io.tolgee.activity.annotation.ActivityEntityDescribingPaths
import io.tolgee.activity.annotation.ActivityLoggedEntity
import io.tolgee.activity.annotation.ActivityLoggedProp
import io.tolgee.model.StandardAuditModel
import io.tolgee.model.UserAccount
import io.tolgee.model.key.Key
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Index
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

@Entity
@ActivityLoggedEntity
@ActivityEntityDescribingPaths(paths = ["task", "key"])
@Table(
  uniqueConstraints = [
    UniqueConstraint(
      columnNames = ["task_id", "key_id"],
      name = "task_key_unique",
    ),
  ],
  indexes = [
    Index(columnList = "task_id"),
    Index(columnList = "key_id"),
  ],
)
class TaskKey(
  @ManyToOne(fetch = FetchType.LAZY)
  var task: Task = Task(),
  @ManyToOne(fetch = FetchType.LAZY)
  var key: Key = Key(),
  @ActivityLoggedProp
  var done: Boolean = false,
  @ActivityLoggedProp
  @ManyToOne(fetch = FetchType.LAZY)
  var author: UserAccount? = null,
) : StandardAuditModel()
