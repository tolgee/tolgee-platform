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

@SpringBootTest(
  properties = [
    "tolgee.authentication.jwtSecret=this_is_dummy_jwt_secret_azeazezaezaezaezaezzaezaezaeazeazezaeazezeaeazeazezaezaea"
  ]
)
@ContextRecreatingTest
class TimestampValidationTest {

  @set:Autowired
  lateinit var timestampValidation: TimestampValidation

  @Test
  fun testCheckTimeStamp() {
    val encrypted = timestampValidation.encryptTimeStamp("hello", Date().time - 10000)
    assertThatThrownBy {
      timestampValidation.checkTimeStamp("hello", encrypted, 9500)
    }.isInstanceOf(ValidationException::class.java)
    assertThatThrownBy {
      timestampValidation.checkTimeStamp("helloo", encrypted, 30000)
    }.isInstanceOf(ValidationException::class.java)
    timestampValidation.checkTimeStamp("hello", encrypted, 10500)
  }

  @Test
  fun isTimeStampValid() {
    val encrypted = timestampValidation.encryptTimeStamp("hello", Date().time - 10000)
    assertThat(timestampValidation.isTimeStampValid("hello", encrypted, 10500)).isTrue
    assertThat(timestampValidation.isTimeStampValid("hello", encrypted, 20500)).isTrue
    assertThat(timestampValidation.isTimeStampValid("hello", encrypted, 9500)).isFalse
    assertThat(timestampValidation.isTimeStampValid("hello", encrypted, 1000)).isFalse
    assertThat(timestampValidation.isTimeStampValid("hello", encrypted, 1000)).isFalse
  }

  @Test
  fun testEncryptTimeStamp() {
    val encrypted = timestampValidation.encryptTimeStamp("hello", 1608300796)
    assertThat(encrypted).isEqualTo("9095807353c7e00bfb1e001f1027e5e0be9cb6029aafdf231023a072887f8be5")
  }

  @Test
  fun testDecryptTimeStamp() {
    val timestamp = timestampValidation
      .decryptTimeStamp("9095807353c7e00bfb1e001f1027e5e0be9cb6029aafdf231023a072887f8be5")
    assertThat(timestamp?.timestamp).isEqualTo(1608300796)
    assertThat(timestamp?.entityUnique).isEqualTo("hello")
  }
}
