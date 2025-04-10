package io.tolgee.postgresRunners

import io.tolgee.PostgresRunner
import io.tolgee.configuration.tolgee.FileStorageProperties
import io.tolgee.configuration.tolgee.PostgresAutostartProperties
import io.tolgee.fixtures.waitFor
import org.slf4j.LoggerFactory
import java.io.IOException
import java.io.InputStream
import java.net.Socket
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

class PostgresEmbeddedRunner(
  private val postgresAutostartProperties: PostgresAutostartProperties,
  private val storageProperties: FileStorageProperties,
) : PostgresRunner {
  private val logger = LoggerFactory.getLogger(javaClass)
  private lateinit var proc: Process
  private var running: AtomicBoolean = AtomicBoolean(false)

  override fun run() {
    if (!::proc.isInitialized) {
      startPostgresProcess()
      startWatchingThread()
      waitForPostgres()
    }
  }

  override fun stop() {
    proc.destroy()
    proc.waitFor()
    running.set(false)
  }

  override val shouldRunMigrations: Boolean = true

  private fun startPostgresProcess() {
    val processBuilder = buildProcess()
    logger.info("Starting embedded Postgres DB...")
    proc = processBuilder.start()
    running.set(true)
  }

  private fun buildProcess(): ProcessBuilder {
    val processBuilder =
      ProcessBuilder()
        .command("bash", "-c", "docker-entrypoint.sh postgres")

    initProcessEnv(processBuilder)
    return processBuilder
  }

  private fun initProcessEnv(processBuilder: ProcessBuilder) {
    val env = processBuilder.environment()

    env.putAll(
      mapOf(
        "POSTGRES_PASSWORD" to postgresAutostartProperties.password,
        "POSTGRES_USER" to postgresAutostartProperties.user,
        "POSTGRES_DB" to postgresAutostartProperties.databaseName,
        "PGDATA" to storageProperties.fsDataPath + "/postgres",
      ),
    )
  }

  private fun startWatchingThread() {
    thread(start = true) {
      logOutput(
        mapOf(
          proc.inputStream to logger::info,
          proc.errorStream to logger::error,
        ),
      )
      if (proc.exitValue() != 0) {
        throw Exception("Postgres failed to start...")
      }
    }
  }

  private fun waitForPostgres() {
    waitFor(20000) {
      isPostgresUp()
    }
  }

  override val datasourceUrl by lazy {
    // It's not that easy to change port in embedded version, since there is no env prop for that
    "jdbc:postgresql://localhost:$POSTGRES_PORT/${postgresAutostartProperties.databaseName}"
  }

  private fun isPostgresUp(): Boolean {
    var s: Socket? = null
    return try {
      s = Socket("localhost", POSTGRES_PORT)
      true
    } catch (e: java.lang.Exception) {
      false
    } finally {
      if (s != null) {
        try {
          s.close()
        } catch (e: java.lang.Exception) {
          e.printStackTrace()
        }
      }
    }
  }

  private fun logOutput(loggerMap: Map<InputStream, (message: String) -> Unit>) {
    while (running.get()) {
      try {
        val allNull =
          loggerMap.entries
            .map { (inputStream, logger) ->
              val line = inputStream.bufferedReader().readLine()
              logLine(line, logger)
              line == null
            }.all { it }
        if (allNull) {
          break
        }
      } catch (e: IOException) {
        logger.debug("Resolved IOException ${e.message}")
      }
    }
  }

  private fun logLine(
    line: String?,
    logger: (message: String) -> Unit,
  ) {
    if (line != null) {
      logger("Postgres: $line")
    }
  }

  companion object {
    private const val POSTGRES_PORT = 5432
  }
}
