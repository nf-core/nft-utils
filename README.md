# nft-utils

nf-test utility functions

This repository contains utility functions for nf-test.

These functions are used to help capture level tests using nf-test.

## `removeNextflowVersion()`

Remove the Nextflow version from the yml pipeline created file.

Usage:

```groovy
assert snapshot(removeNextflowVersion("$outputDir/pipeline_info/nf_core_pipeline_software_mqc_versions.yml")).match()
```

Only argument is path to the file.

## `getAllFilesFromDir()`

Get all files (can include folders too) from a directory, not matching a regex pattern.

```groovy
def timestamp      = [/.*\d{4}-\d{2}-\d{2}_\d{2}-\d{2}-\d{2}.*/]
def stable_name    = getAllFilesFromDir(params.outdir, false, timestamp)
def stable_content = getAllFilesFromDir(params.outdir, false, timestamp + [/stable_name\.txt/] )
assert snapshot(
  // Only snapshot name
  stable_name*.name,
  // Snapshot content
  stable_content
).match()
```

First argument is the directory path, second is a boolean to include folders, and the third is a list of regex patterns to exclude.
