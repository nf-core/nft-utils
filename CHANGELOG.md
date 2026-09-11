# Changelog

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/)
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## Unreleased

### Added

- [#90](https://github.com/nf-core/nft-utils/pull/90) Implement `additionalPatterns` parameter in `filterNextflowOutput` (@maxulysse)

### Changed

- [#90](https://github.com/nf-core/nft-utils/pull/90) Refactor `Methods.java` into separate utility classes (`HashUtils`, `YamlUtils`, `ArchiveDownloader`, `FileTraversalUtils`, `NextflowOutputFilter`) (@maxulysse)
- [#90](https://github.com/nf-core/nft-utils/pull/90) More standardized CHANGELOG (@maxulysse)

### Fixed

- [#90](https://github.com/nf-core/nft-utils/pull/90) `ArchiveDownloader` and `NfCoreUtils.installModule` now throw `IOException` on failure (@maxulysse)
- [#90](https://github.com/nf-core/nft-utils/pull/90) Fix raw types in public API signatures (@maxulysse)

## 1.2.0

### Added

- [#86](https://github.com/nf-core/nft-utils/pull/86) Add `csvMD5Keys` for `sanitizeOutput` to hash normalized CSV content (@LouisLeNezet)

### Fixed

- [#85](https://github.com/nf-core/nft-utils/pull/85) Fix empty value usage in `sanitizeOutput()`. See [#84](https://github.com/nf-core/nft-utils/issues/84) (@LouisLeNezet)

### Changed

- [#88](https://github.com/nf-core/nft-utils/pull/88) Clean up documentation prose, add feature list to README, standardize CHANGELOG (@maxulysse)

## 1.1.1

### Fixed

- [#83](https://github.com/nf-core/nft-utils/pull/83) Use `unstablePatterns` and `ignorePatterns` instead of `unstablePattern` and `ignorePattern` (@LouisLeNezet)

## 1.1.0

### Added

- [#75](https://github.com/nf-core/nft-utils/pull/75) Add `ignoreKeys` for `sanitizeOutput` and key presence check (@LouisLeNezet)
- [#76](https://github.com/nf-core/nft-utils/pull/76) Add `readsMD5Keys` and `variantsMD5Keys` for `sanitizeOutput` to hash reads and variants separately (@LouisLeNezet)
- [#77](https://github.com/nf-core/nft-utils/pull/77) Add `unstablePattern` and `ignorePattern` to `sanitizeOutput` for glob-based filtering (@LouisLeNezet)

### Fixed

- [#74](https://github.com/nf-core/nft-utils/pull/74) Fix `unstableKeys` usage for `sanitizeOutput` when a folder is passed (@LouisLeNezet)
- [#78](https://github.com/nf-core/nft-utils/pull/78) Fix linting with `mvn checkstyle:check` and restructure folders (@LouisLeNezet)
- [#79](https://github.com/nf-core/nft-utils/pull/79) Use glob patterns instead of regex for `ignorePattern` and `unstablePattern` (@LouisLeNezet)
- [#80](https://github.com/nf-core/nft-utils/pull/80) Update GitHub Actions to use environment and cache; add linting and bug checks to Maven lifecycle (@LouisLeNezet)
- [#81](https://github.com/nf-core/nft-utils/pull/81) Fix `nfcoreInitialise()` for nf-core tools 4.1.0 compatibility (requires `conf/` directory) (@JimDownie)

### New Contributors

- @LouisLeNezet

## 1.0.0

### Added

- [#69](https://github.com/nf-core/nft-utils/pull/69) Add `getAllFilesFromPath()` to retrieve files from local or S3 paths with `include`/`ignore` glob patterns, `includeDir`, and `noSignRequest` for S3 (@maxulysse)

## 0.0.9

### Added

- [#61](https://github.com/nf-core/nft-utils/pull/61) `nfcoreInstall()` now tracks installed modules via state files, skipping already installed modules (@JimDownie)
- [#62](https://github.com/nf-core/nft-utils/pull/62) Add `getAllFilesFromChannel()` to extract absolute file paths from Nextflow channel output (@maxulysse)
- [#63](https://github.com/nf-core/nft-utils/pull/63) Add `filterNextflowOutput()` to clean Nextflow stdout/stderr with `include`/`ignore` filters and `keepAnsi` option (@maxulysse)

## 0.0.8

### Added

- [#55](https://github.com/nf-core/nft-utils/pull/55) Add `curlAndExtract()` to download and extract `tar` and `zip` files during nf-test setup (@muffato)

### New Contributors

- @muffato

## 0.0.7

### Added

- [#48](https://github.com/nf-core/nft-utils/pull/48) Add `OutputSanitizer` class with `sanitizeOutput()` to process and clean output channels, supporting `unstableKeys` for unstable file paths (@nvnieuwk)

### Fixed

- [#50](https://github.com/nf-core/nft-utils/pull/50) `nfcoreLibraryLinker`: exit gracefully when directories don't exist (@prototaxites)

## 0.0.6

### Added

- [#45](https://github.com/nf-core/nft-utils/pull/45) Add stdout and stderr helper functions for better snapshots (@maxulysse)

### Fixed

- [#44](https://github.com/nf-core/nft-utils/pull/44) Improve documentation (@maxulysse)

## 0.0.5

### Added

- [#42](https://github.com/nf-core/nft-utils/pull/42) Add functions for managing dependencies on nf-core modules (@prototaxites)

### Fixed

- [#41](https://github.com/nf-core/nft-utils/pull/41) Fix rendering of cloning code blocks (@TCLamnidis)

### New Contributors

- @prototaxites
- @TCLamnidis

## 0.0.4

### Added

- [#26](https://github.com/nf-core/nft-utils/pull/26) Add `listToMD5` (@nvnieuwk)

### Fixed

- [#24](https://github.com/nf-core/nft-utils/pull/24) Improve `getAllFilesInDir()` documentation (@jfy133)
- [#28](https://github.com/nf-core/nft-utils/pull/28) Fix missing comma in documentation code (@Joon-Klaps)
- [#34](https://github.com/nf-core/nft-utils/pull/34) Update usage docs and images (@itrujnara)
- [#35](https://github.com/nf-core/nft-utils/pull/35) Improve `removeFromYamlMap` (@maxulysse)
- [#36](https://github.com/nf-core/nft-utils/pull/36) Fix `listToMD5` (@maxulysse)

### New Contributors

- @jfy133
- @Joon-Klaps
- @itrujnara

## 0.0.3

### Added

- [#18](https://github.com/nf-core/nft-utils/pull/18) Add wrapper functions for `getAllFilesFromDir` with named parameters (@lukfor)
- [#20](https://github.com/nf-core/nft-utils/pull/20) Add `removeFromYamlMap` (@maxulysse)
- [#21](https://github.com/nf-core/nft-utils/pull/21) Add `include` in `getAllFilesFromDir` (@maxulysse)

### Fixed

- [#19](https://github.com/nf-core/nft-utils/pull/19) Move documentation to its own folder (@maxulysse)

### New Contributors

- @lukfor

## 0.0.2

### Added

- [#7](https://github.com/nf-core/nft-utils/pull/7) Add recursive file listing method (@JonathanManning, @maxulysse)
- [#15](https://github.com/nf-core/nft-utils/pull/15) Add Maven to Gitpod install (@nvnieuwk)
- [#16](https://github.com/nf-core/nft-utils/pull/16) Add `getRelativePath()` function (@maxulysse)

### New Contributors

- @JonathanManning
- @nvnieuwk

## 0.0.1

First release of nft-utils 🍏🚀

### Added

- [#1](https://github.com/nf-core/nft-utils/pull/1) Initial repo setup (@adamrtalbot)
- [#6](https://github.com/nf-core/nft-utils/pull/6) Add `removeNextflowVersion` function (@maxulysse)
- [#9](https://github.com/nf-core/nft-utils/pull/9), [#11](https://github.com/nf-core/nft-utils/pull/11) Add documentation (@maxulysse, @adamrtalbot)
- [#12](https://github.com/nf-core/nft-utils/pull/12) Add ignore file (@maxulysse)

### New Contributors

- @adamrtalbot
- @maxulysse
