process MOOVE {
    tag "${meta.id}"
    label 'process_low'

    input:
    tuple val(meta), path(input)

    output:
    tuple val(meta), path("output/"), emit: output
    tuple val(meta), path("output/*.bam"), emit: bam, optional: true
    tuple val(meta), path("output/*.sam"), emit: sam, optional: true
    tuple val(meta), path("output/*.vcf{,.gz}"), emit: vcf, optional: true

    when:
    task.ext.when == null || task.ext.when

    script:
    """
    mkdir -p output/sub_folder
    mv ${input} output/${input}
    cp output/${input} output/sub_folder/${input}
    touch output/test.txt
    """
}
