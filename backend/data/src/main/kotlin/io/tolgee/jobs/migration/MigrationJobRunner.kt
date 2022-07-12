package io.tolgee.jobs.migration

import org.springframework.batch.core.JobExecution

interface MigrationJobRunner {
  fun run(): JobExecution?
}
