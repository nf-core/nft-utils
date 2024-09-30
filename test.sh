#!/bin/bash
nf-test test --debug --verbose tests/**/*.nf.test --plugins target/nft-utils-*.jar "${@}"
