# nft-utils

nf-test utility functions

This repository contains utility functions for nf-test.

These functions are used to help capture level tests using nf-test.

## `removeNextflowVersion()`

nf-core pipelines create a yml file listing all the versions of the software used in the pipeline.

Here is an example of this file coming from the rnaseq pipeline.

```yaml
BBMAP_BBSPLIT:
  bbmap: 39.01
CAT_FASTQ:
  cat: 8.3
CUSTOM_CATADDITIONALFASTA:
  python: 3.9.5
CUSTOM_GETCHROMSIZES:
  getchromsizes: 1.2
FASTQC:
  fastqc: 0.12.1
GTF2BED:
  perl: 5.26.2
GTF_FILTER:
  python: 3.9.5
GUNZIP_ADDITIONAL_FASTA:
  gunzip: 1.1
GUNZIP_GTF:
  gunzip: 1.1
STAR_GENOMEGENERATE:
  star: 2.7.10a
  samtools: 1.18
  gawk: 5.1.0
TRIMGALORE:
  trimgalore: 0.6.7
  cutadapt: 3.4
UNTAR_SALMON_INDEX:
  untar: 1.34
Workflow:
    nf-core/rnaseq: v3.16.0dev
    Nextflow: 24.04.4
```

This function remove the Nextflow version from this yml file, as it is not relevant for the snapshot.

Usage:

```groovy
assert snapshot(removeNextflowVersion("$outputDir/pipeline_info/nf_core_rnaseq_software_mqc_versions.yml")).match()
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
