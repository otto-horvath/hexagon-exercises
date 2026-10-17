# SPDX-License-Identifier: MIT
# Copyright (c) 2026 Otto Horvath
# https://github.com/otto-horvath/hexagon-exercises

"""Summarize Claude Code usage for the session log.

Reads Claude Code transcripts (JSONL files under ~/.claude/projects/) and
prints a `claude_usage` YAML block for docs/sessions/: wall-clock span, request
and token counts (observed), and an API list-price cost estimate (estimated).

Usage:
    python3 -I scripts/claude_usage.py                      # latest transcript for this repo
    python3 -I scripts/claude_usage.py --session <id>       # a specific session (repeatable)
    python3 -I scripts/claude_usage.py --since 2026-10-20T19:00 --until 2026-10-20T21:00

Times without an offset are local time. Standard library only.
"""

from __future__ import annotations

import argparse
import json
import sys
from datetime import datetime, timezone
from pathlib import Path

PRICES_CHECKED = "2026-10-16"
PRICES_SOURCE = "https://platform.claude.com/docs/en/about-claude/pricing"

# USD per million tokens: input, 5m cache write, 1h cache write, cache read, output.
PRICES = {
    "claude-opus-5-5": (4.00, 5.00, 8.00, 0.20, 20.00),
}
FAST_MODE_MULTIPLIER = 2.0  # Opus 5.5 fast mode is $8 / $40, twice the standard rates
WEB_SEARCH_USD = 10.00 / 1000  # per request


def project_dir(cwd: Path) -> Path:
    return Path.home() / ".claude" / "projects" / str(cwd).replace("/", "-")


def parse_time(value: str) -> datetime:
    parsed = datetime.fromisoformat(value.replace("Z", "+00:00"))
    return parsed if parsed.tzinfo else parsed.astimezone()


def transcripts(args: argparse.Namespace) -> list[Path]:
    root = Path(args.project_dir) if args.project_dir else project_dir(Path.cwd())
    if not root.is_dir():
        sys.exit(f"No Claude Code transcripts found in {root}")
    if args.session:
        files = [root / f"{session}.jsonl" for session in args.session]
    elif args.since:
        files = sorted(root.glob("*.jsonl"))
    else:
        files = sorted(root.glob("*.jsonl"), key=lambda path: path.stat().st_mtime)[-1:]
    # Subagent transcripts live next to their parent session.
    for file in list(files):
        files += sorted((root / file.stem).glob("**/*.jsonl"))
    missing = [file for file in files if not file.exists()]
    if missing:
        sys.exit(f"Transcript not found: {missing[0]}")
    return files


def summarize(files: list[Path], since: datetime | None, until: datetime | None) -> dict:
    seen = set()
    totals = dict.fromkeys(
        ["requests", "input", "cache_write_5m", "cache_write_1h", "cache_read", "output", "web_searches"], 0
    )
    cost = 0.0
    unpriced = set()
    sessions = set()
    first = last = None

    for file in files:
        for line in file.read_text().splitlines():
            entry = json.loads(line)
            message = entry.get("message")
            if not isinstance(message, dict) or "usage" not in message or "timestamp" not in entry:
                continue
            when = parse_time(entry["timestamp"])
            if (since and when < since) or (until and when > until):
                continue
            # A streamed response is written as several lines that repeat the same usage.
            key = entry.get("requestId") or message.get("id")
            if key in seen:
                continue
            seen.add(key)

            usage = message["usage"]
            creation = usage.get("cache_creation") or {}
            write_1h = creation.get("ephemeral_1h_input_tokens", 0)
            write_5m = usage.get("cache_creation_input_tokens", 0) - write_1h
            tokens = (
                usage.get("input_tokens", 0),
                write_5m,
                write_1h,
                usage.get("cache_read_input_tokens", 0),
                usage.get("output_tokens", 0),
            )
            searches = (usage.get("server_tool_use") or {}).get("web_search_requests", 0)

            model = message.get("model", "unknown")
            if model == "<synthetic>":  # written locally by Claude Code, never billed
                continue
            prices = PRICES.get(model)
            if prices is None:
                unpriced.add(model)
            else:
                multiplier = FAST_MODE_MULTIPLIER if usage.get("speed") == "fast" else 1.0
                cost += sum(count * price for count, price in zip(tokens, prices)) / 1_000_000 * multiplier
            cost += searches * WEB_SEARCH_USD

            totals["requests"] += 1
            for name, count in zip(["input", "cache_write_5m", "cache_write_1h", "cache_read", "output"], tokens):
                totals[name] += count
            totals["web_searches"] += searches

            sessions.add(entry.get("sessionId", file.stem)[:8])
            first = when if first is None or when < first else first
            last = when if last is None or when > last else last

    return {
        "totals": totals,
        "cost": cost,
        "unpriced": sorted(unpriced),
        "sessions": sorted(sessions),
        "first": first,
        "last": last,
    }


def print_yaml(summary: dict) -> None:
    totals = summary["totals"]
    if not totals["requests"]:
        sys.exit("No requests in the selected transcripts and time window.")
    first, last = summary["first"].astimezone(), summary["last"].astimezone()
    minutes = round((last - first).total_seconds() / 60)
    cost_note = f"estimated: tokens x API list price checked {PRICES_CHECKED}"
    if summary["unpriced"]:
        cost_note += f"; excludes unpriced models {', '.join(summary['unpriced'])}"
    print("claude_usage:")
    print(f"  sessions: [{', '.join(summary['sessions'])}]  # observed, Claude Code session ids")
    print(f'  first_request: "{first:%Y-%m-%d %H:%M}"  # observed, local time')
    print(f'  last_request: "{last:%Y-%m-%d %H:%M}"   # observed, local time')
    print(f"  wall_clock_minutes: {minutes}  # observed, first to last request, includes idle time")
    print(f"  requests: {totals['requests']}  # observed")
    print(f"  input_tokens: {totals['input']}  # observed")
    print(f"  cache_write_5m_tokens: {totals['cache_write_5m']}  # observed")
    print(f"  cache_write_1h_tokens: {totals['cache_write_1h']}  # observed")
    print(f"  cache_read_tokens: {totals['cache_read']}  # observed")
    print(f"  output_tokens: {totals['output']}  # observed")
    print(f"  web_searches: {totals['web_searches']}  # observed; Claude Code's own WebSearch tool is not recorded here")
    print(f"  cost_usd_estimated: {summary['cost']:.2f}  # {cost_note}")
    print("  cost_usd_reported: null  # reported, from /cost or the billing page")
    print("  billing: null  # reported: api | subscription")


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--session", action="append", help="session id (transcript file name); repeatable")
    parser.add_argument("--since", help="only requests at or after this time (ISO 8601)")
    parser.add_argument("--until", help="only requests at or before this time (ISO 8601)")
    parser.add_argument("--project-dir", help="transcript directory (default: the one for the current directory)")
    args = parser.parse_args()

    since = parse_time(args.since) if args.since else None
    until = parse_time(args.until) if args.until else None
    print_yaml(summarize(transcripts(args), since, until))
    print(f"# Prices: {PRICES_SOURCE}", file=sys.stderr)


if __name__ == "__main__":
    main()
