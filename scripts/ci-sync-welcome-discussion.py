#!/usr/bin/env python3
"""Sync welcome GitHub Discussion from docs/github/DISCUSSIONS-WELCOME.md.

GITHUB_TOKEN can create discussions but not updateDiscussion (FORBIDDEN).
Optional: set repository secret DISCUSSION_PAT (classic PAT with repo scope)
to allow updating discussion #1 when the welcome doc changes.
"""
from __future__ import annotations

import json
import os
import re
import subprocess
import sys
from pathlib import Path

OWNER = "Baddysays"
REPO = "Saylat"
REPO_ID = "R_kgDOSnrE7Q"
SCREENSHOT_MARKER = "home-speed-modes"


def run(cmd: list[str], *, input_text: str | None = None) -> subprocess.CompletedProcess[str]:
    return subprocess.run(
        cmd,
        input=input_text,
        text=True,
        capture_output=True,
        check=False,
    )


def gh_api(*path: str, method: str | None = None, fields: dict | None = None) -> dict:
    cmd = ["gh", "api"]
    if method:
        cmd.extend(["--method", method])
    cmd.append("/".join(path))
    if fields:
        for k, v in fields.items():
            cmd.extend(["-f", f"{k}={v}"])
    proc = run(cmd)
    if proc.returncode != 0:
        raise RuntimeError(proc.stderr.strip() or proc.stdout.strip() or "gh api failed")
    return json.loads(proc.stdout) if proc.stdout.strip() else {}


def gh_graphql_input(payload: dict, *, token: str | None = None) -> dict:
    env = os.environ.copy()
    if token:
        env["GH_TOKEN"] = token
    proc = subprocess.run(
        ["gh", "api", "graphql", "--input", "-"],
        input=json.dumps(payload, ensure_ascii=False),
        text=True,
        capture_output=True,
        env=env,
        check=False,
    )
    if proc.returncode != 0:
        raise RuntimeError(proc.stderr.strip() or proc.stdout.strip() or "graphql failed")
    data = json.loads(proc.stdout)
    if data.get("errors"):
        raise RuntimeError(json.dumps(data["errors"], ensure_ascii=False))
    return data


def gh_graphql(query: str) -> dict:
    return gh_graphql_input({"query": query})


def load_welcome() -> tuple[str, str, str]:
    raw = Path("docs/github/DISCUSSIONS-WELCOME.md").read_text(encoding="utf-8")
    title_m = re.search(r"<!-- TITLE: (.+?) -->", raw)
    cat_m = re.search(r"<!-- CATEGORY: (.+?) -->", raw)
    title = title_m.group(1).strip() if title_m else "Welcome"
    category = cat_m.group(1).strip() if cat_m else "general"
    body = re.sub(r"<!--.*?-->\s*", "", raw, flags=re.S).strip()
    return title, body, category


def find_discussion_by_title(title: str) -> dict | None:
    proc = run(
        [
            "gh",
            "api",
            f"repos/{OWNER}/{REPO}/discussions",
            "--paginate",
            "--jq",
            f'.[] | select(.title=={json.dumps(title)})',
        ]
    )
    if proc.returncode != 0:
        raise RuntimeError(proc.stderr.strip() or "failed to list discussions")
    for line in proc.stdout.splitlines():
        line = line.strip()
        if line:
            return json.loads(line)
    return None


def update_with_pat(discussion_id: str, title: str, body: str) -> None:
    pat = os.environ.get("DISCUSSION_PAT", "").strip()
    if not pat:
        return
    payload = {
        "query": (
            "mutation($id: ID!, $title: String!, $body: String!) {"
            " updateDiscussion(input: {discussionId: $id, title: $title, body: $body}) {"
            " discussion { url } } }"
        ),
        "variables": {"id": discussion_id, "title": title, "body": body},
    }
    gh_graphql_input(payload, token=pat)
    print("Updated welcome discussion with DISCUSSION_PAT.")


def create_discussion(title: str, body: str, category_slug: str) -> str:
    cats = gh_graphql(
        "query { repository(owner: \"Baddysays\", name: \"Saylat\") {"
        " discussionCategories(first: 20) { nodes { id slug } } } }"
    )
    nodes = cats["data"]["repository"]["discussionCategories"]["nodes"]
    cat_id = next(
        (n["id"] for n in nodes if n["slug"] == category_slug),
        next((n["id"] for n in nodes if n["slug"] == "general"), nodes[0]["id"]),
    )

    result = gh_graphql_input(
        {
            "query": (
                "mutation($repo: ID!, $cat: ID!, $title: String!, $body: String!) {"
                " createDiscussion(input: {repositoryId: $repo, categoryId: $cat, title: $title, body: $body}) {"
                " discussion { url id } } }"
            ),
            "variables": {
                "repo": REPO_ID,
                "cat": cat_id,
                "title": title,
                "body": body,
            },
        }
    )
    disc = result["data"]["createDiscussion"]["discussion"]
    url = disc["url"]
    disc_id = disc["id"]
    print(f"Created welcome discussion: {url}")
    try:
        gh_graphql_input(
            {
                "query": (
                    "mutation($id: ID!) {"
                    " pinDiscussion(input: {discussionId: $id}) { clientMutationId } }"
                ),
                "variables": {"id": disc_id},
            }
        )
        print("Pinned.")
    except RuntimeError:
        print(f"Pin manually: {url}")
    return url


def main() -> int:
    title, body, category = load_welcome()
    existing = find_discussion_by_title(title)

    if existing:
        url = existing["html_url"]
        number = existing.get("number", "?")
        disc_body = existing.get("body") or ""
        node_id = existing.get("node_id") or ""

        if SCREENSHOT_MARKER in disc_body:
            print(f"Welcome discussion #{number} is up to date: {url}")
            return 0

        if os.environ.get("DISCUSSION_PAT", "").strip() and node_id:
            update_with_pat(node_id, title, body)
            print(url)
            return 0

        print(
            f"::warning title=Welcome discussion needs a manual edit::"
            f"Discussion #{number} has no screenshots. "
            f"GITHUB_TOKEN cannot update discussions. "
            f"Edit {url} and paste from docs/github/DISCUSSIONS-WELCOME.md "
            f"(or delete it and re-run this workflow to recreate)."
        )
        return 0

    create_discussion(title, body, category)
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except RuntimeError as exc:
        print(f"::error::{exc}", file=sys.stderr)
        raise SystemExit(1) from exc
