process FASTQC {
    tag "${sample_id}"
    label 'process_medium'

    input:
    val sample_id

    output:
    path "*.html", emit: html
    path "*.zip", emit: zip

    script:
    """
    echo "<html>FastQC Report for ${sample_id}</html>" > ${sample_id}_fastqc.html
    echo "FastQC zip data" > ${sample_id}_fastqc.zip
    """
}
