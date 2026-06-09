#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
FRONTEND_DIR="$ROOT_DIR/../frontend"
BACKEND_RESOURCES="$ROOT_DIR/../backend/src/main/resources"

if [[ ! -d "$FRONTEND_DIR" ]]; then
  echo "ERROR: frontend directory not found: $FRONTEND_DIR"
  exit 1
fi

mkdir -p "$BACKEND_RESOURCES/static"
mkdir -p "$BACKEND_RESOURCES/templates"

if command -v rsync >/dev/null 2>&1; then
  rsync -av --delete "$FRONTEND_DIR/static/" "$BACKEND_RESOURCES/static/"
  rsync -av --delete "$FRONTEND_DIR/templates/" "$BACKEND_RESOURCES/templates/"
else
  echo "WARNING: rsync not found, using cp fallback. Install rsync for faster sync."
  rm -rf "$BACKEND_RESOURCES/static"/*
  rm -rf "$BACKEND_RESOURCES/templates"/*
  cp -a "$FRONTEND_DIR/static/." "$BACKEND_RESOURCES/static/"
  cp -a "$FRONTEND_DIR/templates/." "$BACKEND_RESOURCES/templates/"
fi

echo "Frontend assets synced to backend resources."
