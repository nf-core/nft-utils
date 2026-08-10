# nft-utils

This repository contains utility functions for nf-test.
These functions are used to help capture level tests using nf-test.

Please read the [documentation](./docs) for more information.

## Development

The `nft-utils` plugin needs Nextflow and nf-test.
It use maven for compilation and nf-core for testing.
A conda environment is available and can be used for development

```bash
# Install conda environment
mamba env create -f environment.yml
mamba activate env_nft_utils

# Compile the package
mvn package

# Run all the unittest
nf-test test --plugins target/nft-utils-*.jar --verbose --debug tests/
# or a single one and update the snapshot
nf-test test --plugins target/nft-utils-*.jar --update-snapshot tests/sanitizeOutput/
```

## Credits

nft-utils was created by the nf-core community.

We'd like to thank the following people:

- [Adam Talbot](https://github.com/adamrtalbot)
- [Edmund Miller](https://github.com/edmundmiller)
- [Jim Downie](https://github.com/prototaxites)
- [Jonathan Manning](https://github.com/pinin4fjords)
- [Lukas Forer](https://github.com/lukfor)
- [Matthias Zepper](https://github.com/MatthiasZepper)
- [Matthieu Muffato](https://github.com/muffato)
- [Maxime U. Garcia](https://github.com/maxulysse)
- [Nicolas Vannieuwkerke](https://github.com/nvnieuwk)
- [Sateesh Peri](https://github.com/sateeshperi)
