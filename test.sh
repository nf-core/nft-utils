#!/bin/bash

if [ $# -eq 0 ] || [[ ! "$*" == *"tests/"* ]]; then
    nf-test test --plugins target/nft-utils-*.jar --verbose --debug tests/
    nf-test test --plugins target/nft-utils-*.jar \
      --config tests_noplugins/nf-test_noplugins.config tests_noplugins/sanitizeOutput/
else
    nf-test test --plugins target/nft-utils-*.jar --verbose --debug ${@}
fi
