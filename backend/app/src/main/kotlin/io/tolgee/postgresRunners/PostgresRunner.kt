package io.tolgee.postgresRunners

interface PostgresRunner {
  fun run()
  val datasourceUrl: String
}
