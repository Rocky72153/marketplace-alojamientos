#!/usr/bin/env bash
set -euo pipefail

# Requiere GitHub CLI (gh), sesión autenticada con permisos administrativos y que main/develop existan.
# Ejecutar desde la raíz del repositorio: ./scripts/configure-github-branch-protection.sh owner/repo
REPO="${1:-${GITHUB_REPOSITORY:-}}"
if [[ -z "$REPO" || "$REPO" != */* ]]; then
  echo "Uso: $0 owner/repo" >&2
  exit 2
fi
if ! gh auth status >/dev/null 2>&1; then
  echo "Ejecuta primero: gh auth login" >&2
  exit 2
fi

TMP="$(mktemp)"
trap 'rm -f "$TMP"' EXIT
cat > "$TMP" <<'JSON'
{
  "required_status_checks": {
    "strict": true,
    "contexts": ["Backend CI", "Frontend CI", "Documentation CI"]
  },
  "enforce_admins": true,
  "required_pull_request_reviews": {
    "dismiss_stale_reviews": true,
    "require_code_owner_reviews": false,
    "required_approving_review_count": 1
  },
  "restrictions": null,
  "required_linear_history": true,
  "allow_force_pushes": false,
  "allow_deletions": false,
  "block_creations": false,
  "required_conversation_resolution": true,
  "lock_branch": false,
  "allow_fork_syncing": false
}
JSON

for BRANCH in main develop; do
  echo "Aplicando protección a $REPO:$BRANCH"
  gh api --method PUT "repos/$REPO/branches/$BRANCH/protection" \
    -H "Accept: application/vnd.github+json" \
    -H "X-GitHub-Api-Version: 2022-11-28" \
    --input "$TMP" >/dev/null
  echo "Protección configurada: $BRANCH"
done

echo "Protección base aplicada. Confirma los nombres de los checks y añade SonarCloud como obligatorio solo después de configurar token/variables y ver un análisis exitoso."
