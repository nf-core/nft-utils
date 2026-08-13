#!/usr/bin/env bash
set -euo pipefail

PLUGIN_JAR=$(find target -maxdepth 1 -name 'nft-utils-*.jar' -type f -print -quit)

[[ -n "$PLUGIN_JAR" ]] || {
    echo "ERROR: Plugin JAR not found in target/" >&2
    exit 1
}

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
