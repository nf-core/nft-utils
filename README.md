# nft-utils

nf-test plugin with utility functions for pipeline-level snapshot testing.

## Features

- `sanitizeOutput()` - clean process outputs for stable snapshots
- `filterNextflowOutput()` - remove variable content from stdout/stderr
- `getAllFilesFromPath()` - list files from local or S3 paths
- `removeNextflowVersion()` - strip Nextflow version from YAML files
- `nfcoreInstall()` - install nf-core modules for testing
- `curlAndExtract()` - download and extract archives in test setup

Full documentation at [nf-co.re/nft-utils](https://nf-co.re/nft-utils).

## Development

Requires Nextflow, nf-test, and Maven.

```bash
# Install conda environment
mamba env create -f environment.yml
mamba activate env_nft_utils

# Compile the package
mvn -B clean verify

# Run all tests
nf-test test --plugins target/nft-utils-*.jar --verbose --debug tests/

# Run a single test and update snapshots
nf-test test --plugins target/nft-utils-*.jar --update-snapshot tests/sanitizeOutput/

# Test with no plugins installed
nf-test test --plugins target/nft-utils-*.jar --config tests_noplugins/nf-test_noplugins.config tests_noplugins/sanitizeOutput/
```

## Credits

nft-utils was created by the nf-core community.

- [Adam Talbot](https://github.com/adamrtalbot)
- [Edmund Miller](https://github.com/edmundmiller)
- [Igor Trujnara](https://github.com/itrujnara)
- [James A. Fellows Yates](https://github.com/jfy133)
- [Jim Downie](https://github.com/prototaxites)
- [Jonathan Manning](https://github.com/pinin4fjords)
- [Joon Klaps](https://github.com/Joon-Klaps)
- [Louis Le Nezet](https://github.com/LouisLeNezet)
- [Lukas Forer](https://github.com/lukfor)
- [Matthias Zepper](https://github.com/MatthiasZepper)
- [Matthieu Muffato](https://github.com/muffato)
- [Maxime U. Garcia](https://github.com/maxulysse)
- [Nicolas Vannieuwkerke](https://github.com/nvnieuwk)
- [Sateesh Peri](https://github.com/sateeshperi)
- [Thiseas C. Lamnidis](https://github.com/TCLamnidis)
