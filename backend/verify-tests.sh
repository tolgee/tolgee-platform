#!/bin/bash
# Script to verify tests run correctly with optimized settings

echo "Running OptimizedRepositoryTest multiple times to verify data isolation..."

# Set Java compatibility and optimization flags
export JAVA_OPTS="-XX:+UseParallelGC -XX:GCTimeRatio=4 -XX:AdaptiveSizePolicyWeight=90 -XX:MinHeapFreeRatio=20 -XX:MaxHeapFreeRatio=40 -XX:+ExplicitGCInvokesConcurrent"

# Set Spring Boot properties for optimized testing
export SPRING_JPA_HIBERNATE_DDL_AUTO=create-drop
export SPRING_DATASOURCE_URL=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL
export SPRING_DATASOURCE_DRIVER_CLASS_NAME=org.h2.Driver
export SPRING_JPA_PROPERTIES_HIBERNATE_DIALECT=org.hibernate.dialect.H2Dialect
export SPRING_JPA_PROPERTIES_HIBERNATE_FORMAT_SQL=false
export SPRING_JPA_PROPERTIES_HIBERNATE_SHOW_SQL=false
export LOGGING_LEVEL_ORG_HIBERNATE_SQL=ERROR
export LOGGING_LEVEL_ORG_HIBERNATE_TYPE_DESCRIPTOR_SQL_BASICBINDER=ERROR
export SPRING_MAIN_LAZY_INITIALIZATION=true

# Now run the tests multiple times
for i in {1..5}; do
    echo "Run $i of 5..."
    ./gradlew :backend:data:test --tests "io.tolgee.example.OptimizedRepositoryTest" --no-daemon
    
    if [ $? -ne 0 ]; then
        echo "Test failed on run $i"
        exit 1
    fi
done

echo "All test runs completed successfully!"

# Run performance tests to measure improvements
echo "Running performance tests..."
./gradlew :backend:data:test --tests "io.tolgee.performance.PerformanceTest" --no-daemon

echo "Measuring context load time..."
./gradlew :backend:data:test --tests "io.tolgee.context.ContextLoadTimeTest" --no-daemon

echo "All verification tests completed!" 