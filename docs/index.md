# nft-utils

nft-utils is an nf-test plugin to provide additional functions and assertions that fall outside of the typical nf-test features.
They were primarily developed by the nf-core community but should be applicable to any nf-tests.

## Start using the plugin

To start using the plugin please add it to your `nf-test.config` file:

```groovy title="nf-test.config"
config {
    plugins {
        load "nft-utils@0.0.8"
    }
}
```

Have a look at the [usage documentation](./usage.md) for more information on how to start working with the plugin.

## Use a development version

To use the development version, please do the following steps:

- Clone the [nft-utils repository](https://github.com/nf-core/nft-utils)

### SSH

```bash
git clone git@github.com:nf-core/nft-utils.git
```

### HTTPS

```bash
git clone https://github.com/nf-core/nft-utils.git
```

- Run the build script

```bash
./build.sh
```

- Add the jar location (visible at the end of the build script output) to the `nf-test.config` file

```groovy title="nf-test.config"
config {
    plugins {
        loadFromFile "full/path/to/the/plugin/jar"
    }
}
```
