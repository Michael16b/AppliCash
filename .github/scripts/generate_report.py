#!/usr/bin/env python3
"""
Combine per-device JSON results into compatibility-report.csv and compatibility-report.json
Usage: generate_report.py <results_dir> <out_prefix>
"""
import sys
import json
import csv
from pathlib import Path

if len(sys.argv) < 3:
    print("Usage: generate_report.py <results_dir> <out_prefix>")
    sys.exit(2)

results_dir = Path(sys.argv[1])
out_prefix = Path(sys.argv[2])

entries = []
for p in results_dir.rglob('result.json'):
    try:
        with open(p, 'r', encoding='utf-8') as f:
            data = json.load(f)
            entries.append(data)
    except Exception as e:
        print(f"Failed to read {p}: {e}")

if not out_prefix.parent.exists():
    out_prefix.parent.mkdir(parents=True, exist_ok=True)

csv_path = out_prefix.with_suffix('.csv')
json_path = out_prefix.with_suffix('.json')

fields = ['manufacturer','model','api_level','abi','density','install_result','smoke_result','logs']

with open(csv_path, 'w', newline='', encoding='utf-8') as csvfile:
    writer = csv.DictWriter(csvfile, fieldnames=fields)
    writer.writeheader()
    for e in entries:
        row = {k: e.get(k, '') for k in fields}
        writer.writerow(row)

with open(json_path, 'w', encoding='utf-8') as jf:
    json.dump({'summary': {'total': len(entries), 'passed': sum(1 for e in entries if e.get('smoke_result')=='success')}, 'devices': entries}, jf, indent=2)

print(f"Wrote {csv_path} and {json_path}")

