nextflow.enable.types = true

process PRECEDENCE {
    input:
    id: String

    output:
    report = record(id: id, log: file("test.log"))
    log    = file("test.log")

    script:
    """
    date > test.log
    """
}
