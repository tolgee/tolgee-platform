package io.tolgee.model.task

import io.tolgee.model.Language
import io.tolgee.model.Project
import io.tolgee.model.UserAccount
import io.tolgee.model.enums.TaskState
import io.tolgee.model.enums.TaskType
import jakarta.persistence.*
import jakarta.validation.constraints.Size
import org.hibernate.annotations.Formula
import java.util.*

@Entity
@IdClass(TaskId::class)
class Task {
  @Id
  @ManyToOne(fetch = FetchType.LAZY)
  var project: Project = Project() // Initialize to avoid null issues

  @Id
  var id: Long = 1L

  @field:Size(max = 255)
  @Column(length = 255)
  var name: String = ""

  @field:Size(max = 2000)
  @Column(length = 2000)
  var description: String = ""

  @Enumerated(EnumType.STRING)
  var type: TaskType = TaskType.TRANSLATE

  @ManyToOne
  lateinit var language: Language

  var dueDate: Date? = null

  @ManyToMany(fetch = FetchType.EAGER)
  var assignees: MutableSet<UserAccount> = mutableSetOf()

  @OneToMany(mappedBy = "task", fetch = FetchType.LAZY)
  var translations: MutableSet<TaskTranslation> = mutableSetOf()

  @ManyToOne(fetch = FetchType.EAGER, optional = true)
  var author: UserAccount? = null

  var createdAt: Date = Date()

  @Enumerated(EnumType.STRING)
  var state: TaskState = TaskState.IN_PROGRESS

  var closedAt: Date? = null

  @Formula(
    """
    (select count(tt.translation_id)
    from task_translation tt
    where tt.task_id = id
      and tt.task_project_id = project_id)
  """,
  )
  private var totalItems: Long = 0

  fun getTotalItems(): Long {
    return totalItems
  }

  @Formula(
    """
    (select count(tt.translation_id)
    from task_translation tt
    where tt.done = True
      and tt.task_id = id
      and tt.task_project_id = project_id)
  """,
  )
  private var doneItems: Long = 0

  fun getDoneItems(): Long {
    return doneItems
  }

  @Formula(
    """
    (select COALESCE(sum(t.word_count), 0)
      from key k
        left join translation t on t.key_id = k.id
        left join project p on k.project_id = p.id
      where
          p.base_language_id = t.language_id
      and
        k.id in (
            select k.id
            from key k
                left join translation t on k.id = t.key_id
                left join task_translation tt on t.id = tt.translation_id
            where tt.task_id = id
                and k.project_id = project_id
        )
    )
  """,
  )
  private var baseWordCount: Long = 0

  fun getBaseWordCount(): Long {
    return baseWordCount
  }
}
