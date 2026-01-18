### Batch Job Performance Test Script

This script tests the batch job orchestration mechanism of the Tolgee platform using a NO_OP (no-operation) job type that measures pure orchestration overhead without any actual processing.

#### What it does:
1.  **Builds the JAR**: Runs `./gradlew :server-app:bootJar` to create the application JAR.
2.  **Starts Redis**: Launches a Redis container via Docker for distributed caching and batch job coordination.
3.  **Starts Multiple Pods (Instances)**: Launches configurable number of Java processes of the Tolgee JAR on consecutive ports. This simulates a multi-node environment.
4.  **Starts NO_OP Batch Job**: Calls the internal NO_OP batch job endpoint with fake item IDs.
5.  **Monitors Progress**: Polls the database to track the batch job status and chunk execution progress until completion.
6.  **Reports Timing**: Calculates and displays the total execution time upon success.

#### Important Note:
This test uses a **NO_OP batch job type** that does nothing except report progress. This isolates the batch job orchestration overhead from any actual processing logic, making it ideal for performance testing the multi-instance coordination.

#### Prerequisites:
- `docker`
- `jq`
- `psql` (libpq)
- `java` (JDK 21)

#### Usage:
Run from the project root:
```bash
./e2e/scripts/batch/run-batch-mt-job-with-2-pods-and-redis.sh [-k NUM_ITEMS] [-n NUM_INSTANCES] [-p START_PORT] [-c POSTGRES_CONTAINER] [-r REDIS_CONTAINER] [-P POSTGRES_PORT] [-R REDIS_PORT]
```

Options:
- `-k NUM_ITEMS` - Number of items to process (default: 10000)
- `-n NUM_INSTANCES` - Number of Tolgee instances to start (default: 2)
- `-p START_PORT` - Starting port number (default: 10020)
- `-c POSTGRES_CONTAINER` - PostgreSQL container name (default: tolgee-batch-jobs-perf-test-postgres)
- `-r REDIS_CONTAINER` - Redis container name (default: tolgee-batch-jobs-perf-test-redis)
- `-P POSTGRES_PORT` - PostgreSQL port (default: 25433)
- `-R REDIS_PORT` - Redis port (default: 6380)

Examples:
```bash
# Default: 2 instances on ports 10020-10021, 10000 items
./e2e/scripts/batch/run-batch-mt-job-with-2-pods-and-redis.sh

# 3 instances starting at port 9000 with 1000 items
./e2e/scripts/batch/run-batch-mt-job-with-2-pods-and-redis.sh -n 3 -p 9000 -k 1000

# 5 instances on ports 10020-10024
./e2e/scripts/batch/run-batch-mt-job-with-2-pods-and-redis.sh -n 5
```
