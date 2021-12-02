package io.tolgee.postgresStarters

interface PostgresRunner {
  fun run()
  val datasourceUrl: String
}
