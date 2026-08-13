#!/bin/bash

mvn package
mvn checkstyle:check

echo "Built $(readlink -f target/nft-utils*.jar)"
