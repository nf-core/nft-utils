# Changelog

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/)
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## 1.2.0

### Fixed

- Fix empty value usage in `sanitizeOutput()`. See [#84](https://github.com/nf-core/nft-utils/issues/84)

### Added

- Add `csvMD5Keys` for `sanitizeOutput` to hash normalized CSV content

### Changed

- Improve documentation: clean up prose, add feature list to README, standardize CHANGELOG categories

## [1.1.1]

### Fixed

- Use `unstablePatterns` and `ignorePatterns` instead of `unstablePattern` and `ignorePattern`

## [1.1.0]

### Added

- Add `ignoreKeys` for `sanitizeOutput` and key presence check
- Add `readsMD5Keys` and `variantsMD5Keys` for `sanitizeOutput` to hash reads and variants separately
- Add `unstablePattern` and `ignorePattern` to `sanitizeOutput` for glob-based filtering

### Fixed

- Fix `unstableKeys` usage for `sanitizeOutput` when a folder is passed
- Fix linting with `mvn checkstyle:check` and restructure folders
- Use glob patterns instead of regex for `ignorePattern` and `unstablePattern`
- Update GitHub Actions to use environment and cache; add linting and bug checks to Maven lifecycle
- Fix `nfcoreInitialise()` for nf-core tools 4.1.0 compatibility (requires `conf/` directory)

## [1.0.0]

### Added

- Add `getAllFilesFromPath()` to retrieve files from local or S3 paths with `include`/`ignore` glob patterns, `includeDir`, and `noSignRequest` for S3

## [0.0.9]

### Added

- `nfcoreInstall()` now tracks installed modules via state files, skipping already installed modules
- Add `filterNextflowOutput()` to clean Nextflow stdout/stderr with `include`/`ignore` filters and `keepAnsi` option

## [0.0.8]

### Added

- Add `curlAndExtract()` to download and extract `tar` and `zip` files during nf-test setup

## [0.0.7]

### Added

- Add `OutputSanitizer` class with `sanitizeOutput()` to process and clean output channels, supporting `unstableKeys` for unstable file outputs (@nvnieuwk)

### Fixed

- `nfcoreLibraryLinker`: exit gracefully when directories don't exist (@prototaxites)

## [0.0.6]

### Added

- Add stdout and stderr helper functions for better snapshots (@maxulysse)

### Fixed

- Improve documentation (@maxulysse)

## [0.0.5]

### Added

- Add functions for managing dependencies on nf-core modules (@prototaxites)

### Fixed

- Fix rendering of cloning code blocks (@TCLamnidis)

### New Contributors

- @prototaxites
- @TCLamnidis

## [0.0.4]

### Added

- Add `listToMD5` (@nvnieuwk in #26)

### Fixed

- Improve `getAllFilesInDir()` documentation (@jfy133)
- Fix missing comma in documentation code (@Joon-Klaps)
- Fix `listToMD5` (@maxulysse)
- Improve `removeFromYamlMap` (@maxulysse)

### New Contributors

- @jfy133
- @Joon-Klaps
- @itrujnara

## [0.0.3]

### Added

- Add wrapper functions for `getAllFilesFromDir` with named parameters (@lukfor)
- Add `include` in `getAllFilesFromDir` (@maxulysse)
- Add `removeFromYamlMap` (@maxulysse)

### Fixed

- Move documentation to its own folder (@maxulysse)

### New Contributors

- @lukfor

## [0.0.2]

### Added

- Add Maven to Gitpod install (@nvnieuwk)
- Add `getRelativePath()` function (@maxulysse)

### New Contributors

- @nvnieuwk

## [0.0.1]

First release of nft-utils.

### Added

- Add `removeNextflowVersion` function (@maxulysse)
- Add `getAllFilesFromDir` function (@maxulysse)
- Add documentation (@maxulysse)

### New Contributors

- @adamrtalbot
- @maxulysse
