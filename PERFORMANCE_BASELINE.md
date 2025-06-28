# Performance Baseline System

This document describes the performance baseline system for the Tolgee platform, which establishes consistent performance metrics to track build times, test execution, bundle sizes, and other key performance indicators.

## Overview

The performance baseline system consists of:

1. **GitHub Actions Workflow** (`.github/workflows/performance-baseline.yml`) - Automatically collects performance metrics
2. **Analysis Script** (`scripts/analyze-performance.py`) - Analyzes collected data and generates insights
3. **Documentation** (this file) - Explains how to use the system

## What Metrics Are Collected

### Backend Metrics
- **Build Time**: Time to compile and package the Kotlin/Spring Boot application
- **JAR Size**: Size of the final executable JAR file
- **Memory Usage**: Memory consumption during build process
- **Test Execution Time**: Time to run each test suite individually
- **Total Test Time**: Combined time for all test suites

### Frontend Metrics
- **Install Time**: Time to install npm dependencies
- **Build Time**: Time to build the React/TypeScript application
- **Bundle Size**: Total size of the built application
- **JavaScript Size**: Size of JavaScript bundles
- **CSS Size**: Size of CSS bundles
- **File Count**: Number of files in the build output
- **TypeScript Compilation Time**: Time for TypeScript type checking
- **ESLint Time**: Time for code linting

### E2E Metrics
- **Install Time**: Time to install E2E test dependencies
- **Cypress Install Time**: Time to install Cypress browser

### System Metrics
- **CPU Cores**: Number of available CPU cores
- **Total Memory**: Total available system memory
- **Available Memory**: Available memory at measurement time
- **Disk Usage**: Current disk usage percentage
- **Network Speed**: Network interface speed (if available)

## How It Works

### Automatic Collection

The performance baseline workflow runs automatically:

1. **On every push to main branch** - Establishes performance baselines for each commit
2. **Manually** - Via workflow dispatch for ad-hoc measurements

### Data Storage

Performance data is stored as:
- **JSON files** in the `performance-results/` directory
- **GitHub Actions artifacts** for each workflow run
- **Markdown reports** for easy reading

### Data Format

Each metric collection creates JSON files with:
```json
{
  "timestamp": "2024-01-15T10:30:00Z",
  "commit": "abc123...",
  "branch": "main",
  "workflow_run_id": "123456789",
  "metrics": {
    "build_time_seconds": 45.23,
    "jar_size_bytes": 52428800,
    "memory_usage_percent": 67.5
  }
}
```

## How to Use

### Running the Baseline

#### Automatic (Recommended)
The workflow runs automatically on every push to the main branch, so you don't need to do anything. Each commit will generate performance metrics.

#### Manual Trigger
1. Go to the GitHub Actions tab
2. Select "Performance Baseline" workflow
3. Click "Run workflow"
4. Choose the branch to test
5. Click "Run workflow"

### Analyzing Results

#### View Recent Results
1. Go to the GitHub Actions tab
2. Find a recent "Performance Baseline" run
3. Download the artifacts:
   - `performance-baseline-{run_id}` - Raw JSON data
   - `performance-report-{run_id}` - Human-readable report

#### Run Analysis Script
```bash
# Analyze the latest data
python scripts/analyze-performance.py

# Analyze specific data directory
python scripts/analyze-performance.py path/to/performance-results
```

The analysis script will:
- Load all performance data
- Calculate trends and statistics
- Generate recommendations
- Save a comprehensive report

### Interpreting Results

#### Build Times
- **Good**: Consistent times with low standard deviation
- **Concerning**: Increasing trends or high variability
- **Action needed**: If build times increase by >20% or have >20% standard deviation

#### Bundle Sizes
- **Good**: Stable or decreasing sizes
- **Concerning**: Increasing trends
- **Action needed**: If bundle size increases by >10% or exceeds 2MB for JS

#### Test Performance
- **Good**: Fast, consistent test execution
- **Concerning**: Increasing test times
- **Action needed**: If test suite takes >10 minutes or shows increasing trend

## Performance Recommendations

The analysis script automatically generates recommendations based on:

### Build Performance
- Investigate increasing build times
- Optimize build configuration
- Check for flaky dependencies

### Bundle Optimization
- Implement code splitting
- Remove unused dependencies
- Enable tree shaking

### Test Optimization
- Parallelize slow tests
- Optimize test setup/teardown
- Remove redundant tests

## Integration with CI/CD

The performance baseline system integrates with your existing CI/CD pipeline:

