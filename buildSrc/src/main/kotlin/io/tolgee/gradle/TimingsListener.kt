@file:Suppress("DEPRECATION")

package io.tolgee.gradle

import org.gradle.BuildListener
import org.gradle.BuildResult
import org.gradle.api.Task
import org.gradle.api.execution.TaskExecutionListener
import org.gradle.api.initialization.Settings
import org.gradle.api.invocation.Gradle
import org.gradle.api.tasks.TaskState
import java.time.LocalTime
import java.util.concurrent.TimeUnit

class TimingsListener : TaskExecutionListener, BuildListener {
  var startTime: Long = 0
  var timings = mutableListOf<TimingEntry>()

  override fun beforeExecute(task: Task) {
    startTime = System.nanoTime()
  }

  override fun afterExecute(task: Task, state: TaskState) {
    val ms = TimeUnit.MILLISECONDS.convert(System.nanoTime() - startTime, TimeUnit.NANOSECONDS);
    timings += TimingEntry(ms, task.path)

    val now = LocalTime.now()
    task.project.logger.warn("$now ${task.path} took ${ms}ms")
  }

  override fun buildFinished(result: BuildResult) {
    println("Task timings:")
    for (timing in timings) {
      if (timing.ms >= 50L) {
        println(String.format("%dms  %s", timing.ms, timing.task))
      }
    }
  }

  override fun settingsEvaluated(settings: Settings) {
  }

  override fun projectsLoaded(gradle: Gradle) {
  }

  override fun projectsEvaluated(gradle: Gradle) {
  }

  data class TimingEntry(val ms: Long, val task: String)

}
