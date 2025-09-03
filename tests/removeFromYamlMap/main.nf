workflow {
    channel
        .of(
            """
            Workflow:
                Pipeline: 1.0.0
                Nextflow: ${workflow.nextflow.version}
            Workflow2:
                TEMPLATE: 1.0.0
                Pipeline: 1.0.0
            """.stripIndent().trim()
        )
        .collectFile(storeDir: "${params.outdir}/pipeline_info", name: 'nf_core_pipeline_software_mqc_versions.yml', sort: true, newLine: true)
}
