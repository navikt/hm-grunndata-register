#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080/admreg/api/v1/techlabels}"

if [ "$#" -eq 0 ]; then
  echo "Usage: $0 <isocode> [isocode ...]" >&2
  echo "Example: $0 18090301 18090301" >&2
  echo "Example: $0 18090301,18090302" >&2
  exit 1
fi

isocodes=()
for arg in "$@"; do
  IFS=',' read -r -a parts <<< "$arg"
  for part in "${parts[@]}"; do
    isocode="${part//[[:space:]]/}"
    if [ -n "$isocode" ]; then
      isocodes+=("$isocode")
    fi
  done
done

for isocode in "${isocodes[@]}"; do
  encoded_isocode="$(jq -rn --arg value "$isocode" '$value | @uri')"
  curl -fsS -X GET -H 'Accept: application/json' "${BASE_URL%/}/${encoded_isocode}"
done | jq -s 'add | unique_by(.label)'

