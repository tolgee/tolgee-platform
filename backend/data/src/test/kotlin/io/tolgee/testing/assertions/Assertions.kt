package io.tolgee.testing.assertions

import jakarta.persistence.EntityManager
import org.junit.jupiter.api.Assertions
import org.springframework.test.util.ReflectionTestUtils

class Assertions {
    companion object {
        private lateinit var entityManager: EntityManager

        fun setEntityManager(em: EntityManager) {
            entityManager = em
        }

        fun <T> assertEntityEquals(expected: T, actual: T) {
            if (expected == null && actual == null) {
                return
            }
            
            if (expected == null || actual == null) {
                Assertions.fail<Any>("One of the entities is null")
                return
            }
            
            val expectedFields = ReflectionTestUtils.getFields(expected::class.java)
                .filter { !it.name.startsWith("$") }
            
            for (field in expectedFields) {
                field.isAccessible = true
                val expectedValue = field.get(expected)
                val actualValue = field.get(actual)
                
                Assertions.assertEquals(
                    expectedValue, 
                    actualValue, 
                    "Field ${field.name} values don't match"
                )
            }
        }
    }
} 