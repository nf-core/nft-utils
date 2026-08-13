#!/usr/bin/env bash
set -euo pipefail

if [[ $# -ne 1 ]]; then
    echo "Usage: $0 <version>" >&2
    exit 1
fi

version="$1"
if [[ -z "$version" ]]; then
    echo "ERROR: Version cannot be empty" >&2
    exit 1
fi

if [[ ! "$version" =~ ^[0-9]+\.[0-9]+\.[0-9]+(-[0-9A-Za-z.-]+)?$ ]]; then
    echo "ERROR: Invalid version: $version" >&2
    exit 1
fi

echo "Bumping version to $version"

sed -i -e "0,/<version>/{s/<version>.*<\\/version>/<version>$version<\\/version>/}" \
  ./pom.xml

sed -i -e "s/moduleVersion=.*/moduleVersion=$version/" \
  ./src/main/resources/META-INF/nf-test-plugin

if [[ "$version" != *dev ]]; then
  sed -i -e "s/load \"nft-utils@.*\"/load \"nft-utils@$version\"/" \
    ./docs/index.md
fi

./build.sh
