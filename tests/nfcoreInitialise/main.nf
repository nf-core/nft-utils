process TEST_MODULE {

    output:
    path("test.txt"), emit: output

    script:
    """
    echo "hello!" > test.txt
    """
}
