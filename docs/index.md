# nft-utils

nf-test plugin with utility functions for pipeline-level snapshot testing. Works with any nf-test project, not just nf-core pipelines.

## Installation

Add the plugin to your `nf-test.config`:

```groovy title="nf-test.config"
config {
    plugins {
        load "nft-utils@1.2.0"
    }
}
```

See the [usage documentation](./usage.md) for available functions.

## Development version

Clone the repository:

```bash
git clone git@github.com:nf-core/nft-utils.git
```

Build the plugin:

```bash
./build.sh
```

Add the jar to your `nf-test.config`:

```groovy title="nf-test.config"
config {
    plugins {
        loadFromFile "full/path/to/the/plugin/jar"
    }
}
```
