/**
 * Copyright (C) 2023 Tolgee s.r.o. and contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.tolgee

import com.zaxxer.hikari.HikariDataSource
import io.tolgee.development.testDataBuilder.data.TranslationsTestData
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.retry
import io.tolgee.fixtures.waitForNotThrowing
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import io.tolgee.testing.assert
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import javax.sql.DataSource

@SpringBootTest(
  properties = [
    "tolgee.rate-limits.global-limits = false",
    "tolgee.rate-limits.endpoint-limits = false",
    "tolgee.rate-limits.authentication-limits = false",
  ],
)
class StreamingBodyDatabasePoolHealthTest : ProjectAuthControllerTest("/v2/projects/") {
  lateinit var testData: TranslationsTestData

  @Autowired
  lateinit var dataSource: DataSource

  @BeforeEach
  fun setupData() {
    testData = TranslationsTestData()
    testDataService.saveTestData(testData.root)

    userAccount = testData.user
    projectSupplier = { testData.project }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `streaming responses do not cause a database connection pool exhaustion`() {
    // there is the bug in spring, co it throws the concurrent modification exception
    // to avoid this, we will retry the test until it passes,
    // but we will also increase the sleep time between requests to make it more probable to pass
    // I know, it's ugly. Sorry. If you have time to spare, remove the repeats and the sleep, maybe it will pass
    // in future spring versions
    // https://github.com/spring-projects/spring-security/issues/9175
    var sleepBetweenMs = 0L
    retry(
      retries = 100,
      exceptionMatcher = { it is ConcurrentModificationException || it is IllegalStateException },
    ) {
      try {
        val hikariDataSource = dataSource as HikariDataSource
        val pool = hikariDataSource.hikariPoolMXBean

        pool.idleConnections.assert.isGreaterThan(85)
        repeat(50) {
          performProjectAuthGet("export").andIsOk
          Thread.sleep(sleepBetweenMs)
        }
        waitForNotThrowing(pollTime = 50, timeout = 5000) {
          pool.idleConnections.assert.isGreaterThan(85)
        }
      } finally {
        sleepBetweenMs += 10
      }
    }
  }
}
