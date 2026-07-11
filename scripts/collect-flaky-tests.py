#!/usr/bin/env python3
"""Classify failed backend tests from merged JUnit XML into flaky vs hard failures.

The Gradle test-retry plugin, with `reports.junitXml.mergeReruns = true`,
records a test that failed then passed on retry as `<flakyFailure>` (or
`<flakyError>`) and a test that failed every attempt as `<failure>` (or
`<error>`), all within a single `<testcase>`.

This script scans the JUnit XML reports under the current directory and writes
two files:

  flaky-tests-<report>.txt    genuinely flaky tests  -> reported to the board
  failed-tests-<report>.txt   hard failures          -> reported to Slack only

Each line is a `<classname>.<name>` test id.

Usage:
  collect-flaky-tests.py <report-name>
"""
from __future__ import annotations

import glob
import sys
import xml.etree.ElementTree as ET

XML_GLOB = "**/build/test-results/**/TEST-*.xml"


def collect() -> tuple[set[str], set[str]]:
    """Return (flaky, hard-failed) test ids found in the JUnit XML reports."""
    flaky: set[str] = set()
    failed: set[str] = set()
    for path in glob.glob(XML_GLOB, recursive=True):
        try:
            tree = ET.parse(path)
        except ET.ParseError:
            continue
        for tc in tree.findall(".//testcase"):
            name = f'{tc.get("classname")}.{tc.get("name")}'
            has_failure = (
                tc.find("failure") is not None or tc.find("error") is not None
            )
            has_flaky = (
                tc.find("flakyFailure") is not None
                or tc.find("flakyError") is not None
            )
            if has_failure:
                failed.add(name)
            elif has_flaky:
                flaky.add(name)
    # A hard failure is authoritative: if the same test is reported both flaky
    # and hard-failed (e.g. across reruns), never mislabel it as flaky.
    flaky -= failed
    return flaky, failed


def write(file_name: str, names: set[str]) -> None:
    with open(file_name, "w") as fh:
        for name in sorted(names):
            fh.write(f"{name}\n")


def main() -> int:
    if len(sys.argv) != 2:
        print(__doc__, file=sys.stderr)
        return 2
    report = sys.argv[1]
    flaky, failed = collect()
    write(f"flaky-tests-{report}.txt", flaky)
    write(f"failed-tests-{report}.txt", failed)
    print(f"{report}: {len(flaky)} flaky, {len(failed)} hard failures")
    return 0


if __name__ == "__main__":
    sys.exit(main())
