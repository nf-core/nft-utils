# Agent guide

nft-utils is an nf-test plugin: Java in `src/main/java`, unit tests in `src/test/java`, nf-tests in `tests/` and `tests_noplugins/`. `Methods.java` is the public API.

## Commands

```bash
mise trust
mise install --locked        # the pinned Java, nf-test, nf-core and Nextflow
mise run assemble            # lint and package target/nft-utils-<version>.jar
mise run test                # unit tests
mise run validate            # nf-tests; takes paths and nf-test options
mise run validate-noplugins  # nf-tests without nft-bam and nft-vcf
mise run clean
mise run bump <version>      # set the version in pom.xml and docs/index.md
```

## Conventions

- The version lives in `pom.xml`. `moduleVersion` is filled in at build time.
- After changing `mise.toml`, run `mise lock` and commit `mise.lock` and `.mise/locks/`: CI installs with `--locked`.
- CI takes Nextflow from `setup-nextflow`, so the matrix decides its version, not `mise.toml`.
- Add user-facing changes to `CHANGELOG.md` under `[Unreleased]`.
- Run nf-tests through `env -u CLAUDECODE`: with `CLAUDECODE` set, Nextflow changes its log format and snapshots fail.
