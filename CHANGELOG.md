# Changelog

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/)
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## 0.0.9dev

### New features

- `nfcoreInstall()` now tracks installed modules using state files in the library, skipping installation for already installed modules.

## 0.0.8

### New features

- Added the `curlAndExtract` function to download and extract `tar` and `zip` files during the nf-test setup stage.

## 0.0.7

### Added

- Added the OutputSanitizer class with the sanitizeOutput() method to process and clean output channels, supporting options like unstableKeys to handle unstable file outputs by @nvnieuwk

### Fixed

- nfcoreLibraryLinker: exit gracefully when directories don't exist by @prototaxites

## 0.0.6

### Added

- Add stdout and stderr helper function for better snapshot by @maxulysse

### Fixed

- improve docs by @maxulysse

## 0.0.5

### Added

- Add functions for managing dependencies on nf-core modules by @prototaxites

### Fixed

- Fix rendering of cloning code blocks by @TCLamnidis

### New Contributors

- @prototaxites
- @TCLamnidis

## 0.0.4

### Added

- Add listToMD5 by @nvnieuwk in #26

### Fixed

- Improve user eligibility for getAllFilesInDir() docs by @jfy133
- fix missing ',' in docs code by @Joon-Klaps
- fix listToMD5 by @maxulysse
- Improve remove from yaml map by @maxulysse

### New Contributors

- @jfy133
- @Joon-Klaps
- @itrujnara

## 0.0.3

### Added

- Add wrapper functions for getAllFilesFromDir with named parameters by @lukfor
- add include in getAllFilesFromDir by @maxulysse
- add removeFromYamlMap by @maxulysse

### Fixed

- Move all docs in its own folder by @maxulysse

### New Contributors

- @lukfor

## 0.0.2

### Added

- Add maven to gitpod install by @nvnieuwk
- Add getRelativePath() function by @maxulysse

### New Contributors

- @nvnieuwk

## 0.0.1

First release of nft-utils 🍏🚀

### Added

- add removeNextflowVersion function by @maxulysse
- Add getAllFilesFromDir function by @maxulysse
- Add docs by @maxulysse

### New Contributors

- @adamrtalbot
- @maxulysse
