package io.tolgee.misc.dockerRunner

import io.tolgee.fixtures.waitFor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.BufferedReader
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
  private val logDebug: (String) -> Unit = {},
  private val logStdOut: ((String) -> Unit)? = null,
  private val logErrOut: ((String) -> Unit)? = null,
) {
  fun run() {
    if (stopBeforeStart) {
      stop()
    }
    startNewOrExistingContainer()
    startOutputLogging()
  }

  private fun startNewOrExistingContainer() {
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

  private fun startOutputLogging() {
    if (logStdOut == null && logErrOut == null) {
      return
    }

    val process = "docker logs $name -f".startProcess()
    val stdoutBufferedReader = process.inputStream.bufferedReader()
    val errorBufferedReader = process.errorStream.bufferedReader()

    @Suppress("OPT_IN_USAGE")
    GlobalScope.launch(Dispatchers.IO) {
      while (process.isAlive) {
        logOutputs(stdoutBufferedReader, errorBufferedReader)
      }
    }
  }

  private fun logOutputs(std: BufferedReader, err: BufferedReader) {
    logStdOut?.let { out ->
      std.forEachLine { out(it) }
    }

    logErrOut?.let { out ->
      std.forEachLine { out(it) }
    }
  }

  private fun startNewContainer() {
    val startTime = System.currentTimeMillis()
    val command = "docker run $rmString -d $exposeString$envString --name $containerName $image $command"
    logDebug("Running new container using command: $command")
    command.runCommand()
    waitForContainerLoggedOutput(startTime, waitForLogTimesForNewContainer)
  }

  private fun startExistingContainer() {
    val startTime = System.currentTimeMillis()
    if (!isContainerRunning()) {
      val command = "docker start $containerName"
      logDebug("Starting existing container using command: $command")
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
      logDebug("Waiting for container to log output using command: $command")
      val output = command.runCommand()

      logDebug("Waiting for string: $waitForLog in container output:\n $output")

      val result = output.containsTimes(waitForLog) >= times

      if (!result) {
        logDebug("String not found in container output, waiting...")
      } else {
        logDebug("String found in container output, continuing...")
      }

      result
    }
  }

  fun stop() {
    if (rm) {
      val command = "docker rm --force $containerName"
      logDebug("Removing container using command: $command")
      command.runCommand()
      return
    }
    if (isContainerRunning()) {
      val command = "docker stop $containerName"
      logDebug("Removing container using command: $command")
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
    val process = startProcessAndWait(workingDir, timeoutAmount, timeoutUnit)
    val errorOut = process.errorStream.bufferedReader().readText()
    val stdOut = process.inputStream.bufferedReader().use { it.readText() }

    if (process.exitValue() != 0) {
      throw CommandRunFailedException(errorOut, stdout = stdOut)
    }

    return stdOut + errorOut
  }

  private fun String.startProcessAndWait(workingDir: File, timeoutAmount: Long, timeoutUnit: TimeUnit): Process {
    return this.startProcess(workingDir).also { it.waitFor(timeoutAmount, timeoutUnit) }
  }

  private fun String.startProcess(workingDir: File = File(".")): Process =
    ProcessBuilder("\\s+".toRegex().split(this.trim()))
      .directory(workingDir)
      .redirectOutput(ProcessBuilder.Redirect.PIPE)
      .redirectError(ProcessBuilder.Redirect.PIPE)
      .start()

  private fun String?.containsTimes(string2: String): Int {
    return this?.windowed(string2.length) {
      if (it == string2) 1 else 0
    }?.sum() ?: 0
  }

  class CommandRunFailedException(val errorout: String, val stdout: String = "") :
    RuntimeException("Command execution failed\n\nError Output:\n$errorout\n\nStandard Output:\n$stdout")
}
