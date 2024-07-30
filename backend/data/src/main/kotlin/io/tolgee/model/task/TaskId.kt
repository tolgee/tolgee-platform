package io.tolgee.model.task

import io.tolgee.model.Project
import jakarta.persistence.FetchType
import jakarta.persistence.ManyToOne
import java.io.Serializable

data class TaskId(
  @ManyToOne(fetch = FetchType.LAZY)
  var project: Project = Project(),
  var id: Long = 1L,
) : Serializable
