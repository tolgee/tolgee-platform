package io.tolgee.model.task

import io.tolgee.activity.annotation.ActivityDescribingProp
import io.tolgee.activity.annotation.ActivityLoggedEntity
import io.tolgee.activity.annotation.ActivityLoggedProp
import io.tolgee.model.*
import io.tolgee.model.enums.TaskState
import io.tolgee.model.enums.TaskType
import jakarta.persistence.*
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
)
@ActivityLoggedEntity
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
  var assignees: MutableSet<UserAccount> = mutableSetOf()

  @OneToMany(mappedBy = "task", fetch = FetchType.LAZY)
  var keys: MutableSet<TaskKey> = mutableSetOf()

  @ManyToOne(fetch = FetchType.LAZY, optional = true)
  var author: UserAccount? = null

  @ActivityLoggedProp
  @Enumerated(EnumType.STRING)
  var state: TaskState = TaskState.NEW

  var closedAt: Date? = null
}
