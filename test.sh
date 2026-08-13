#!/usr/bin/env bash
set -euo pipefail

PLUGIN="target/nft-utils-*.jar"

if [[ $# -eq 0 ]]; then
    nf-test test \
        --plugins "$PLUGIN" \
        --verbose \
        --debug \
        tests/

    nf-test test \
        --plugins "$PLUGIN" \
        --config tests_noplugins/nf-test_noplugins.config \
        tests_noplugins/sanitizeOutput/
else
    nf-test test \
        --plugins "$PLUGIN" \
        --verbose \
        --debug \
        "$@"
fi
