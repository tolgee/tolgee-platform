package io.tolgee.model.task

import io.tolgee.activity.annotation.ActivityDescribingProp
import io.tolgee.activity.annotation.ActivityEntityDescribingPaths
import io.tolgee.activity.annotation.ActivityLoggedEntity
import io.tolgee.activity.annotation.ActivityLoggedProp
import io.tolgee.activity.propChangesProvider.EntityWithIdCollectionPropChangesProvider
import io.tolgee.model.Language
import io.tolgee.model.Project
import io.tolgee.model.StandardAuditModel
import io.tolgee.model.UserAccount
import io.tolgee.model.enums.TaskState
import io.tolgee.model.enums.TaskType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Index
import jakarta.persistence.ManyToMany
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import jakarta.validation.constraints.Size
import java.util.*

@Entity
@Table(
  uniqueConstraints = [
    UniqueConstraint(
      columnNames = ["project_id", "number"],
      name = "project_number_unique",
    ),
  ],
  indexes = [
    Index(columnList = "author_id"),
    Index(columnList = "language_id"),
  ],
)
@ActivityLoggedEntity
@ActivityEntityDescribingPaths(["language"])
class Task : StandardAuditModel() {
  @ManyToOne(fetch = FetchType.LAZY)
  var project: Project = Project() // Initialize to avoid null issues

  @ActivityLoggedProp
  @ActivityDescribingProp
  var number: Long = 1L

  @ActivityLoggedProp
  @ActivityDescribingProp
  @field:Size(max = 255)
  @Column(length = 255)
  var name: String = ""

  @ActivityLoggedProp
  @field:Size(max = 2000)
  @Column(length = 2000)
  var description: String = ""

  @ActivityLoggedProp
  @ActivityDescribingProp
  @Enumerated(EnumType.STRING)
  var type: TaskType = TaskType.TRANSLATE

  @ManyToOne(fetch = FetchType.LAZY)
  lateinit var language: Language

  @ActivityLoggedProp
  var dueDate: Date? = null

  @ManyToMany(fetch = FetchType.LAZY)
  @ActivityLoggedProp(EntityWithIdCollectionPropChangesProvider::class)
  var assignees: MutableSet<UserAccount> = mutableSetOf()

  @OneToMany(mappedBy = "task", fetch = FetchType.LAZY)
  var keys: MutableSet<TaskKey> = mutableSetOf()

  @ManyToOne(fetch = FetchType.LAZY, optional = true)
  @ActivityLoggedProp
  var author: UserAccount? = null

  @ActivityLoggedProp
  @Enumerated(EnumType.STRING)
  var state: TaskState = TaskState.NEW

  @ActivityLoggedProp
  var closedAt: Date? = null
}
