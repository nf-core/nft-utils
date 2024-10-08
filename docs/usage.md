# Functions usage

The plugin adds the following functions to assist with managing pipeline-level nf-test snapshots:

## `removeNextflowVersion()`

nf-core pipelines create a yml file listing all the versions of the software used in the pipeline.

Here is an example of this file coming from the rnaseq pipeline.

```yaml
UNTAR:
  untar: 1.34
Workflow:
  nf-core/rnaseq: v3.16.0dev
  Nextflow: 24.04.4
```

This function remove the Nextflow version from this yml file, as it is not relevant for the snapshot. Therefore for the purpose of the snapshot, it would consider this to be the contents of the YAML file:

```yaml
UNTAR:
  untar: 1.34
Workflow:
  nf-core/rnaseq: v3.16.0dev
```

Usage:

```groovy
assert snapshot(removeNextflowVersion("$outputDir/pipeline_info/nf_core_rnaseq_software_mqc_versions.yml")).match()
```

The only argument is path to the file which must be a versions file in YAML format as per the nf-core standard.

## `removeFromYamlMap()`

Remove any key from a YAML file.

```yaml
UNTAR:
  untar: 1.34
Workflow:
  nf-core/rnaseq: v3.16.0dev
  Nextflow: 24.04.4
```

This function remove the Nextflow version from this yml file, as it is not relevant for the snapshot. Therefore for the purpose of the snapshot, it would consider this to be the contents of the YAML file:

```yaml
UNTAR:
  untar: 1.34
Workflow:
  nf-core/rnaseq: v3.16.0dev
```

Usage:

```groovy
assert snapshot(removeFromYamlMap("$outputDir/pipeline_info/nf_core_pipeline_software_mqc_versions.yml", "Workflow", "Nextflow")).match()
```

The first argument is path to the YAML file, the second and third arguments are the key and subkey to remove.

## `getAllFilesFromDir()`

This function generates a list of all the contents within a directory, allowing for the exclusion of specific files using a glob pattern. It can be used to obtain filenames alone, enabling snapshotting of just the names when the content is not stable. Alternatively, it can snapshot the entire list of files with stable content.

In this example, below are the files produced by a pipeline:

```bash
results/
├── pipeline_info
│   └── execution_trace_2024-09-30_13-10-16.txt
└── stable
    ├── stable_content.txt
    └── stable_name.txt

2 directories, 3 files
```

In this example, 1 file is stable with stable content (`stable_content.txt`), and 1 file is stable with a stable name (`stable_name.txt`).
The last file has no stable content (`execution_trace_2024-09-30_13-10-16.txt`) as its name is based on the date and time of the pipeline execution.

In this example, we aim to snapshot files with stable content, along with the names of files and folders that are stable.

•`stable_name` is a list of all files and folders, excluding those matching the glob pattern `pipeline_info/execution_*.{html,txt}`.
•`stable_content` is a list of all files, excluding those that match the two glob patterns: `pipeline_info/execution_*.{html,txt}` and `**/stable_name.txt`. The latter is specified in the `tests/getAllFilesFromDir/.nftignore` file.
By using `stable_name*.name`, we extract the name of every file in `stable_name` and add them to the snapshot.
`stable_content` can be used in the snapshot directly to include the hash of the file contents.

```groovy
def stable_name    = getAllFilesFromDir(params.outdir, true, ['pipeline_info/execution_*.{html,txt}'], null, ['*', '**/*'])
def stable_content = getAllFilesFromDir(params.outdir, false, ['pipeline_info/execution_*.{html,txt}'], 'tests/getAllFilesFromDir/.nftignore', ['*', '**/*'])
assert snapshot(
  stable_name*.name,
  stable_content
).match()
```

• The first argument is the pipeline’s `outdir` directory path.
• The second argument is a boolean indicating whether to include folders.
• The third argument is a list of glob patterns to ignore.
• The fourth argument is a file containing additional glob patterns to ignore.
• The fifth argument is a list of glob patterns to include.

`getAllFilesFromDir()` also supports named parameters:

```groovy
def stable_name       = getAllFilesFromDir(params.outdir, ignore: ['pipeline_info/execution_*.{html,txt}'])
def stable_name_again = getAllFilesFromDir(params.outdir, include: ['stable/*'])
def stable_content    = getAllFilesFromDir(params.outdir, includeDir: false, ignore: ['pipeline_info/execution_*.{html,txt}'], ignoreFile: 'tests/getAllFilesFromDir/.nftignore')
```

## `getRelativePath()`

This function is used to get the relative path from a list of files compared to a given directory.

```bash
results/
├── pipeline_info
│   └── execution_trace_2024-09-30_13-10-16.txt
└── stable
    ├── stable_content.txt
    └── stable_name.txt

2 directories, 3 files
```

Following the previous example, we want to get the relative path of the stable paths in the `results` directory.

```groovy
def stable_name    = getAllFilesFromDir(params.outdir, true, ['pipeline_info/execution_*.{html,txt}'], null )
```

The `stable_name` variable contains the list of stable files and folders in the `results` directory.

```groovy
assert snapshot(
  getRelativePath(stable_name, outputDir)
).match()
```

By using `getRelativePath()` we generate in the snapshot:

```text
"content": [
    [
        "pipeline_info",
        "stable",
        "stable/stable_content.txt",
        "stable/stable_name.txt"
    ]
]
```

A reduced list can be generated by using `getAllFilesFromDir()` without including the folders in the output.

```text
"content": [
    [
        "stable/stable_content.txt",
        "stable/stable_name.txt"
    ]
]
```

Without using `getRelativePath()` and by using `*.name` to capture the file names, only a flat structure would be generated, as shown below:

```text
"content": [
    [
        "pipeline_info",
        "stable",
        "stable_content.txt",
        "stable_name.txt"
    ]
]
```

`getAllFilesFromDir()` named parameters `relative` can also be used to combine the two functions:

```groovy
def stable_name       = getAllFilesFromDir(params.outdir, relative: true, ignore: ['pipeline_info/execution_*.{html,txt}'] )
def stable_name_again = getAllFilesFromDir(params.outdir, relative: true, include: ['stable/*'] )
```
