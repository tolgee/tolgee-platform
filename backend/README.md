# Tolgee Platform Development

## Prerequisites

- Java 17 (required for building and testing)
- Gradle 8.5+ (included via wrapper)

## Running Tests

### On Windows

Use the Java 17 wrapper script:

```
.\gradlew-java17.bat test
```

### On macOS/Linux

Use the Java 17 wrapper script:

```
./gradlew-java17 test
```

## Performance Testing

To run the optimized performance tests:

```
# Windows
.\gradlew-java17.bat :data:runParallelTests

# macOS/Linux
./gradlew-java17 :data:runParallelTests
```

## Troubleshooting

If you encounter Java version issues, set the JAVA_HOME_17 environment variable to point to your Java 17 installation:

### Windows
```
set JAVA_HOME_17=C:\path\to\java17
```

### macOS/Linux
```
export JAVA_HOME_17=/path/to/java17
``` 