package io.tolgee.testing

import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationListener
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.Instant
import java.util.concurrent.atomic.AtomicReference

@Component
class ContextLoadTimeListener : ApplicationListener<ContextRefreshedEvent> {
    companion object {
        private val startTime = AtomicReference<Instant>()
        private val loadTime = AtomicReference<Duration>()
        
        fun startTracking() {
            startTime.set(Instant.now())
        }
        
        fun getLoadTime(): Duration? {
            return loadTime.get()
        }
    }
    
    override fun onApplicationEvent(event: ContextRefreshedEvent) {
        val start = startTime.get() ?: return
        val end = Instant.now()
        val duration = Duration.between(start, end)
        loadTime.set(duration)
        
        println("Application context loaded in ${duration.toMillis()} ms")
    }
} 