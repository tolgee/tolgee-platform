#!/usr/bin/env python3
"""
Performance Baseline Analysis Script

This script analyzes performance baseline data collected by the GitHub Actions workflow
and generates insights and trends from the metrics.
"""

import json
import os
import sys
from datetime import datetime
from pathlib import Path
from typing import Dict, List, Any, Optional
import statistics

class PerformanceAnalyzer:
    def __init__(self, data_dir: str = "performance-results"):
        self.data_dir = Path(data_dir)
        self.data = []
        
    def load_data(self) -> None:
        """Load all performance data from the results directory."""
        if not self.data_dir.exists():
            print(f"Error: Data directory {self.data_dir} does not exist")
            return
            
        for json_file in self.data_dir.glob("*.json"):
            try:
                with open(json_file, 'r') as f:
                    data = json.load(f)
                    data['source_file'] = json_file.name
                    self.data.append(data)
            except Exception as e:
                print(f"Warning: Could not load {json_file}: {e}")
    
    def get_summary_data(self) -> List[Dict[str, Any]]:
        """Extract summary data from all runs."""
        summaries = []
        for data in self.data:
            if 'summary' in data:
                summary = data.copy()
                summary['timestamp'] = datetime.fromisoformat(data['timestamp'].replace('Z', '+00:00'))
                summaries.append(summary)
        return sorted(summaries, key=lambda x: x['timestamp'])
    
    def analyze_build_times(self) -> Dict[str, Any]:
        """Analyze build time trends."""
        summaries = self.get_summary_data()
        if not summaries:
            return {}
        
        backend_times = [s['backend']['build_time_seconds'] for s in summaries]
        frontend_times = [s['frontend']['build_time_seconds'] for s in summaries]
        total_times = [s['summary']['total_build_time_seconds'] for s in summaries]
        
        return {
            'backend': {
                'mean': statistics.mean(backend_times),
                'median': statistics.median(backend_times),
                'min': min(backend_times),
                'max': max(backend_times),
                'std_dev': statistics.stdev(backend_times) if len(backend_times) > 1 else 0,
                'trend': 'increasing' if backend_times[-1] > backend_times[0] else 'decreasing'
            },
            'frontend': {
                'mean': statistics.mean(frontend_times),
                'median': statistics.median(frontend_times),
                'min': min(frontend_times),
                'max': max(frontend_times),
                'std_dev': statistics.stdev(frontend_times) if len(frontend_times) > 1 else 0,
                'trend': 'increasing' if frontend_times[-1] > frontend_times[0] else 'decreasing'
            },
            'total': {
                'mean': statistics.mean(total_times),
                'median': statistics.median(total_times),
                'min': min(total_times),
                'max': max(total_times),
                'std_dev': statistics.stdev(total_times) if len(total_times) > 1 else 0,
                'trend': 'increasing' if total_times[-1] > total_times[0] else 'decreasing'
            }
        }
    
    def analyze_bundle_sizes(self) -> Dict[str, Any]:
        """Analyze bundle size trends."""
        summaries = self.get_summary_data()
        if not summaries:
            return {}
        
        total_sizes = [s['frontend']['total_size_bytes'] for s in summaries]
        js_sizes = [s['frontend']['js_size_bytes'] for s in summaries]
        css_sizes = [s['frontend']['css_size_bytes'] for s in summaries]
        
        return {
            'total': {
                'mean': statistics.mean(total_sizes),
                'median': statistics.median(total_sizes),
                'min': min(total_sizes),
                'max': max(total_sizes),
                'trend': 'increasing' if total_sizes[-1] > total_sizes[0] else 'decreasing',
                'latest_mb': total_sizes[-1] / (1024 * 1024)
            },
            'javascript': {
                'mean': statistics.mean(js_sizes),
                'median': statistics.median(js_sizes),
                'min': min(js_sizes),
                'max': max(js_sizes),
                'trend': 'increasing' if js_sizes[-1] > js_sizes[0] else 'decreasing',
                'latest_mb': js_sizes[-1] / (1024 * 1024)
            },
            'css': {
                'mean': statistics.mean(css_sizes),
                'median': statistics.median(css_sizes),
                'min': min(css_sizes),
                'max': max(css_sizes),
                'trend': 'increasing' if css_sizes[-1] > css_sizes[0] else 'decreasing',
                'latest_mb': css_sizes[-1] / (1024 * 1024)
            }
        }
    
    def analyze_test_performance(self) -> Dict[str, Any]:
        """Analyze test execution time trends."""
        summaries = self.get_summary_data()
        if not summaries:
            return {}
        
        test_times = [s['backend']['total_test_time_seconds'] for s in summaries]
        
        return {
            'mean': statistics.mean(test_times),
            'median': statistics.median(test_times),
            'min': min(test_times),
            'max': max(test_times),
            'std_dev': statistics.stdev(test_times) if len(test_times) > 1 else 0,
            'trend': 'increasing' if test_times[-1] > test_times[0] else 'decreasing',
            'latest_minutes': test_times[-1] / 60
        }
    
    def generate_report(self) -> str:
        """Generate a comprehensive performance analysis report."""
        if not self.data:
            return "No performance data found."
        
        build_analysis = self.analyze_build_times()
        bundle_analysis = self.analyze_bundle_sizes()
        test_analysis = self.analyze_test_performance()
        
        report = []
        report.append("# Performance Baseline Analysis Report")
        report.append(f"Generated: {datetime.now().isoformat()}")
        report.append(f"Data points: {len(self.get_summary_data())}")
        report.append("")
        
        # Build Time Analysis
        if build_analysis:
            report.append("## Build Time Analysis")
            report.append("")
            
            for component in ['backend', 'frontend', 'total']:
                data = build_analysis[component]
                report.append(f"### {component.title()} Build Times")
                report.append(f"- Mean: {data['mean']:.2f}s")
                report.append(f"- Median: {data['median']:.2f}s")
                report.append(f"- Range: {data['min']:.2f}s - {data['max']:.2f}s")
                report.append(f"- Standard Deviation: {data['std_dev']:.2f}s")
                report.append(f"- Trend: {data['trend']}")
                report.append("")
        
        # Bundle Size Analysis
        if bundle_analysis:
            report.append("## Bundle Size Analysis")
            report.append("")
            
            for component in ['total', 'javascript', 'css']:
                data = bundle_analysis[component]
                report.append(f"### {component.title()} Bundle Size")
                report.append(f"- Mean: {data['mean'] / (1024*1024):.2f}MB")
                report.append(f"- Median: {data['median'] / (1024*1024):.2f}MB")
                report.append(f"- Range: {data['min'] / (1024*1024):.2f}MB - {data['max'] / (1024*1024):.2f}MB")
                report.append(f"- Latest: {data['latest_mb']:.2f}MB")
                report.append(f"- Trend: {data['trend']}")
                report.append("")
        
        # Test Performance Analysis
        if test_analysis:
            report.append("## Test Performance Analysis")
            report.append("")
            report.append(f"- Mean: {test_analysis['mean']:.2f}s ({test_analysis['mean']/60:.1f}min)")
            report.append(f"- Median: {test_analysis['median']:.2f}s ({test_analysis['median']/60:.1f}min)")
            report.append(f"- Range: {test_analysis['min']:.2f}s - {test_analysis['max']:.2f}s")
            report.append(f"- Standard Deviation: {test_analysis['std_dev']:.2f}s")
            report.append(f"- Latest: {test_analysis['latest_minutes']:.1f}min")
            report.append(f"- Trend: {test_analysis['trend']}")
            report.append("")
        
        # Recommendations
        report.append("## Performance Recommendations")
        report.append("")
        
        if build_analysis:
            if build_analysis['total']['trend'] == 'increasing':
                report.append("⚠️ **Build times are increasing** - Consider investigating recent changes that may be slowing down the build process.")
            
            if build_analysis['backend']['std_dev'] > build_analysis['backend']['mean'] * 0.2:
                report.append("⚠️ **Backend build times are inconsistent** - Consider optimizing build configuration or investigating flaky dependencies.")
            
            if build_analysis['frontend']['std_dev'] > build_analysis['frontend']['mean'] * 0.2:
                report.append("⚠️ **Frontend build times are inconsistent** - Consider optimizing build configuration or investigating flaky dependencies.")
        
        if bundle_analysis:
            if bundle_analysis['total']['trend'] == 'increasing':
                report.append("⚠️ **Bundle sizes are increasing** - Consider code splitting, tree shaking, or removing unused dependencies.")
            
            if bundle_analysis['javascript']['latest_mb'] > 2.0:
                report.append("⚠️ **JavaScript bundle is large** - Consider implementing code splitting or lazy loading.")
        
        if test_analysis:
            if test_analysis['trend'] == 'increasing':
                report.append("⚠️ **Test execution times are increasing** - Consider parallelizing tests or optimizing slow test cases.")
            
            if test_analysis['latest_minutes'] > 10:
                report.append("⚠️ **Test suite is taking a long time** - Consider running tests in parallel or optimizing slow tests.")
        
        return "\n".join(report)
    
    def save_report(self, output_file: str = "performance-analysis.md") -> None:
        """Save the analysis report to a file."""
        report = self.generate_report()
        with open(output_file, 'w') as f:
            f.write(report)
        print(f"Performance analysis report saved to {output_file}")

def main():
    """Main function to run the performance analysis."""
    if len(sys.argv) > 1:
        data_dir = sys.argv[1]
    else:
        data_dir = "performance-results"
    
    analyzer = PerformanceAnalyzer(data_dir)
    analyzer.load_data()
    
    if not analyzer.data:
        print("No performance data found. Make sure to run the performance baseline workflow first.")
        sys.exit(1)
    
    # Generate and display report
    report = analyzer.generate_report()
    print(report)
    
    # Save report
    analyzer.save_report()

if __name__ == "__main__":
    main() 