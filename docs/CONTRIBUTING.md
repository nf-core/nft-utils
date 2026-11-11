# nf-core/nft-utils: Contributing guidelines

nft-utils is an nf-test plugin: Java in `src/main/java`, unit tests in `src/test/java`, nf-tests in `tests/` and `tests_noplugins/`. `Methods.java` is the public API.

## Commands

```bash
mise trust
mise install                 # the pinned Java, nf-test, nf-core and Nextflow
mise run assemble            # lint, test and package target/nft-utils-<version>.jar
mise run test                # unit tests
mise run validate            # nf-tests; takes paths and nf-test options
mise run validate-noplugins  # nf-tests without nft-bam and nft-vcf
mise run clean
mise run bump <version>      # set the version in pom.xml and docs/index.md
```

## Conventions

- The version lives in `pom.xml`. `moduleVersion` is filled in at build time.
- After changing tools in `mise.toml`, run `mise lock` and commit `mise.lock` and `.mise/locks/`: `locked = true` rejects anything not locked.
- After changing the Maven version, update `distributionSha256Sum` in `.mvn/wrapper/maven-wrapper.properties`.
- CI sets `NXF_VER` from its matrix: the Nextflow launcher installed by mise then downloads that version.
- Add user-facing changes to `CHANGELOG.md` under `[Unreleased]`.
- `[env]` in `mise.toml` holds the settings that change snapshots, and unsets the variables that turn on Nextflow agent mode: run nf-tests through `mise run`, locally and in CI.
- `mise run assemble` runs checkstyle, PMD and SpotBugs. Fix all violations before pushing.
- Each utility function has its nf-test in `tests/<functionName>/main.nf.test`.
