package io.tolgee.fixtures

import io.tolgee.component.fileStorage.FileStorage
import io.tolgee.testing.assert
import org.mockito.Mockito
import org.mockito.invocation.Invocation

data class StoredFileCall(
  val path: String,
  val contentType: String?,
)

fun FileStorage.assertStoredSingleFile(
  pathPattern: String,
  contentType: String?,
) {
  val calls = getStoreFileCalls()
  calls.assert.describedAs("storeFile calls").hasSize(1)
  val stored = calls.single()
  stored.path.assert.matches(pathPattern)
  stored.contentType.assert.isEqualTo(contentType)
}

fun FileStorage.assertPrunedSingleDirectory(pathPattern: String) {
  val pruned = getPrunedDirectories()
  pruned.assert.describedAs("pruneDirectory calls").hasSize(1)
  pruned.single().assert.matches(pathPattern)
}

fun FileStorage.getStoreFileCalls(): List<StoredFileCall> =
  getInvocations()
    .filter { it.method.name == "storeFile" }
    .map { invocation ->
      StoredFileCall(
        path = invocation.getArgument(0),
        contentType = invocation.getArgument(2),
      )
    }

fun FileStorage.getPrunedDirectories(): List<String> =
  getInvocations()
    .filter { it.method.name == "pruneDirectory" }
    .map { invocation -> invocation.getArgument(0) }

private fun FileStorage.getInvocations(): List<Invocation> = Mockito.mockingDetails(this).invocations.toList()
