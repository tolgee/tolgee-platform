package io.tolgee

import io.tolgee.batch.BatchJobChunkExecutionQueue
import io.tolgee.batch.BatchJobConcurrentLauncher
import kotlinx.coroutines.TimeoutCancellationException
import org.postgresql.util.PSQLException
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationContext
import org.springframework.test.context.TestContext
import org.springframework.test.context.TestExecutionListener
import java.sql.ResultSet
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
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
            withTimeout(3000) {
              doClean(testContext)
            }
            break
          } catch (e: Exception) {
            when (e) {
              is PSQLException, is TimeoutCancellationException -> {
                if (i > 5) {
                  throw e
                }
              }
            }
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
      val databaseName: Any = "postgres"
      val ignoredTablesString = ignoredTables.joinToString(", ") { "'$it'" }
      try {
        val rs: ResultSet =
          stmt.executeQuery(
            String.format(
              "SELECT table_schema, table_name" +
                " FROM information_schema.tables" +
                " WHERE table_catalog = '%s' and (table_schema in ('public', 'billing', 'ee'))" +
                "   and table_name not in ($ignoredTablesString)",
              databaseName,
            ),
          )
        val tables: MutableList<Pair<String, String>> = ArrayList()
        while (rs.next()) {
          tables.add(rs.getString(1) to rs.getString(2))
        }

        val disableConstraintsSQL = generateDisableConstraintsSQL(tables)
        disableConstraintsSQL.forEach { stmt.addBatch(it) }
        stmt.executeBatch()

        stmt.execute(
          java.lang.String.format(
            "TRUNCATE TABLE %s",
            tables.joinToString(",") { it.first + "." + it.second },
          ),
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

  private fun generateDisableConstraintsSQL(tables: List<Pair<String, String>>): List<String> {
    return tables.map { (schema, table) ->
      "ALTER TABLE \"$schema\".\"$table\" DISABLE TRIGGER ALL"
    }
  }

  private fun generateEnableConstraintsSQL(tables: List<Pair<String, String>>): List<String> {
    return tables.map { (schema, table) ->
      "ALTER TABLE \"$schema\".\"$table\" ENABLE TRIGGER ALL"
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

  private fun withTimeout(
    timeout: Long,
    block: () -> Unit,
  ) {
    val executor = Executors.newSingleThreadExecutor()
    val future: Future<Any> = executor.submit<Any>(block)

    try {
      println(future[timeout, TimeUnit.MILLISECONDS])
    } catch (e: TimeoutException) {
      future.cancel(true)
      throw e
    }

    executor.shutdownNow()
  }
}
