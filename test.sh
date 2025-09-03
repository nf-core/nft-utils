#!/bin/bash

if [ $# -eq 0 ] || [[ ! "$*" == *"tests/"* ]]; then
    nf-test test --plugins target/nft-utils-*.jar tests/
else
    nf-test test --plugins target/nft-utils-*.jar ${@}
fi
