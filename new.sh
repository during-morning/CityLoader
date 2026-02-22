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
upload_with_api() {
  local auth_mode="$1"
  local auth_value="$2"
  local api_base="https://api.github.com/repos/${REPO_SLUG}/releases"
  local headers=(-H "Accept: application/vnd.github+json")
  local curl_auth=()
  local release_code
  local create_code
  local release_json
  local create_json
  local upload_url
  local asset_name
  local asset_code
  local asset_id

  if [[ "${auth_mode}" == "bearer" ]]; then
    headers+=(-H "Authorization: Bearer ${auth_value}")
  else
    curl_auth=(-u "${auth_value}")
  fi

  release_json="$(mktemp)"
  release_code="$(curl -sS -o "${release_json}" -w '%{http_code}' "${curl_auth[@]}" "${headers[@]}" \
    "${api_base}/tags/${TAG}")"

  if [[ "${release_code}" != "200" ]]; then
    create_json="$(mktemp)"
    create_code="$(curl -sS -o "${create_json}" -w '%{http_code}' "${curl_auth[@]}" "${headers[@]}" \
      -H "Content-Type: application/json" \
      -d "{\"tag_name\":\"${TAG}\",\"target_commitish\":\"main\",\"name\":\"${RELEASE_TITLE}\",\"body\":\"${RELEASE_NOTES}\"}" \
      "${api_base}")"
    if [[ "${create_code}" != "201" && "${create_code}" != "200" ]]; then
      echo "Error: release create failed (HTTP ${create_code})."
      cat "${create_json}"
      return 1
    fi
    upload_url="$(jq -r '.upload_url' "${create_json}" | sed 's/{?name,label}//')"
    release_json="${create_json}"
  else
    upload_url="$(jq -r '.upload_url' "${release_json}" | sed 's/{?name,label}//')"
  fi

  if [[ -z "${upload_url}" || "${upload_url}" == "null" ]]; then
    echo "Error: failed to resolve release upload_url."
    cat "${release_json}"
    return 1
  fi

  asset_name="$(basename "${ARTIFACT}")"
  asset_code="$(curl -sS -o /tmp/cityloader_release_asset.json -w '%{http_code}' \
    "${curl_auth[@]}" "${headers[@]}" \
    -H "Content-Type: application/java-archive" \
    --data-binary @"${ARTIFACT}" \
    "${upload_url}?name=${asset_name}")"

  if [[ "${asset_code}" == "422" ]]; then
    asset_id="$(jq -r ".assets[] | select(.name==\"${asset_name}\") | .id" "${release_json}" | head -n1)"
    if [[ -n "${asset_id}" ]]; then
      curl -sS -X DELETE "${curl_auth[@]}" "${headers[@]}" \
        "${api_base}/assets/${asset_id}" >/dev/null
      asset_code="$(curl -sS -o /tmp/cityloader_release_asset.json -w '%{http_code}' \
        "${curl_auth[@]}" "${headers[@]}" \
        -H "Content-Type: application/java-archive" \
        --data-binary @"${ARTIFACT}" \
        "${upload_url}?name=${asset_name}")"
    fi
  fi

  if [[ "${asset_code}" != "201" && "${asset_code}" != "200" ]]; then
    echo "Error: asset upload failed (HTTP ${asset_code})."
    cat /tmp/cityloader_release_asset.json
    return 1
  fi
}

if ! command -v jq >/dev/null 2>&1; then
  echo "Error: jq is required for API-based release creation."
  exit 1
fi

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
  upload_with_api "bearer" "${GITHUB_TOKEN}"
elif creds="$(printf 'protocol=https\nhost=github.com\n\n' | git credential fill 2>/dev/null)" && \
  [[ -n "$(printf '%s\n' "${creds}" | sed -n 's/^password=//p' | head -n1)" ]]; then
  GH_USER="$(printf '%s\n' "${creds}" | sed -n 's/^username=//p' | head -n1)"
  GH_PASS="$(printf '%s\n' "${creds}" | sed -n 's/^password=//p' | head -n1)"
  upload_with_api "basic" "${GH_USER}:${GH_PASS}"
else
  echo "Error: cannot create release."
  echo "Install GitHub CLI (gh), set GITHUB_TOKEN, or configure git credential helper for github.com."
  exit 1
fi

echo "Done: pushed and released ${TAG}"
