package io.tolgee.component.fileStorage

import com.azure.core.annotation.ReturnType
import com.azure.core.annotation.ServiceMethod
import com.azure.core.http.rest.Response
import com.azure.core.util.Context
import com.azure.core.util.FluxUtil
import com.azure.core.util.logging.ClientLogger
import com.azure.storage.blob.BlobAsyncClient
import com.azure.storage.blob.BlobClient
import com.azure.storage.blob.models.BlockBlobItem
import com.azure.storage.blob.options.BlobParallelUploadOptions
import com.azure.storage.blob.options.BlobUploadFromFileOptions
import com.azure.storage.common.implementation.StorageImplUtils
import reactor.core.publisher.Mono
import java.io.UncheckedIOException
import java.time.Duration
import java.util.*

/**
 * The Azure blob client uses deprecated method:
 * reactor.core.publisher.Mono reactor.core.publisher.Mono.subscriberContext
 * It was removed in mono 3.5 (minor version ugh..!)
 *
 * This class is a workaround for this issue.
 */
class TolgeeBlobClient(val client: BlobAsyncClient) : BlobClient(client) {
  private val logger = ClientLogger(BlobClient::class.java)

  @ServiceMethod(returns = ReturnType.SINGLE)
  override fun uploadWithResponse(
    options: BlobParallelUploadOptions,
    timeout: Duration?,
    context: Context?,
  ): Response<BlockBlobItem> {
    Objects.requireNonNull(options)
    val upload: Mono<Response<BlockBlobItem>> =
      client.uploadWithResponse(options)
        .contextWrite(FluxUtil.toReactorContext(context))

    try {
      return StorageImplUtils.blockWithOptionalTimeout(upload, timeout)
    } catch (e: UncheckedIOException) {
      throw logger.logExceptionAsError(e)
    }
  }

  @ServiceMethod(returns = ReturnType.SINGLE)
  override fun uploadFromFileWithResponse(
    options: BlobUploadFromFileOptions?,
    timeout: Duration?,
    context: Context?,
  ): Response<BlockBlobItem> {
    val upload: Mono<Response<BlockBlobItem>> =
      this.client.uploadFromFileWithResponse(options)
        .contextWrite(FluxUtil.toReactorContext(context))
    try {
      return StorageImplUtils.blockWithOptionalTimeout(upload, timeout)
    } catch (e: UncheckedIOException) {
      throw logger.logExceptionAsError(e)
    }
  }
}
