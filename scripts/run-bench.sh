#!/usr/bin/env bash
# Run DB I/O benchmarks in two passes to compare JDBC batch_size effect.
#
# Pass 1 (batch_size=20): R1 (Board index) + R2 (Comment index) + U1 (batch=20)
# Pass 2 (batch_size=1):  U1 (batch=1) only — R1/R2 do not depend on batch_size.
#
# CSV is appended across runs. Delete the file to start fresh.
set -euo pipefail

cd "$(dirname "$0")/.."
CSV="docs/benchmark/results/2026-08-05_db_io.csv"

if [[ "${1:-}" == "--fresh" ]]; then
  echo "[bench] removing existing CSV: $CSV"
  rm -f "$CSV"
fi

echo "==========================================================="
echo "[bench] PASS 1: batch_size=20  (R1, R2, U1)"
echo "==========================================================="
./gradlew --no-daemon bench \
  -Dbench.batch.size=20 \
  -Dbench.run.scenarios=R1,R2,U1

echo
echo "==========================================================="
echo "[bench] PASS 2: batch_size=1   (U1 only)"
echo "==========================================================="
./gradlew --no-daemon bench \
  -Dbench.batch.size=1 \
  -Dbench.run.scenarios=U1

echo
echo "[bench] done. Result CSV:"
cat "$CSV"
