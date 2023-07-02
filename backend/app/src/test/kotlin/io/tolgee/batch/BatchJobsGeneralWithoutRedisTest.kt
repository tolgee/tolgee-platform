package io.tolgee.batch

import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.annotation.DirtiesContext

@SpringBootTest(
  properties = [
    "tolgee.cache.enabled=true",
  ],
  webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class BatchJobsGeneralWithoutRedisTest : AbstractBatchJobsGeneralTest()
