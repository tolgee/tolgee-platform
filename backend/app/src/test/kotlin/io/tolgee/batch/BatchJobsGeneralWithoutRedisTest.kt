package io.tolgee.batch

import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest(
  webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
class BatchJobsGeneralWithoutRedisTest : AbstractBatchJobsGeneralTest()
