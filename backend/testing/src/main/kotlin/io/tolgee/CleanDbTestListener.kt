package io.tolgee

import io.tolgee.batch.BatchJobChunkExecutionQueue
import io.tolgee.batch.BatchJobConcurrentLauncher
import kotlinx.coroutines.TimeoutCancellationException
import org.postgresql.util.PSQLException
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationContext
import org.springframework.test.context.TestContext
import org.springframework.test.context.TestExecutionListener
import java.sql.BatchUpdateException
import java.sql.ResultSet
import java.sql.Statement
import javax.sql.DataSource
import kotlin.system.measureTimeMillis

class CleanDbTestListener : TestExecutionListener {
  private val logger = LoggerFactory.getLogger(this::class.java)
  private val ignoredTables =
    listOf(
      "mt_credits_price",
      "databasechangelog",
      "databasechangeloglock",
    )

  override fun beforeTestMethod(testContext: TestContext) {
    if (!shouldClenBeforeClass(testContext)) {
      cleanWithRetries(testContext)
    }
  }

  private fun cleanWithRetries(testContext: TestContext) {
    logger.info("Cleaning DB")

    val appContext: ApplicationContext = testContext.applicationContext
    val batchJobConcurrentLauncher = appContext.getBean(BatchJobConcurrentLauncher::class.java)
    val batchJobQueue = appContext.getBean(BatchJobChunkExecutionQueue::class.java)

    batchJobConcurrentLauncher.pause = true
    batchJobQueue.clear()

    val time =
      measureTimeMillis {
        var i = 0
        while (true) {
          try {
            doClean(testContext)
            break
          } catch (e: Exception) {
            when (e) {
              is PSQLException, is TimeoutCancellationException, is BatchUpdateException -> {
                if (i > 5) {
                  throw e
                }
              }

              else -> throw e
            }
            logger.info(
              "Failed to clean DB, retrying in 1s. Attempt ${i + 1}, " +
                "error: ${e.message}, " +
                "stacktrace: ${e.stackTraceToString()}",
            )
            i++
          }
        }
      }

    batchJobConcurrentLauncher.pause = false
    logger.info("DB cleaned in ${time}ms")
  }

  private fun doClean(testContext: TestContext) {
    val appContext: ApplicationContext = testContext.applicationContext
    val ds: DataSource = appContext.getBean(DataSource::class.java)

    ds.connection.use { conn ->
      val stmt = conn.createStatement()
      try {
        val tables = stmt.getTablesToTruncate()
        stmt.disableConstraints(tables)
        val tablesString = tables.joinToString(",")

        stmt.execute(
          "SET statement_timeout = 5000;" +
            "TRUNCATE TABLE $tablesString;",
        )

        val enableConstraintsSQL = generateEnableConstraintsSQL(tables)
        enableConstraintsSQL.forEach { stmt.addBatch(it) }
        stmt.executeBatch()
      } catch (e: InterruptedException) {
        stmt.cancel()
        throw e
      }
    }
  }

  private fun Statement.disableConstraints(tables: List<String>) {
    val disableConstraintsSQL = generateDisableConstraintsSQL(tables)
    disableConstraintsSQL.forEach { addBatch(it) }
    executeBatch()
  }

  private fun Statement.getTablesToTruncate(): List<String> {
    val databaseName: Any = "postgres"
    val ignoredTablesString = ignoredTables.joinToString(", ") { "'$it'" }

    val rs: ResultSet =
      executeQuery(
        String.format(
          "SELECT table_schema, table_name" +
            " FROM information_schema.tables" +
            " WHERE table_catalog = '%s' and (table_schema in ('public', 'billing', 'ee'))" +
            "   and table_name not in ($ignoredTablesString)",
          databaseName,
        ),
      )
    val tables: MutableList<String> = ArrayList()
    while (rs.next()) {
      tables.add(rs.getString(1) + "." + rs.getString(2))
    }

    return tables
  }

  private fun generateDisableConstraintsSQL(tables: List<String>): List<String> {
    return tables.map { table ->
      "ALTER TABLE $table DISABLE TRIGGER ALL"
    }
  }

  private fun generateEnableConstraintsSQL(tables: List<String>): List<String> {
    return tables.map { table ->
      "ALTER TABLE $table ENABLE TRIGGER ALL"
    }
  }

  @Throws(Exception::class)
  override fun afterTestMethod(testContext: TestContext) {
  }

  @Throws(Exception::class)
  override fun afterTestClass(testContext: TestContext) {
  }

  @Throws(Exception::class)
  override fun beforeTestClass(testContext: TestContext) {
    if (shouldClenBeforeClass(testContext)) {
      cleanWithRetries(testContext)
    }
  }

  private fun shouldClenBeforeClass(testContext: TestContext) =
    testContext.testClass.isAnnotationPresent(CleanDbBeforeClass::class.java)

  @Throws(Exception::class)
  override fun prepareTestInstance(testContext: TestContext) {
  }
}
