workflow {

    ch_version = Channel.of(
    """
    Workflow:
        Pipeline: 1.0
        Nextflow: $workflow.nextflow.version
    """.stripIndent().trim())
    .collectFile(storeDir: "${params.outdir}/pipeline_info", name: 'nf_core_pipeline_software_mqc_versions.yml', sort: true, newLine: true)

}
