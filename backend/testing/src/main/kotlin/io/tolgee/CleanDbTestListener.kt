package io.tolgee

import io.tolgee.batch.BatchJobActionService
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.postgresql.util.PSQLException
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationContext
import org.springframework.test.context.TestContext
import org.springframework.test.context.TestExecutionListener
import java.sql.ResultSet
import javax.sql.DataSource
import kotlin.system.measureTimeMillis

class CleanDbTestListener : TestExecutionListener {
  private val logger = LoggerFactory.getLogger(this::class.java)
  private val ignoredTables = listOf(
    "mt_credits_price",
    "databasechangelog",
    "databasechangeloglock"
  )

  override fun beforeTestMethod(testContext: TestContext) {
    val appContext: ApplicationContext = testContext.applicationContext
    val batchJobActionService = appContext.getBean(BatchJobActionService::class.java)
    batchJobActionService.pause = true

    if (!shouldClenBeforeClass(testContext)) {
      cleanWithRetries(testContext)
    }

    batchJobActionService.pause = false
  }

  private fun cleanWithRetries(testContext: TestContext) {
    logger.info("Cleaning DB")
    val time = measureTimeMillis {
      var i = 0
      while (true) {
        try {
          runBlocking {
            withTimeout(3000) {
              doClean(testContext)
            }
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
    logger.info("DB cleaned in ${time}ms")
  }

  private fun doClean(testContext: TestContext) {
    val appContext: ApplicationContext = testContext.applicationContext
    val ds: DataSource = appContext.getBean(DataSource::class.java)
    ds.connection.use { conn ->
      val stmt = conn.createStatement()
      val databaseName: Any = "postgres"
      val ignoredTablesString = ignoredTables.joinToString(", ") { "'$it'" }
      val rs: ResultSet = stmt.executeQuery(
        String.format(
          "SELECT table_schema, table_name" +
            " FROM information_schema.tables" +
            " WHERE table_catalog = '%s' and (table_schema in ('public', 'billing', 'ee'))" +
            "   and table_name not in ($ignoredTablesString)",
          databaseName
        )
      )
      val tables: MutableList<Pair<String, String>> = ArrayList()
      while (rs.next()) {
        tables.add(rs.getString(1) to rs.getString(2))
      }
      stmt.execute(
        java.lang.String.format(
          "TRUNCATE TABLE %s",
          tables.joinToString(",") { it.first + "." + it.second }
        )
      )
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
