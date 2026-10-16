#!/usr/bin/env python3
"""Summarizes async-profiler collapsed stacks: inclusive share of the most expensive frames.

usage: profile_summary.py file.collapsed [--thread-filter REGEX] [--frame-filter REGEX] [--top N]
"""
import argparse
import re
from collections import Counter


def main():
    p = argparse.ArgumentParser()
    p.add_argument("file")
    p.add_argument("--stack-filter", default=None, help="only stacks containing this regex")
    p.add_argument("--frame-filter", default=r"io[./]tolgee|org[./]hibernate|org[./]postgresql|redisson|jackson",
                   help="frames to rank")
    p.add_argument("--top", type=int, default=40)
    a = p.parse_args()
    frame_re = re.compile(a.frame_filter)
    stack_re = re.compile(a.stack_filter) if a.stack_filter else None
    inclusive = Counter()
    self_time = Counter()
    total = 0
    with open(a.file) as f:
        for line in f:
            stack, _, n = line.rstrip().rpartition(" ")
            if not stack:
                continue
            if stack_re and not stack_re.search(stack):
                continue
            n = int(n)
            total += n
            frames = stack.split(";")
            self_time[frames[-1]] += n
            for fr in set(frames):
                if frame_re.search(fr):
                    inclusive[fr] += n
    print(f"total samples: {total}")
    print("\n== inclusive (filtered frames) ==")
    for fr, n in inclusive.most_common(a.top):
        print(f"{100.0 * n / total:6.2f}%  {fr}")
    print("\n== self (leaf frames) ==")
    for fr, n in self_time.most_common(25):
        print(f"{100.0 * n / total:6.2f}%  {fr}")


if __name__ == "__main__":
    main()
