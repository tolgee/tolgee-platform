package io.tolgee.misc.dockerRunner

import io.tolgee.fixtures.waitFor
import java.io.File
import java.util.*
import java.util.concurrent.TimeUnit

class DockerContainerRunner(
  private val image: String,
  private val expose: Map<String, String> = mapOf(),
  private val waitForLog: String,

  /**
   * How many times should the waitForLog string
   * be present in the log, when starting new container
   */
  private val waitForLogTimesForNewContainer: Int = 1,

  /**
   * How many times should the waitForLog string
   * be present in the log, when starting existing container
   */
  private val waitForLogTimesForExistingContainer: Int = 1,

  /**
   * Should container be removed after run?
   */
  private val rm: Boolean = true,
  private val name: String? = null,
  private val stopBeforeStart: Boolean = true,
  private val env: Map<String, String>? = null,
  private val command: String = "",
  private val timeout: Long = 10000,
  private val debug: (String) -> Unit = {}
) {
  fun run() {
    if (stopBeforeStart) {
      stop()
    }
    if (!this.rm) {
      startExistingOrNewContainer()
      return
    }
    startNewContainer()
  }

  private fun startExistingOrNewContainer() {
    try {
      startExistingContainer()
    } catch (e: CommandRunFailedException) {
      e.printStackTrace()
      startNewContainer()
    }
  }

  private fun startNewContainer() {
    val startTime = System.currentTimeMillis()
    val command = "docker run $rmString -d $exposeString$envString --name $containerName $image $command"
    debug("Running new container using command: $command")
    command.runCommand()
    waitForContainerLoggedOutput(startTime, waitForLogTimesForNewContainer)
  }

  private fun startExistingContainer() {
    val startTime = System.currentTimeMillis()
    if (!isContainerRunning()) {
      val command = "docker start $containerName"
      debug("Starting existing container using command: $command")
      command.runCommand()
      waitForContainerLoggedOutput(startTime, waitForLogTimesForExistingContainer)
    }
  }

  private fun isContainerRunning() = "docker ps".runCommand().contains(" $containerName\n")

  private fun waitForContainerLoggedOutput(startTime: Long, times: Int) {
    waitFor(timeout) {
      val since = System.currentTimeMillis() - startTime
      val sinceString = String.format(Locale.US, "%.03f", since.toFloat() / 1000)
      val command = "docker logs --since=${sinceString}s $containerName"
      debug("Waiting for container to log output using command: $command")
      val output = command.runCommand()

      debug("Waiting for string: $waitForLog in container output:\n $output")

      val result = output.containsTimes(waitForLog) >= times

      if (!result) {
        debug("String not found in container output, waiting...")
      } else {
        debug("String found in container output, continuing...")
      }

      result
    }
  }

  fun stop() {
    if (rm) {
      val command = "docker rm --force $containerName"
      debug("Removing container using command: $command")
      command.runCommand()
      return
    }
    if (isContainerRunning()) {
      val command = "docker stop $containerName"
      debug("Removing container using command: $command")
      command.runCommand()
    }
  }

  private val containerName: String by lazy {
    name ?: UUID.randomUUID().toString()
  }

  private val envString: String
    get() = env?.map { """ -e ${it.key}=${it.value}""" }?.joinToString("") ?: ""

  private val rmString = if (rm) "--rm" else ""

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
  ): String {
    val process = startProcess(workingDir, timeoutAmount, timeoutUnit)

    if (process.exitValue() != 0) {
      throw CommandRunFailedException(process.errorStream.bufferedReader().readText())
    }

    return process.inputStream.bufferedReader().use { it.readText() } +
      process.errorStream.bufferedReader().readText()
  }

  private fun String.startProcess(workingDir: File, timeoutAmount: Long, timeoutUnit: TimeUnit): Process {
    return ProcessBuilder("\\s+".toRegex().split(this.trim()))
      .directory(workingDir)
      .redirectOutput(ProcessBuilder.Redirect.PIPE)
      .redirectError(ProcessBuilder.Redirect.PIPE)
      .start().also { it.waitFor(timeoutAmount, timeoutUnit) }
  }

  private fun String?.containsTimes(string2: String): Int {
    return this?.windowed(string2.length) {
      if (it == string2) 1 else 0
    }?.sum() ?: 0
  }

  class CommandRunFailedException(val output: String) :
    RuntimeException("Command execution failed\n\nOutput:\n$output")
}
