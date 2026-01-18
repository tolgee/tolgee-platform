### Batch Machine Translation Job Test Script

This script replicates the build and execution flow for Tolgee platform to test the batch job mechanism, specifically for Machine Translation.

#### What it does:
1.  **Builds the JAR**: Runs `./gradlew :server-app:bootJar` to create the application JAR.
2.  **Starts Redis**: Launches a Redis container via Docker for distributed caching and batch job coordination.
3.  **Starts Multiple Pods (Instances)**: Launches configurable number of Java processes of the Tolgee JAR on consecutive ports. This simulates a multi-node environment.
4.  **Generates and Imports Data**: Automatically generates a JSON file with a specified number of keys (defaulting to 10,000) and imports them into the first instance via REST API.
5.  **Initiates Batch Job**: Calls the `machineTranslation` batch job endpoint for the imported keys.
6.  **Monitors Progress**: Polls the database to track the batch job status and chunk execution progress until completion.
7.  **Reports Timing**: Calculates and displays the total execution time upon success.

#### Important Note:
This test is run **without any translation provider configured**. As a result, no actual translations are performed (the values won't change), but it allows for testing the **batch job orchestration, chunking, and multi-instance processing** logic of the platform.

#### Prerequisites:
- `docker`
- `jq`
- `psql` (libpq)
- `java` (JDK 21)

#### Usage:
Run from the project root:
```bash
./e2e/scripts/batch/run-batch-mt-job-with-2-pods-and-redis.sh [-k NUM_KEYS] [-n NUM_INSTANCES] [-p START_PORT]
```

Options:
- `-k NUM_KEYS` - Number of keys to process (default: 10000)
- `-n NUM_INSTANCES` - Number of Tolgee instances to start (default: 2)
- `-p START_PORT` - Starting port number (default: random port > 10000)

Examples:
```bash
# Default: 2 instances on random ports, 10000 keys
./e2e/scripts/batch/run-batch-mt-job-with-2-pods-and-redis.sh

# 3 instances starting at port 9000 with 1000 keys
./e2e/scripts/batch/run-batch-mt-job-with-2-pods-and-redis.sh -n 3 -p 9000 -k 1000

# 5 instances with random starting port
./e2e/scripts/batch/run-batch-mt-job-with-2-pods-and-redis.sh -n 5
```
