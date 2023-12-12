package io.tolgee.batch

import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest(
  properties = [
    "tolgee.cache.enabled=true",
    "disable-server-app-test-mock-overrides=true"
  ],
  webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
class BatchJobsGeneralWithoutRedisTest : AbstractBatchJobsGeneralTest()
