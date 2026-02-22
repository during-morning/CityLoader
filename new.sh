#!/usr/bin/env bash
set -euo pipefail

REPO_SLUG="${REPO_SLUG:-during-morning/CityLoader}"
DEFAULT_REMOTE_SSH="git@github.com:${REPO_SLUG}.git"
DEFAULT_REMOTE_HTTPS="https://github.com/${REPO_SLUG}.git"

TAG="${1:-v$(date +%Y.%m.%d-%H%M%S)}"
COMMIT_MSG="${2:-release: ${TAG}}"
RELEASE_TITLE="${RELEASE_TITLE:-${TAG}}"
RELEASE_NOTES="${RELEASE_NOTES:-Auto release for ${TAG}}"

if [[ ! -f "pom.xml" ]]; then
  echo "Error: run this script in CityLoader project root (pom.xml not found)."
  exit 1
fi

echo "[1/7] Building project with Maven..."
mvn clean package -DskipTests

ARTIFACT="$(ls -t target/*.jar 2>/dev/null | grep -v '/original-' | head -n 1 || true)"
if [[ -z "${ARTIFACT}" ]]; then
  echo "Error: no JAR artifact found in target/."
  exit 1
fi
echo "Artifact: ${ARTIFACT}"

if [[ ! -d ".git" ]]; then
  echo "[2/7] Initializing Git repository..."
  git init -b main
fi

echo "[3/7] Ensuring remote origin..."
if git remote get-url origin >/dev/null 2>&1; then
  echo "origin -> $(git remote get-url origin)"
else
  if git ls-remote "${DEFAULT_REMOTE_SSH}" >/dev/null 2>&1; then
    git remote add origin "${DEFAULT_REMOTE_SSH}"
  else
    git remote add origin "${DEFAULT_REMOTE_HTTPS}"
  fi
fi

echo "[4/7] Committing changes..."
git add -A
if git diff --cached --quiet; then
  echo "No staged changes to commit."
else
  git commit -m "${COMMIT_MSG}"
fi

echo "[5/7] Creating tag ${TAG}..."
if git rev-parse "${TAG}" >/dev/null 2>&1; then
  echo "Tag ${TAG} already exists locally."
else
  git tag -a "${TAG}" -m "${RELEASE_TITLE}"
fi

echo "[6/7] Pushing main branch and tags..."
git push -u origin main
git push origin "${TAG}"

echo "[7/7] Creating GitHub release..."
if command -v gh >/dev/null 2>&1; then
  if gh release view "${TAG}" --repo "${REPO_SLUG}" >/dev/null 2>&1; then
    echo "Release ${TAG} already exists. Uploading artifact..."
    gh release upload "${TAG}" "${ARTIFACT}" --repo "${REPO_SLUG}" --clobber
  else
    gh release create "${TAG}" "${ARTIFACT}" \
      --repo "${REPO_SLUG}" \
      --title "${RELEASE_TITLE}" \
      --notes "${RELEASE_NOTES}"
  fi
elif [[ -n "${GITHUB_TOKEN:-}" ]]; then
  API_URL="https://api.github.com/repos/${REPO_SLUG}/releases"
  RELEASE_ID="$(curl -sS -X POST \
    -H "Authorization: Bearer ${GITHUB_TOKEN}" \
    -H "Accept: application/vnd.github+json" \
    "${API_URL}" \
    -d "{\"tag_name\":\"${TAG}\",\"name\":\"${RELEASE_TITLE}\",\"body\":\"${RELEASE_NOTES}\"}" | sed -n 's/.*"id":[[:space:]]*\([0-9][0-9]*\).*/\1/p' | head -n1)"

  if [[ -z "${RELEASE_ID}" ]]; then
    echo "Error: failed to create release via API."
    exit 1
  fi

  ASSET_NAME="$(basename "${ARTIFACT}")"
  curl -sS -X POST \
    -H "Authorization: Bearer ${GITHUB_TOKEN}" \
    -H "Content-Type: application/octet-stream" \
    --data-binary @"${ARTIFACT}" \
    "https://uploads.github.com/repos/${REPO_SLUG}/releases/${RELEASE_ID}/assets?name=${ASSET_NAME}" >/dev/null
else
  echo "Error: cannot create release."
  echo "Install GitHub CLI (gh) or set GITHUB_TOKEN, then re-run ./new.sh"
  exit 1
fi

echo "Done: pushed and released ${TAG}"
