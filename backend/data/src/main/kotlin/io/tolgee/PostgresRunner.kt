package io.tolgee

interface PostgresRunner {
  fun run()

  fun stop()

  /**
   * Whether migrations should be run or not.
   * Usefull for tests, where we don't want to wait for liquibase every time context starts
   */
  val shouldRunMigrations: Boolean
  val datasourceUrl: String
}
