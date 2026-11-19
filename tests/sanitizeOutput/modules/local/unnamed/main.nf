process UNNAMED {
    output:
    path "a.txt"
    path "b.txt"

    script:
    """
    echo a > a.txt
    echo b > b.txt
    """
}
