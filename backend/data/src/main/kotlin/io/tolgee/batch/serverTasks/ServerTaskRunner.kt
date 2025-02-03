package io.tolgee.batch.serverTasks

import java.util.*

interface ServerTaskRunner {
  fun plan(
    executeAt: Date,
    data: Any,
  )

  fun execute(data: Any)
}
