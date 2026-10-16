#!/usr/bin/env python3
"""Batch operations scale benchmark. See README.md."""

import argparse
import json
import os
import sys
import time

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from common import Ctx, log  # noqa: E402
from scenarios import SCENARIOS, Run  # noqa: E402
import seed as seeding  # noqa: E402


def make_ctx(args, out_name):
    urls = [u.strip() for u in args.app_urls.split(",") if u.strip()]
    return Ctx(os.path.join(args.out, out_name), args.pg_port, args.redis_port, urls)


def cmd_seed(args):
    ctx = make_ctx(args, "seed")
    seeding.seed(ctx, args.name, args.keys, args.langs, args.fill)


def cmd_add_langs(args):
    ctx = make_ctx(args, "seed")
    seeding.add_languages(ctx, args.project, args.count)


def cmd_run(args):
    label = args.label or time.strftime("run-%Y%m%d-%H%M%S")
    ctx = make_ctx(args, label)
    project = ctx.project(args.project)
    pids = [int(p) for p in args.pids.split(",")] if args.pids else []
    names = list(SCENARIOS) if args.scenarios == "all" else args.scenarios.split(",")
    results = {}
    for name in names:
        if name not in SCENARIOS:
            sys.exit(f"Unknown scenario {name}. Known: {', '.join(SCENARIOS)}")
        run = Run(ctx, args.project, project, args.keys, args.langs, args.profile, pids, args.probe, args.timeout,
                  args.lang_offset)
        log(f"=== {name} on {args.project} (keys={args.keys or 'all'}, langs={args.langs or 'all'}) ===")
        try:
            results[name] = SCENARIOS[name]["fn"](run)
        except Exception as e:  # noqa: BLE001
            log(f"scenario {name} failed: {e!r}")
            results[name] = {"error": repr(e)}
        with open(os.path.join(ctx.out_dir, f"summary-{args.project}.json"), "w") as f:
            json.dump(results, f, indent=2, default=str)


def main():
    here = os.path.dirname(os.path.abspath(__file__))
    default_out = os.environ.get("BENCH_OUT", os.path.join(here, "out"))
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument("--out", default=default_out, help="output root (runs/<label>/<scenario>/...)")
    p.add_argument("--pg-port", type=int, default=int(os.environ.get("BENCH_PG_PORT", 54715)))
    p.add_argument("--redis-port", type=int, default=int(os.environ.get("BENCH_REDIS_PORT", 53715)))
    p.add_argument("--app-urls", default=os.environ.get("BENCH_APP_URLS", "http://localhost:18715"),
                   help="comma separated, one per node")
    sub = p.add_subparsers(dest="cmd", required=True)

    s = sub.add_parser("seed")
    s.add_argument("--name", required=True)
    s.add_argument("--keys", type=int, required=True)
    s.add_argument("--langs", type=int, default=30)
    s.add_argument("--fill", type=float, default=0.7, help="share of non-base translations that exist")
    s.set_defaults(fn=cmd_seed)

    a = sub.add_parser("add-langs", help="add empty languages to a seeded project")
    a.add_argument("--project", required=True)
    a.add_argument("--count", type=int, default=3)
    a.set_defaults(fn=cmd_add_langs)

    r = sub.add_parser("run")
    r.add_argument("--project", required=True)
    r.add_argument("--scenarios", default="all", help=f"comma separated or 'all' ({', '.join(SCENARIOS)})")
    r.add_argument("--keys", type=int, default=0, help="limit the number of keys (0 = all)")
    r.add_argument("--langs", type=int, default=0, help="limit the number of target languages (0 = all)")
    r.add_argument("--lang-offset", type=int, default=0, help="skip the first N target languages")
    r.add_argument("--label", default=None)
    r.add_argument("--profile", default="none", choices=["none", "jfr", "cpu", "wall", "alloc", "lock"])
    r.add_argument("--pids", default=os.environ.get("BENCH_PIDS", ""), help="app JVM pids for profiling")
    r.add_argument("--probe", action="store_true", help="measure API latency while jobs run")
    r.add_argument("--timeout", type=int, default=3600)
    r.set_defaults(fn=cmd_run)

    args = p.parse_args()
    args.fn(args)


if __name__ == "__main__":
    main()
