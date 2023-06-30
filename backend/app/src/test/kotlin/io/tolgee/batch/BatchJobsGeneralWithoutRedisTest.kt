package io.tolgee.batch

import io.tolgee.testing.ContextRecreatingTest
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest(
  webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ContextRecreatingTest
class BatchJobsGeneralWithoutRedisTest : AbstractBatchJobsGeneralTest()