### PR Comments
When the workflow runs on pull requests, it automatically comments with:
- Performance summary
- Key metrics comparison
- Recommendations

### Artifact Storage
All performance data is stored as GitHub Actions artifacts for:
- Historical analysis
- Trend tracking
- Performance regression detection

### Workflow Dependencies
The performance baseline workflow:
- Uses the same environment setup as your main test workflow
- Reuses build artifacts when possible
- Runs independently to avoid blocking other workflows

## Customization

### Adding New Metrics

To add new performance metrics:

1. **Modify the workflow** (`.github/workflows/performance-baseline.yml`):
   ```yaml
   - name: Measure new metric
     id: new-metric
     run: |
       # Your measurement logic here
       echo "new_metric_value=$value" >> $GITHUB_OUTPUT
   ```

2. **Update the summary generation**:
   ```yaml
   - name: Generate performance summary
     run: |
       # Add your metric to the summary.json
   ```

3. **Update the analysis script** (`scripts/analyze-performance.py`):
   ```python
   def analyze_new_metric(self):
       # Add analysis logic for your metric
   ```

### Modifying Triggers

To change when the workflow runs:

```yaml
on:
  workflow_dispatch:
  push:
    branches: [main]
    # Add path filters if you want to run only on specific file changes
    paths:
      - 'webapp/**'
      - 'backend/**'
      - 'build.gradle'
```

### Adjusting Thresholds

To modify performance thresholds in the analysis script:

```python
# In scripts/analyze-performance.py
if build_analysis['backend']['std_dev'] > build_analysis['backend']['mean'] * 0.2:  # 20% threshold
    report.append("⚠️ **Backend build times are inconsistent**")
```

## Troubleshooting

### Common Issues

#### Workflow Fails
- Check that all required tools are available (bc, jq, numfmt)
- Verify the setup-env action works correctly
- Check for permission issues with file operations

#### Missing Data
- Ensure the performance-results directory is created
- Check that JSON files are properly formatted
- Verify that all required environment variables are set

#### Analysis Script Errors
- Make sure Python 3.6+ is available
- Check that the data directory exists and contains JSON files
- Verify JSON file format matches expected schema

### Debugging

#### Enable Debug Output
Add debug information to the workflow:
```yaml
- name: Debug information
  run: |
    echo "Current directory: $(pwd)"
    echo "Files in performance-results:"
    ls -la performance-results/ || echo "Directory not found"
```

#### Check Individual Steps
Run individual measurement steps locally to debug:
```bash
# Test backend build measurement
./gradlew clean classes jar bootJar --no-daemon --parallel

# Test frontend build measurement
cd webapp
npm ci
npm run build
```

## Best Practices

### For Developers
1. **Monitor trends**: Check performance reports regularly
2. **Address regressions**: Fix performance issues before they accumulate
3. **Optimize incrementally**: Make small improvements rather than big changes
4. **Document changes**: Note performance-related changes in commit messages

### For Maintainers
1. **Review baselines**: Check performance reports for each commit
2. **Set alerts**: Configure notifications for performance regressions
3. **Update thresholds**: Adjust analysis thresholds based on project needs
4. **Archive old data**: Clean up old performance artifacts periodically

### For CI/CD
1. **Parallel execution**: Run performance baseline in parallel with other workflows
2. **Caching**: Use GitHub Actions caching for dependencies
3. **Artifact retention**: Set appropriate retention periods for performance data
4. **Resource allocation**: Ensure sufficient resources for accurate measurements

## Future Enhancements

Potential improvements to the performance baseline system:

1. **Visualization**: Add charts and graphs to performance reports
2. **Alerting**: Integrate with notification systems for regressions
3. **Historical analysis**: Add long-term trend analysis
4. **Custom metrics**: Allow teams to define their own performance metrics
5. **Integration**: Connect with external monitoring tools
6. **Benchmarking**: Compare against industry standards

## Contributing

To contribute to the performance baseline system:

1. **Fork the repository**
2. **Create a feature branch**
3. **Make your changes**
4. **Test thoroughly**
5. **Submit a pull request**

Please ensure that:
- All changes are documented
- Tests are added for new functionality
- Performance impact is considered
- Backward compatibility is maintained

## Support

For questions or issues with the performance baseline system:

1. **Check this documentation** first
2. **Review existing issues** on GitHub
3. **Create a new issue** with detailed information
4. **Contact the maintainers** for urgent issues

---

*This performance baseline system helps ensure the Tolgee platform maintains high performance standards and provides early warning of potential regressions.* 