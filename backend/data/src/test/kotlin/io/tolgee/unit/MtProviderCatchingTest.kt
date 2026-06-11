package io.tolgee.unit

import io.tolgee.batch.FailedDontRequeueException
import io.tolgee.batch.MtProviderCatching
import io.tolgee.batch.MultipleItemsFailedException
import io.tolgee.batch.data.BatchTranslationTargetItem
import io.tolgee.component.CurrentDateProvider
import io.tolgee.constants.Message
import io.tolgee.exceptions.BadRequestException
import io.tolgee.security.OrganizationNotSelectedException
import io.tolgee.security.ProjectNotSelectedException
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import kotlin.coroutines.coroutineContext

class MtProviderCatchingTest {
  private val catching = MtProviderCatching(mock<CurrentDateProvider>())

  private val item = BatchTranslationTargetItem(keyId = 1, languageId = 2)

  @Test
  fun `ProjectNotSelectedException is fatal and skips retries`() {
    val thrown =
      assertThatThrownBy {
        runBlocking {
          catching.iterateCatching(listOf(item), coroutineContext) {
            throw ProjectNotSelectedException()
          }
        }
      }

    thrown.isInstanceOf(FailedDontRequeueException::class.java)
    thrown.hasCauseInstanceOf(ProjectNotSelectedException::class.java)
  }

  @Test
  fun `OrganizationNotSelectedException is fatal and skips retries`() {
    val thrown =
      assertThatThrownBy {
        runBlocking {
          catching.iterateCatching(listOf(item), coroutineContext) {
            throw OrganizationNotSelectedException()
          }
        }
      }

    thrown.isInstanceOf(FailedDontRequeueException::class.java)
    thrown.hasCauseInstanceOf(OrganizationNotSelectedException::class.java)
  }

  @Test
  fun `arbitrary BadRequestException is fatal and propagates its tolgeeMessage`() {
    val thrown =
      assertThatThrownBy {
        runBlocking {
          catching.iterateCatching(listOf(item), coroutineContext) {
            throw BadRequestException(Message.INVALID_PATH)
          }
        }
      }

    thrown.isInstanceOf(FailedDontRequeueException::class.java)
    val exception = thrown.actual() as FailedDontRequeueException
    assertThat(exception.tolgeeMessage).isEqualTo(Message.INVALID_PATH)
  }

  @Test
  fun `unrelated Throwable is still aggregated as MultipleItemsFailedException`() {
    val chunk = listOf(item, item.copy(keyId = 3))

    val thrown =
      assertThatThrownBy {
        runBlocking {
          catching.iterateCatching(chunk, coroutineContext) {
            throw IllegalStateException("boom")
          }
        }
      }

    thrown.isInstanceOf(MultipleItemsFailedException::class.java)
    val exception = thrown.actual() as MultipleItemsFailedException
    assertThat(exception.exceptions).hasSize(2)
  }
}
