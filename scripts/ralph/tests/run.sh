#!/bin/bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

for test_script in "$SCRIPT_DIR"/*_test.sh; do
  bash "$test_script"
done
