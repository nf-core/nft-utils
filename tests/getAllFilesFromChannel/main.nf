process FASTQC {
    tag "${meta.id}"
    label 'process_medium'

    input:
    val meta

    output:
    tuple val(meta), val("${task.process}"), val('fastqc'), path("*.html"), topic: multiqc_files, emit: html
    tuple val(meta), val("${task.process}"), val('fastqc'), path("*.zip"), topic: multiqc_files, emit: zip

    script:
    """
    echo "<html>FastQC Report for ${meta.id}</html>" > ${meta.id}_fastqc.html
    echo "FastQC zip data" > ${meta.id}_1_fastqc.zip
    echo "FastQC zip data" > ${meta.id}_2_fastqc.zip
    """
}
