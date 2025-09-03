workflow {

    def trace_timestamp = new java.util.Date().format('yyyy-MM-dd_HH-mm-ss')

    // stable_content
    channel
        .of(
            """
          I HAVE STABLE CONTENT
          """.stripIndent().trim()
        )
        .collectFile(storeDir: "${params.outdir}/stable", name: 'stable_content.txt', sort: true, newLine: true)

    // stable_name
    channel
        .of(
            """
            I DO NOT HAVE STABLE CONTENT
            ${trace_timestamp}
            """.stripIndent().trim()
        )
        .collectFile(storeDir: "${params.outdir}/stable", name: 'stable_name.txt', sort: true, newLine: true)

    // unstable_name
    channel
        .of(
            """
            I DO NOT HAVE STABLE NAME
            """.stripIndent().trim()
        )
        .collectFile(storeDir: "${params.outdir}/pipeline_info", name: "execution_trace_${trace_timestamp}.txt", sort: true, newLine: true)
}
