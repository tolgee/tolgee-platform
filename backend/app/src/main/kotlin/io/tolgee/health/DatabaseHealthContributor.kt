package io.tolgee.health

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.actuate.health.Health
import org.springframework.boot.actuate.health.HealthContributor
import org.springframework.boot.actuate.health.HealthIndicator
import org.springframework.stereotype.Component
import java.sql.SQLException
import java.sql.Statement
import javax.sql.DataSource

@Component("Database")
class DatabaseHealthContributor :
  HealthIndicator,
  HealthContributor {
  @Autowired
  private lateinit var ds: DataSource

  override fun health(): Health {
    try {
      ds.connection.use { conn ->
        val stmt: Statement = conn.createStatement()
        stmt.execute("select name from user_account limit 1")
      }
    } catch (ex: SQLException) {
      return Health.outOfService().withException(ex).build()
    }
    return Health.up().build()
  }
}
