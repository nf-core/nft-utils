#!/bin/bash

if [ $# -eq 0 ] || [[ ! "$*" == *"tests/"* ]]; then
    nf-test test --plugins target/nft-utils-*.jar --verbose --debug tests/ --update-snapshot --clean-snapshot
else
    nf-test test --plugins target/nft-utils-*.jar --verbose --debug ${@} --update-snapshot --clean-snapshot
fi
