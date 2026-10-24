#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/../.."
if ./mvnw verify -Dgroups=Layer1; then
  echo "✅ Layer 1 integration tests PASSED"
else
  test_exit=$?
  echo "❌ Layer 1 integration tests FAILED"
  exit "$test_exit"
fi
