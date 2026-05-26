#!/usr/bin/env bash
# Used by .github/workflows/post-welcome-discussion.yml (needs repo Actions: Read and write).
set -eu

TITLE='Добро пожаловать в Saylat — с чего начать'

exists="$(gh api "repos/Baddysays/Saylat/discussions" --paginate \
  --jq ".[] | select(.title==\"$TITLE\") | .number" 2>/dev/null || true)"
if [ -n "$exists" ]; then
  echo "Welcome discussion already exists: #$exists"
  exit 0
fi

BODY="$(sed '1,2d' docs/github/DISCUSSIONS-WELCOME.md | sed '/^<!--/d' | sed '/^-->$/d')"

CAT_ID="$(gh api repos/Baddysays/Saylat/discussions/categories \
  --jq '.[] | select(.slug=="general") | .id' | head -n1)"
if [ -z "$CAT_ID" ]; then
  CAT_ID="$(gh api repos/Baddysays/Saylat/discussions/categories --jq '.[0].id')"
fi

echo "Using category_id=$CAT_ID"

payload="$(jq -n --arg title "$TITLE" --arg body "$BODY" --argjson category_id "$CAT_ID" \
  '{title: $title, body: $body, category_id: $category_id}')"
resp="$(gh api --method POST repos/Baddysays/Saylat/discussions --input - <<<"$payload")"

url="$(echo "$resp" | jq -r '.html_url')"
echo "Created: $url"

NODE_ID="$(echo "$resp" | jq -r '.node_id')"
if gh api graphql \
  -f query='mutation($id: ID!) { pinDiscussion(input: {discussionId: $id}) { clientMutationId } }' \
  -f id="$NODE_ID" 2>/dev/null; then
  echo "Pinned."
else
  echo "Pin skipped — open $url and Pin discussion manually."
fi
