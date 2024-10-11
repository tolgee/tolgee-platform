package io.tolgee.component.bucket

import io.tolgee.testing.ContextRecreatingTest
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
@ContextRecreatingTest
class TokenBucketManagerTestWithoutRedis : AbstractTokenBucketManagerTest()
