package io.tolgee.model.task

import io.tolgee.model.key.Key
import jakarta.persistence.FetchType
import jakarta.persistence.ManyToOne
import java.io.Serializable

data class TaskKeyId(
  @ManyToOne(fetch = FetchType.LAZY)
  var task: Task = Task(),
  @ManyToOne(fetch = FetchType.LAZY)
  var key: Key = Key(),
) : Serializable
