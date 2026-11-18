process PRECEDENCE {
    output:
    path "test.log", emit: log
    val report, emit: report

    script:
    report = [id: "test", log: "test.log"]
    """
    date > test.log
    """
}
