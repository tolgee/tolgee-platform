package io.tolgee.batch

import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest(
  properties = [
    "tolgee.cache.enabled=true",
  ],
  webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
)
class BatchJobsGeneralWithoutRedisTest : AbstractBatchJobsGeneralTest()
