workflow {

    def trace_timestamp = new java.util.Date().format( 'yyyy-MM-dd_HH-mm-ss')

    ch_stable_content = Channel.of(
    """
    I HAVE STABLE CONTENT
    """.stripIndent().trim())
    .collectFile(storeDir: "${params.outdir}/stable", name: 'stable_content.txt', sort: true, newLine: true)

    ch_stable_name = Channel.of(
    """
    I DO NOT HAVE STABLE CONTENT
    ${trace_timestamp}
    """.stripIndent().trim())
    .collectFile(storeDir: "${params.outdir}/stable", name: 'stable_name.txt', sort: true, newLine: true)

    ch_unstable_name = Channel.of(
    """
    I DO NOT HAVE STABLE NAME
    """.stripIndent().trim())
    .collectFile(storeDir: "${params.outdir}/not_stable", name: "${trace_timestamp}.txt", sort: true, newLine: true)
}
