package io.tolgee.fixtures

import java.io.File
import java.util.*
import java.util.concurrent.TimeUnit

class DockerContainerRunner(
  private val image: String,
  private val expose: Map<String, String>,
  private val waitForLog: String,
  private val rm: Boolean = true,
  private val name: String? = null,
  private val stopBeforeStart: Boolean = true
) {

  fun run() {
    if (stopBeforeStart) {
      stop()
    }
    val rmString = if (rm) "--rm" else ""
    "docker run $rmString -d $exposeString --name $containerName $image".runCommand()
    waitFor {
      val output = "docker logs $containerName".runCommand()
      output?.contains(waitForLog) ?: false
    }
  }

  fun stop() {
    "docker rm --force $containerName".runCommand()
  }

  private val containerName: String by lazy {
    name ?: UUID.randomUUID().toString()
  }

  private val exposeString: String
    get() {
      return expose
        .map { (hostPort, containerPort) -> "-p$hostPort:$containerPort" }
        .joinToString(" ")
    }

  private fun String.runCommand(
    workingDir: File = File("."),
    timeoutAmount: Long = 120,
    timeoutUnit: TimeUnit = TimeUnit.SECONDS
  ): String? = runCatching {
    ProcessBuilder("\\s".toRegex().split(this))
      .directory(workingDir)
      .redirectOutput(ProcessBuilder.Redirect.PIPE)
      .redirectError(ProcessBuilder.Redirect.PIPE)
      .start().also { it.waitFor(timeoutAmount, timeoutUnit) }
      .inputStream.bufferedReader().readText()
  }.onFailure {
    it.printStackTrace()
  }.getOrNull()
}
