#!/usr/bin/env bash
set -euo pipefail

mvn -B clean verify

echo "Built $(readlink -f target/nft-utils*.jar)"
