#!/usr/bin/env bash
set -euo pipefail

mvn -B package
mvn -B checkstyle:check

echo "Built $(readlink -f target/nft-utils*.jar)"
