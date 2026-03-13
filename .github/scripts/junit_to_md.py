#!/usr/bin/env python3
"""
Parse JUnit XML files and produce a Markdown report listing each test (module, class, test, status, time, message).
Usage:
  python .github/scripts/junit_to_md.py "**/build/test-results/**/*.xml" output.md

Designed to run on ubuntu-latest in GitHub Actions.
"""
import argparse
import glob
import os
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

MAX_MESSAGE = 500


def find_module_from_path(p: Path) -> str:
    s = str(p).replace('\\', '/')
    if '/build/' in s:
        return s.split('/build/')[0]
    parts = s.split('/')
    if len(parts) >= 2:
        return '/'.join(parts[:2])
    return parts[0] if parts else '.'


def safe_text(s: str) -> str:
    if s is None:
        return ''
    t = ' '.join(s.splitlines())
    if len(t) > MAX_MESSAGE:
        return t[:MAX_MESSAGE] + '...'
    return t


def escape_pipe(s: str) -> str:
    return s.replace('|', '\\|')


def parse_files(pattern: str):
    files = glob.glob(pattern, recursive=True)
    files = sorted(set(files))
    results = []
    for f in files:
        try:
            path = Path(f)
            module = find_module_from_path(path)
            tree = ET.parse(f)
            root = tree.getroot()
            suites = []
            if root.tag == 'testsuites':
                suites = root.findall('testsuite')
            elif root.tag == 'testsuite':
                suites = [root]
            else:
                suites = root.findall('.//testsuite')

            for suite in suites:
                for tc in suite.findall('testcase'):
                    name = tc.get('name', '')
                    classname = tc.get('classname', '')
                    time = tc.get('time', '')
                    status = 'passed'
                    message = ''
                    # failure or error
                    fail = tc.find('failure')
                    err = tc.find('error')
                    skip = tc.find('skipped')
                    if fail is not None:
                        status = 'failed'
                        message = safe_text(fail.text or fail.get('message') or '')
                    elif err is not None:
                        status = 'failed'
                        message = safe_text(err.text or err.get('message') or '')
                    elif skip is not None:
                        status = 'skipped'
                        message = safe_text(skip.text or skip.get('message') or '')

                    results.append({
                        'module': module,
                        'classname': classname,
                        'name': name,
                        'status': status,
                        'time': time,
                        'message': escape_pipe(message),
                    })
        except Exception as e:
            print(f"Warning: failed to parse {f}: {e}", file=sys.stderr)
    return results


def to_markdown(items, out_path: Path):
    total = len(items)
    passed = sum(1 for i in items if i['status'] == 'passed')
    failed = sum(1 for i in items if i['status'] == 'failed')
    skipped = sum(1 for i in items if i['status'] == 'skipped')

    lines = []
    lines.append(f"# Test report\n")
    lines.append(f"**Total:** {total}  |  **Passed:** {passed}  |  **Failed:** {failed}  |  **Skipped:** {skipped}\n")
    lines.append('\n')

    if total == 0:
        lines.append('_No JUnit XML test result files were found for the provided pattern._\n')
        out_path.write_text('\n'.join(lines), encoding='utf-8')
        return

    # Header
    lines.append('| Module | Class | Test | Status | Time(s) | Message |')
    lines.append('| --- | --- | --- | ---: | ---: | --- |')

    # Sort
    items_sorted = sorted(items, key=lambda x: (x['module'], x['classname'], x['name']))
    for it in items_sorted:
        module = it['module'] or ''
        classname = it['classname'] or ''
        name = it['name'] or ''
        status = it['status'] or ''
        time = it['time'] or ''
        message = it['message'] or ''
        # shorten long message
        if len(message) > MAX_MESSAGE:
            message = message[:MAX_MESSAGE] + '...'
        # escape pipes already handled
        lines.append(f"| {module} | {classname} | {name} | {status} | {time} | {message} |")

    out_path.write_text('\n'.join(lines), encoding='utf-8')


def main():
    parser = argparse.ArgumentParser(description='Convert JUnit XML results to a Markdown table')
    parser.add_argument('pattern', help='Glob pattern to find junit xml files (use quotes), e.g. "**/build/test-results/**/*.xml"')
    parser.add_argument('output', help='Output Markdown file path')
    args = parser.parse_args()

    items = parse_files(args.pattern)
    out_path = Path(args.output)
    out_path.parent.mkdir(parents=True, exist_ok=True)
    to_markdown(items, out_path)
    print(f"Wrote report to {out_path} (tests: {len(items)})")


if __name__ == '__main__':
    main()

