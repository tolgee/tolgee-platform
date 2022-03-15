package io.tolgee

import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationContext
import org.springframework.test.context.TestContext
import org.springframework.test.context.TestExecutionListener
import org.springframework.util.StringUtils.collectionToCommaDelimitedString
import java.sql.ResultSet
import javax.sql.DataSource
import kotlin.system.measureTimeMillis

class CleanDbTestListener : TestExecutionListener {
  private val logger = LoggerFactory.getLogger(this::class.java)

  override fun beforeTestMethod(testContext: TestContext) {
    if (!shouldClenBeforeClass(testContext)) {
      clean(testContext)
    }
  }

  private fun clean(testContext: TestContext) {
    logger.info("Cleaning DB")
    val time = measureTimeMillis {
      val appContext: ApplicationContext = testContext.applicationContext
      val ds: DataSource = appContext.getBean(DataSource::class.java)
      ds.connection.use { conn ->
        val stmt = conn.createStatement()
        val databaseName: Any = "postgres"
        val rs: ResultSet = stmt.executeQuery(
          String.format(
            "SELECT table_name" +
              " FROM information_schema.tables" +
              " WHERE table_catalog = '%s' and table_schema = 'public'" +
              "   and table_name not like 'databasechange%%'",
            databaseName
          )
        )
        val tables: MutableList<String> = ArrayList()
        while (rs.next()) {
          tables.add(rs.getString(1))
        }
        stmt.execute(java.lang.String.format("TRUNCATE TABLE %s", collectionToCommaDelimitedString(tables)))
      }
    }

    logger.info("DB cleaned in ${time}ms")
  }

  @Throws(Exception::class)
  override fun afterTestMethod(testContext: TestContext?) {
  }

  @Throws(Exception::class)
  override fun afterTestClass(testContext: TestContext?) {
  }

  @Throws(Exception::class)
  override fun beforeTestClass(testContext: TestContext) {
    if (shouldClenBeforeClass(testContext)) {
      clean(testContext)
    }
  }

  private fun shouldClenBeforeClass(testContext: TestContext) =
    testContext.testClass.isAnnotationPresent(CleanDbBeforeClass::class.java)

  @Throws(Exception::class)
  override fun prepareTestInstance(testContext: TestContext?) {
  }
}
