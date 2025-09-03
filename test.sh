#!/bin/bash

if [ $# -eq 0 ] || [[ ! "$*" == *"tests/"* ]]; then
    nf-test test --debug --verbose --plugins target/nft-utils-*.jar tests/
else
    nf-test test --debug --verbose --plugins target/nft-utils-*.jar ${@}
fi
