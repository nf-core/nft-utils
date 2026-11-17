nextflow.enable.types = true

process TYPED {
    tag "${id}"

    input:
    record(
        id: String,
        meta: Record?
    )

    output:
    record(
        id: id,
        meta: meta,
        report: file("*.report.txt"),
        log: file("*.log"),
        folder: file("folder"),
    )

    script:
    """
    echo "report for ${id}" > ${id}.report.txt
    date > ${id}.log
    mkdir -p folder/sub_folder
    touch folder/test1.txt
    touch folder/sub_folder/test2.txt
    """
}
