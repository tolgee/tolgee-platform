/*
 * Copyright (c) 2020. Tolgee
 */

package io.tolgee.component

import io.tolgee.dtos.request.validators.exceptions.ValidationException
import io.tolgee.testing.ContextRecreatingTest
import io.tolgee.testing.assertions.Assertions.assertThat
import io.tolgee.testing.assertions.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.util.*

@SpringBootTest(properties = ["tolgee.authentication.jwtSecret=this_is_dummy_jwt_secret"])
@ContextRecreatingTest
class TimestampValidationTest {

  @set:Autowired
  lateinit var timestampValidation: TimestampValidation

  @Test
  fun testCheckTimeStamp() {
    val encrypted = timestampValidation.encryptTimeStamp(Date().time - 10000)
    assertThatThrownBy {
      timestampValidation.checkTimeStamp(encrypted, 9500)
    }.isInstanceOf(ValidationException::class.java)
    timestampValidation.checkTimeStamp(encrypted, 10500)
  }

  @Test
  fun isTimeStampValid() {
    val encrypted = timestampValidation.encryptTimeStamp(Date().time - 10000)
    assertThat(timestampValidation.isTimeStampValid(encrypted, 10500)).isTrue
    assertThat(timestampValidation.isTimeStampValid(encrypted, 20500)).isTrue
    assertThat(timestampValidation.isTimeStampValid(encrypted, 9500)).isFalse
    assertThat(timestampValidation.isTimeStampValid(encrypted, 1000)).isFalse
    assertThat(timestampValidation.isTimeStampValid(encrypted, 1000)).isFalse
  }

  @Test
  fun testEncryptTimeStamp() {
    val encrypted = timestampValidation.encryptTimeStamp(1608300796)
    assertThat(encrypted).isEqualTo("e725d2ccb479cf3af6ce94017bd7ad52")
  }

  @Test
  fun testDecryptTimeStamp() {
    val timestamp = timestampValidation.decryptTimeStamp("e725d2ccb479cf3af6ce94017bd7ad52")
    assertThat(timestamp).isEqualTo(1608300796)
  }
}
