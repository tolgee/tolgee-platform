package io.tolgee.util

import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

object ConcurrentTestUtil {
    fun <T> runConcurrently(
        threadCount: Int,
        timeoutSeconds: Long = 30,
        task: (Int) -> T
    ): List<T> {
        val executor = Executors.newFixedThreadPool(threadCount)
        val startLatch = CountDownLatch(1)
        val results = mutableListOf<AtomicReference<T>>()
        val exceptions = mutableListOf<AtomicReference<Throwable>>()
        
        try {
            repeat(threadCount) { threadIndex ->
                val result = AtomicReference<T>()
                val exception = AtomicReference<Throwable>()
                results.add(result)
                exceptions.add(exception)
                
                executor.submit {
                    try {
                        startLatch.await()
                        val taskResult = task(threadIndex)
                        result.set(taskResult)
                    } catch (e: Throwable) {
                        exception.set(e)
                    }
                }
            }
            
            startLatch.countDown()
            executor.shutdown()
            
            if (!executor.awaitTermination(timeoutSeconds, TimeUnit.SECONDS)) {
                throw RuntimeException("Test timed out after $timeoutSeconds seconds")
            }
            
            exceptions.forEachIndexed { index, exceptionRef ->
                if (exceptionRef.get() != null) {
                    throw RuntimeException("Exception in thread $index", exceptionRef.get())
                }
            }
            
            return results.map { it.get() }
        } finally {
            if (!executor.isShutdown) {
                executor.shutdownNow()
            }
        }
    }
} 