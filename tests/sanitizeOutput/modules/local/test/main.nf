process TEST {
    tag "${meta.id}"
    label 'process_single'

    input:
    val meta

    output:
    tuple val(meta), path("*.html"), emit: html
    tuple val(meta), path("*.zip") , emit: zip
    tuple val(meta), path("test/")  , emit: folder
    path ("*.zip")                 , emit: zip_only
    val meta.id, emit: id

    script:
    """
    echo "<html>FastQC Report for ${meta.id}</html>" > ${meta.id}_fastqc.html
    echo "FastQC zip data" > ${meta.id}_fastqc.zip
    mkdir -p "test"
    touch test/test1.txt
    touch test/test2.txt
    """
}
