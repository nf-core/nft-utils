workflow {

  ch_stable_content = Channel
    .of(
      """
    I HAVE STABLE CONTENT
    """.stripIndent().trim()
    )
    .collectFile(storeDir: "${params.outdir}/stable", name: 'stable_content.txt', sort: true, newLine: true)
}
